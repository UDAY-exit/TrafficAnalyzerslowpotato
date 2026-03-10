package com.trafficanalyzer.ui.navigation

import android.app.Activity
import android.app.Application
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.trafficanalyzer.data.model.Packet
import com.trafficanalyzer.ui.screens.AnalysisScreen
import com.trafficanalyzer.ui.screens.PacketListScreen
import com.trafficanalyzer.ui.screens.StartScreen
import com.trafficanalyzer.viewmodel.AnalysisViewModel
import com.trafficanalyzer.viewmodel.PacketViewModel

sealed class Screen(val route: String) {
    data object Start      : Screen("start")
    data object PacketList : Screen("packet_list")
    data object Analysis   : Screen(
        "analysis/{packetId}/{destIp}/{protocol}/{srcPort}/{dstPort}/{srcIp}/{length}"
    ) {
        fun buildRoute(
            packetId: Long, destIp: String, protocol: String,
            srcPort: Int, dstPort: Int, srcIp: String, length: Int
        ) = "analysis/$packetId/${destIp.replace(".", "_")}/$protocol/$srcPort/$dstPort/${srcIp.replace(".", "_")}/$length"
    }
}

@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val application = context.applicationContext as Application

    // Create PacketViewModel with manual factory
    val packetViewModel: PacketViewModel = viewModel(
        factory = PacketViewModel.factory(application)
    )
    val vpnPermissionNeeded by packetViewModel.vpnPermissionNeeded.collectAsStateWithLifecycle()

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            packetViewModel.onVpnPermissionGranted(context)
            navController.navigate(Screen.PacketList.route) {
                popUpTo(Screen.Start.route) { inclusive = false }
            }
        } else {
            packetViewModel.onVpnPermissionDenied()
        }
    }

    LaunchedEffect(vpnPermissionNeeded) {
        if (vpnPermissionNeeded) {
            packetViewModel.getVpnPermissionIntent()?.let { vpnPermissionLauncher.launch(it) }
        }
    }

    NavHost(
        navController    = navController,
        startDestination = Screen.Start.route,
        modifier         = modifier
    ) {
        composable(Screen.Start.route) {
            StartScreen(
                onStartCapture = {
                    packetViewModel.startCapture(context)
                    if (VpnService.prepare(context) == null) {
                        navController.navigate(Screen.PacketList.route) {
                            popUpTo(Screen.Start.route) { inclusive = false }
                        }
                    }
                }
            )
        }

        composable(Screen.PacketList.route) {
            PacketListScreen(
                viewModel = packetViewModel,
                onAnalyzePacket = { packet ->
                    navController.navigate(
                        Screen.Analysis.buildRoute(
                            packetId = packet.id,
                            destIp   = packet.destinationIp,
                            protocol = packet.protocol,
                            srcPort  = packet.sourcePort,
                            dstPort  = packet.destinationPort,
                            srcIp    = packet.sourceIp,
                            length   = packet.length
                        )
                    )
                }
            )
        }

        composable(
            route     = Screen.Analysis.route,
            arguments = listOf(
                navArgument("packetId")  { type = NavType.LongType },
                navArgument("destIp")    { type = NavType.StringType },
                navArgument("protocol")  { type = NavType.StringType },
                navArgument("srcPort")   { type = NavType.IntType },
                navArgument("dstPort")   { type = NavType.IntType },
                navArgument("srcIp")     { type = NavType.StringType },
                navArgument("length")    { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val args = backStackEntry.arguments!!
            val packet = Packet(
                id              = args.getLong("packetId"),
                timestampMs     = 0L,
                sourceIp        = args.getString("srcIp", "").replace("_", "."),
                destinationIp   = args.getString("destIp", "").replace("_", "."),
                protocol        = args.getString("protocol", ""),
                sourcePort      = args.getInt("srcPort"),
                destinationPort = args.getInt("dstPort"),
                length          = args.getInt("length")
            )

            val analysisViewModel: AnalysisViewModel = viewModel(
                factory = AnalysisViewModel.factory(application)
            )

            AnalysisScreen(
                packet         = packet,
                viewModel      = analysisViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
