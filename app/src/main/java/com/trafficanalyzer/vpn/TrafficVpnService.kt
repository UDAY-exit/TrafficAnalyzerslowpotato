package com.trafficanalyzer.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.app.NotificationCompat
import com.trafficanalyzer.MainActivity
import com.trafficanalyzer.data.model.Packet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/**
 * TrafficVpnService — Core VPN capture service (DEMO MODE).
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HOW IT WORKS
 * ──────────────────────────────────────────────────────────────────────────
 * 1. The Android OS creates a TUN (virtual network) interface when we call
 *    Builder.establish(). All IP traffic is redirected into that TUN fd.
 *
 * 2. We open a FileInputStream on the TUN fd and read raw IP packets in a
 *    background coroutine (see TunInterfaceReader).
 *
 * 3. Each packet is parsed (IPv4 header + optional TCP/UDP header) and
 *    emitted on [packetFlow] so the ViewModel can display it.
 *
 * WHY WE DO NOT FORWARD PACKETS:
 *    Full tunnelling requires opening a protected socket (via protect()),
 *    connecting to the real destination, relaying outbound bytes, and
 *    writing responses back into the TUN fd — essentially a user-space
 *    network stack. This demo intentionally skips that. The device has
 *    NO real internet while the VPN is active.
 *
 * WHY INTERNET WORKS AGAIN AFTER STOPPING:
 *    When tunFd.close() is called in onDestroy(), the OS automatically
 *    removes the VPN routes from the kernel routing table. The physical
 *    NIC routes become active again immediately — no manual cleanup needed.
 * ──────────────────────────────────────────────────────────────────────────
 */
class TrafficVpnService : VpnService() {

    companion object {
        private const val TAG = "TrafficVpnService"

        private const val CHANNEL_ID      = "vpn_capture_channel"
        private const val NOTIFICATION_ID = 1001

        /** Intent action used by PacketViewModel to request a clean stop */
        const val ACTION_STOP = "com.trafficanalyzer.vpn.ACTION_STOP"

        // TUN interface configuration
        private const val TUN_ADDRESS      = "10.0.0.2"  // Dummy private IP for TUN end
        private const val TUN_PREFIX_LEN   = 32           // /32 point-to-point
        private const val TUN_MTU          = 1500         // Standard Ethernet MTU
        private const val TUN_ROUTE        = "0.0.0.0"   // Capture ALL traffic
        private const val TUN_ROUTE_PREFIX = 0            // /0 = default route

        /**
         * Process-wide packet flow. Reset to a fresh instance each time
         * [establishVpn] is called so no stale buffered packets carry over
         * into a new capture session.
         *
         * replay=0: late collectors miss past packets (live feed behaviour).
         * extraBufferCapacity=512: buffer up to 512 packets before back-pressure.
         */
        private var _packetFlow = MutableSharedFlow<Packet>(
            replay = 0,
            extraBufferCapacity = 512
        )
        val packetFlow: SharedFlow<Packet> get() = _packetFlow

        /** True while the VPN tunnel is established and reading packets */
        @Volatile var isRunning = false
            private set

        /** Replace the shared flow with a fresh instance for a new session */
        internal fun resetPacketFlow() {
            _packetFlow = MutableSharedFlow(replay = 0, extraBufferCapacity = 512)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var tunInterface: ParcelFileDescriptor? = null
    private var readerJob: Job? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Service lifecycle
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "VPN service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle explicit STOP action sent by PacketViewModel.stopCapture()
        if (intent?.action == ACTION_STOP) {
            Log.d(TAG, "onStartCommand — received STOP action, stopping self")
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        Log.d(TAG, "onStartCommand — establishing VPN tunnel")
        startForeground(NOTIFICATION_ID, buildNotification())
        establishVpn()
        // START_NOT_STICKY: do NOT restart automatically if the OS kills the service
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        Log.d(TAG, "VPN service destroyed — tearing down TUN interface")
        isRunning = false
        readerJob?.cancel()
        serviceScope.cancel()
        // Closing the TUN fd causes the OS to tear down the virtual interface
        // and restore normal routing automatically.
        runCatching { tunInterface?.close() }
        tunInterface = null
        super.onDestroy()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VPN establishment
    // ─────────────────────────────────────────────────────────────────────────

    private fun establishVpn() {
        // Fresh flow for every new session — prevents stale packets from leaking
        resetPacketFlow()
        try {
            val builder = Builder()
                .setSession("TrafficAnalyzer")
                .addAddress(TUN_ADDRESS, TUN_PREFIX_LEN)
                .addRoute(TUN_ROUTE, TUN_ROUTE_PREFIX)
                .setMtu(TUN_MTU)
                .setBlocking(true)
                .addDisallowedApplication(packageName) // Exclude this app from the VPN tunnel so API calls work
                .setConfigureIntent(buildConfigurePendingIntent())

            tunInterface = builder.establish()
                ?: throw IllegalStateException("Builder.establish() returned null — VPN permission not granted?")

            isRunning = true
            Log.d(TAG, "TUN interface established fd=${tunInterface?.fd}")
            startPacketReader()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to establish VPN: ${e.message}", e)
            stopSelf()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Packet reading
    // ─────────────────────────────────────────────────────────────────────────

    private fun startPacketReader() {
        val fd = tunInterface ?: return
        readerJob = serviceScope.launch {
            val reader = TunInterfaceReader(fd, _packetFlow)
            reader.startReading()
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Notification helpers (required for foreground service on API 26+)
    // ─────────────────────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "VPN Capture",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Traffic Analyzer is capturing packets" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Traffic Analyzer")
            .setContentText("Capturing network packets…")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(buildConfigurePendingIntent())
            .build()
    }

    private fun buildConfigurePendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
