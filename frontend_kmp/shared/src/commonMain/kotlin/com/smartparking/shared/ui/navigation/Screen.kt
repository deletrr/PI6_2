package com.smartparking.shared.ui.navigation

sealed class Screen(val route: String) {
    // Auth
    object Login    : Screen("login")
    object Register : Screen("register")

    // User
    object Home          : Screen("home")
    object Map           : Screen("map")
    object QrScanner     : Screen("qr_scanner")
    object ActiveSession : Screen("active_session")
    object Wallet        : Screen("wallet")
    object Recharge      : Screen("recharge")
    object Extract       : Screen("extract")
    object Fines         : Screen("fines")
    object Support       : Screen("support")
    object Profile       : Screen("profile")
    object History       : Screen("history")

    // Admin
    object AdminDashboard  : Screen("admin_dashboard")
    object AdminMap        : Screen("admin_map")
    object AdminUsers      : Screen("admin_users")
    object AdminMeters     : Screen("admin_meters")
    object AdminSessions   : Screen("admin_sessions")
    object AdminFines      : Screen("admin_fines")
    object AdminExtract    : Screen("admin_extract")
    object AdminSupport    : Screen("admin_support")
    object AdminLogs       : Screen("admin_logs")
    object AdminOrphans    : Screen("admin_orphans")

    // Detail screens with args
    object MeterDetail  : Screen("meter/{meterId}") {
        fun createRoute(meterId: String) = "meter/$meterId"
    }
    object SessionDetail : Screen("session/{sessionId}") {
        fun createRoute(sessionId: String) = "session/$sessionId"
    }
}
