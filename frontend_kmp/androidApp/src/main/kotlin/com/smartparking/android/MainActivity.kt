package com.smartparking.android

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.*
import com.google.zxing.integration.android.IntentIntegrator
import com.smartparking.shared.api.initTokenStorage
import com.smartparking.shared.repository.AppSession
import com.smartparking.shared.ui.navigation.Screen
import com.smartparking.shared.ui.screens.admin.*
import com.smartparking.shared.ui.screens.user.*
import com.smartparking.shared.ui.theme.PontoLivreTheme
import com.smartparking.shared.viewmodel.*

class MainActivity : ComponentActivity() {

    private var onQrResult: ((String) -> Unit)? = null

    private val qrLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        IntentIntegrator.parseActivityResult(result.resultCode, result.data)
            ?.contents
            ?.let { onQrResult?.invoke(it) }
        onQrResult = null
    }

    private val cameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchQrScanner() }

    private fun launchQrScanner() {
        val integrator = IntentIntegrator(this)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
        integrator.setPrompt("Aponte para o QR Code da vaga")
        integrator.setBeepEnabled(true)
        integrator.setOrientationLocked(false)
        qrLauncher.launch(integrator.createScanIntent())
    }

    fun requestQrScan(onResult: (String) -> Unit) {
        onQrResult = onResult
        cameraPermission.launch(Manifest.permission.CAMERA)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTokenStorage(applicationContext)
        setContent {
            PontoLivreTheme {
                PontoLivreApp(onScanQr = { cb -> requestQrScan(cb) })
            }
        }
    }
}

@Composable
fun PontoLivreApp(onScanQr: ((String) -> Unit) -> Unit) {
    val navController = rememberNavController()

    val startDestination = if (AppSession.isLoggedIn)
        if (AppSession.isAdmin) Screen.AdminDashboard.route else Screen.Home.route
    else Screen.Login.route

    // Shared ViewModels
    val sessionVm       = remember { SessionViewModel() }
    val walletVm        = remember { WalletViewModel() }
    val adminMetersVm   = remember { AdminMetersViewModel() }
    val adminLogsVm     = remember { AdminLogsViewModel() }

    NavHost(navController = navController, startDestination = startDestination) {

        // ── Auth ──────────────────────────────────────────────────────────────
        composable(Screen.Login.route) {
            val vm = remember { AuthViewModel() }
            LoginScreen(vm,
                onLoginSuccess = {
                    val dest = if (AppSession.isAdmin) Screen.AdminDashboard.route
                               else Screen.Home.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(Screen.Register.route) })
        }

        composable(Screen.Register.route) {
            val vm = remember { AuthViewModel() }
            RegisterScreen(vm,
                onRegisterSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() })
        }

        // ── User ──────────────────────────────────────────────────────────────
        composable(Screen.Home.route) {
            val homeVm = remember { HomeViewModel() }
            UserBottomNav(navController) {
                HomeScreen(homeVm,
                    onNavigateToQr = {
                        onScanQr { code ->
                            navController.navigate(Screen.MeterDetail.createRoute(code))
                        }
                    },
                    onNavigateToActiveSession = { navController.navigate(Screen.ActiveSession.route) },
                    onNavigateToWallet = { navController.navigate(Screen.Wallet.route) },
                    onNavigateToMeterDetail = { id ->
                        navController.navigate(Screen.MeterDetail.createRoute(id))
                    })
            }
        }

        composable(Screen.MeterDetail.route) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("meterId") ?: ""
            MeterDetailScreen(id, sessionVm,
                onNavigateUp = { navController.popBackStack() },
                onSessionStarted = {
                    navController.navigate(Screen.ActiveSession.route) {
                        popUpTo(Screen.Home.route)
                    }
                })
        }

        composable(Screen.ActiveSession.route) {
            ActiveSessionScreen(sessionVm,
                onNavigateUp   = { navController.popBackStack() },
                onSessionEnded = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                })
        }

        composable(Screen.QrScanner.route) {
            ManualCodeScreen(
                onSubmit     = { code -> 
                    // Em vez de iniciar direto, leva para o detalhe para confirmar a placa
                    navController.navigate(Screen.MeterDetail.createRoute(code))
                },
                onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.Wallet.route) {
            UserBottomNav(navController) {
                WalletScreen(walletVm,
                    onNavigateUp        = { navController.popBackStack() },
                    onNavigateToRecharge = { navController.navigate(Screen.Recharge.route) },
                    onNavigateToExtract  = { navController.navigate(Screen.Extract.route) })
            }
        }

        composable(Screen.Recharge.route) {
            RechargeScreen(walletVm, onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.Extract.route) {
            ExtractScreen(walletVm, onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.Fines.route) {
            val vm = remember { FinesViewModel() }
            UserBottomNav(navController) {
                FinesScreen(vm, onNavigateUp = { navController.popBackStack() })
            }
        }

        composable(Screen.Support.route) {
            val vm = remember { SupportViewModel() }
            UserBottomNav(navController) {
                SupportScreen(vm, onNavigateUp = { navController.popBackStack() })
            }
        }

        composable(Screen.History.route) {
            HistoryScreen(sessionVm, onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.Profile.route) {
            UserBottomNav(navController) {
                ProfileScreen(
                    onNavigateUp = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    })
            }
        }

        // ── Admin ─────────────────────────────────────────────────────────────
        composable(Screen.AdminDashboard.route) {
            val vm = remember { AdminDashboardViewModel() }
            AdminDashboardScreen(vm,
                onNavigate = { navController.navigate(it) },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                })
        }

        composable(Screen.AdminUsers.route) {
            AdminUsersScreen(remember { AdminUsersViewModel() },
                onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.AdminMeters.route) {
            AdminMetersScreen(adminMetersVm,
                onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.AdminOrphans.route) {
            AdminOrphansScreen(adminMetersVm,
                onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.AdminSessions.route) {
            AdminSessionsScreen(remember { AdminSessionsViewModel() },
                onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.AdminFines.route) {
            AdminFinesScreen(remember { AdminFinesViewModel() },
                onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.AdminSupport.route) {
            AdminSupportScreen(remember { AdminSupportViewModel() },
                onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.AdminExtract.route) {
            AdminExtractScreen(walletVm,
                onNavigateUp = { navController.popBackStack() })
        }

        composable(Screen.AdminLogs.route) {
            AdminLogsScreen(adminLogsVm,
                onNavigateUp = { navController.popBackStack() })
        }
    }
}

@Composable
fun UserBottomNav(navController: NavController, content: @Composable () -> Unit) {
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) }, label = { Text("Início") },
                    selected = currentRoute == Screen.Home.route,
                    onClick = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    })
                NavigationBarItem(
                    icon = { Icon(Icons.Default.AccountBalanceWallet, null) }, label = { Text("Carteira") },
                    selected = currentRoute == Screen.Wallet.route,
                    onClick = { navController.navigate(Screen.Wallet.route) })
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Warning, null) }, label = { Text("Multas") },
                    selected = currentRoute == Screen.Fines.route,
                    onClick = { navController.navigate(Screen.Fines.route) })
                NavigationBarItem(
                    icon = { Icon(Icons.Default.SupportAgent, null) }, label = { Text("Suporte") },
                    selected = currentRoute == Screen.Support.route,
                    onClick = { navController.navigate(Screen.Support.route) })
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Person, null) }, label = { Text("Perfil") },
                    selected = currentRoute == Screen.Profile.route,
                    onClick = { navController.navigate(Screen.Profile.route) })
            }
        }
    ) { padding -> Box(Modifier.padding(padding)) { content() } }
}
