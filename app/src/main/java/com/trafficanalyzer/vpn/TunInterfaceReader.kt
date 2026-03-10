package com.trafficanalyzer.vpn

import android.os.ParcelFileDescriptor
import android.util.Log
import com.trafficanalyzer.data.model.Packet
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer

/**
 * TunInterfaceReader
 *
 * Continuously reads raw IP packets from the TUN file descriptor using a
 * blocking FileInputStream and delegates parsing to [PacketParser].
 *
 * Threading model:
 *   Called from a Dispatchers.IO coroutine. Blocks on FileInputStream.read()
 *   between packets. The OS unblocks the read when:
 *     a) A new packet is available, or
 *     b) The file descriptor is closed (triggers IOException → exit cleanly)
 *
 * Buffer sizing:
 *   IP packets are at most 65,535 bytes per RFC 791. We allocate 65,536
 *   bytes to avoid off-by-one issues.
 */
class TunInterfaceReader(
    private val tunFd: ParcelFileDescriptor,
    private val packetFlow: MutableSharedFlow<Packet>
) {
    companion object {
        private const val TAG = "TunInterfaceReader"
        private const val MAX_PACKET_SIZE = 65_536
    }

    private var packetCounter = 0L

    /**
     * Blocking read loop — runs until the coroutine is cancelled or the TUN
     * fd is closed by [TrafficVpnService.onDestroy].
     */
    suspend fun startReading() {
        val inputStream = FileInputStream(tunFd.fileDescriptor)
        val buffer = ByteArray(MAX_PACKET_SIZE)

        Log.d(TAG, "Starting TUN read loop")

        try {
            while (currentCoroutineContext().isActive) {
                val bytesRead = inputStream.read(buffer)

                if (bytesRead <= 0) {
                    Log.d(TAG, "TUN fd returned $bytesRead bytes — exiting read loop")
                    break
                }

                // Wrap in a read-only ByteBuffer for zero-copy parsing
                val rawBytes = ByteBuffer.wrap(buffer, 0, bytesRead).asReadOnlyBuffer()

                val packet = PacketParser.parse(rawBytes, ++packetCounter)

                if (packet != null) {
                    // tryEmit drops the packet if the buffer is full (back-pressure protection)
                    packetFlow.tryEmit(packet)
                }
            }
        } catch (e: IOException) {
            // Expected when tunFd.close() is called from onDestroy
            Log.d(TAG, "TUN read loop terminated via IOException: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in TUN read loop", e)
        } finally {
            Log.d(TAG, "TUN read loop exited")
        }
    }
}
