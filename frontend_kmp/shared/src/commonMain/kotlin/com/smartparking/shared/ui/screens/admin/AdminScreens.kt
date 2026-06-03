package com.smartparking.shared.ui.screens.admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartparking.shared.model.*
import com.smartparking.shared.repository.AppSession
import com.smartparking.shared.util.format
import com.smartparking.shared.ui.components.*
import com.smartparking.shared.ui.theme.*
import com.smartparking.shared.viewmodel.*

// ── AdminDashboardScreen ──────────────────────────────────────────────────────

@Composable
fun AdminDashboardScreen(
    viewModel: AdminDashboardViewModel,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val user by AppSession.currentUser.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            PontoLivreTopBar(
                title = "Painel Admin",
                actions = {
                    IconButton(onClick = { AppSession.logout(); onLogout() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        if (state.isLoading) {
            FullScreenLoading()
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding)
                    .verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Welcome
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.AdminPanelSettings, contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        Column {
                            Text("Bem-vindo, ${user?.name?.split(" ")?.first() ?: "Admin"}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Painel de controle",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                    }
                }

                // Stats grid
                state.dashboard?.let { d ->
                    Text("Resumo geral", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Modifier.weight(1f), "Usuários ativos",
                            d.totalUsers.toString(), Icons.Default.People, SuccessGreen)
                        StatCard(Modifier.weight(1f), "Vagas livres",
                            "${d.freeMeters}/${d.totalMeters}", Icons.Default.LocalParking,
                            SuccessGreen)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Modifier.weight(1f), "Vagas ocupadas",
                            d.occupiedMeters.toString(), Icons.Default.DirectionsCar, ErrorRed)
                        StatCard(Modifier.weight(1f), "Sessões ativas",
                            d.activeSessions.toString(), Icons.Default.Timer, WarningAmber)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatCard(Modifier.weight(1f), "Receita hoje",
                            "R$ ${"%.2f".format(d.todayRevenue)}", Icons.Default.AttachMoney, SuccessGreen)
                        StatCard(Modifier.weight(1f), "Multas pendentes",
                            d.pendingFines.toString(), Icons.Default.Warning, WarningAmber)
                    }
                    if (d.orphanMeters > 0) {
                        Card(Modifier.fillMaxWidth().clickable { onNavigate("admin_orphans") },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = WarningAmber.copy(alpha = 0.1f))) {
                            Row(Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Default.LocationOff, contentDescription = null, tint = WarningAmber)
                                Column(Modifier.weight(1f)) {
                                    Text("${d.orphanMeters} parquímetro(s) órfão(s)",
                                        fontWeight = FontWeight.Bold, color = WarningAmber)
                                    Text("Clique para configurar coordenadas",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }

                // Navigation menu
                Text("Gerenciamento", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)

                val menuItems = listOf(
                    Triple(Icons.Default.People,         "Usuários",      "admin_users"),
                    Triple(Icons.Default.LocalParking,   "Parquímetros",  "admin_meters"),
                    Triple(Icons.Default.DirectionsCar,  "Sessões",       "admin_sessions"),
                    Triple(Icons.Default.Warning,        "Multas",        "admin_fines"),
                    Triple(Icons.Default.Receipt,        "Extrato",       "admin_extract"),
                    Triple(Icons.Default.SupportAgent,   "Suporte",       "admin_support"),
                    Triple(Icons.Default.Code,       "Logs MQTT",     "admin_logs"),
                )
                menuItems.forEach { (icon, label, route) ->
                    Card(Modifier.fillMaxWidth().clickable { onNavigate(route) },
                        shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(icon, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color
) {
    Card(modifier, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── AdminUsersScreen ──────────────────────────────────────────────────────────

@Composable
fun AdminUsersScreen(viewModel: AdminUsersViewModel, onNavigateUp: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var search by remember { mutableStateOf("") }
    var editUser by remember { mutableStateOf<UserModel?>(null) }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = {
            PontoLivreTopBar(title = "Usuários (${state.total})", onNavigateUp = onNavigateUp) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = search, onValueChange = { search = it; viewModel.load(it) },
                placeholder = { Text("Buscar usuário...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(16.dp), singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            state.successMessage?.let {
                Text(it, Modifier.padding(horizontal = 16.dp), color = SuccessGreen,
                    style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let { ErrorMessage(it) }

            if (state.isLoading) FullScreenLoading()
            else LazyColumn(Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.users) { user ->
                    Card(Modifier.fillMaxWidth().clickable { editUser = user },
                        shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Surface(shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(40.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(user.name.first().uppercaseChar().toString(),
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                                    }
                                }
                                Column {
                                    Text(user.name, fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium)
                                    Text(user.email, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("R$ ${"%.2f".format(user.balance)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (user.balance > 0) SuccessGreen else ErrorRed)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusBadge(if (user.active) "FREE" else "MAINTENANCE")
                                Spacer(Modifier.height(4.dp))
                                Text(if (user.role == "ADMIN") "ADMIN" else "USER",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    editUser?.let { user ->
        AdminEditUserDialog(
            user = user,
            isSaving = state.isSaving,
            onSave = { req -> viewModel.updateUser(user.id, req) { editUser = null } },
            onDelete = { viewModel.deleteUser(user.id); editUser = null },
            onDismiss = { editUser = null }
        )
    }
}

@Composable
private fun AdminEditUserDialog(
    user: UserModel,
    isSaving: Boolean,
    onSave: (AdminUpdateUserRequest) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    var balance by remember { mutableStateOf(user.balance.toString()) }
    var active by remember { mutableStateOf(user.active) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(user.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(user.email, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = balance, onValueChange = { balance = it },
                    label = { Text("Saldo (R$)") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()) {
                    Text("Conta ativa")
                    Switch(checked = active, onCheckedChange = { active = it })
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDeleteConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)) {
                    Text("Desativar")
                }
                Button(
                    onClick = { onSave(AdminUpdateUserRequest(
                        active = active,
                        balance = balance.toDoubleOrNull()
                    )) },
                    enabled = !isSaving
                ) { Text("Salvar") }
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showDeleteConfirm) {
        ConfirmDialog(
            title = "Desativar usuário?",
            message = "O usuário perderá acesso ao sistema.",
            confirmLabel = "Desativar",
            onConfirm = { onDelete(); showDeleteConfirm = false },
            onDismiss = { showDeleteConfirm = false }
        )
    }
}

// ── AdminMetersScreen ─────────────────────────────────────────────────────────

@Composable
fun AdminMetersScreen(viewModel: AdminMetersViewModel, onNavigateUp: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var newCode by remember { mutableStateOf("") }
    var newDesc by remember { mutableStateOf("") }
    var newLat by remember { mutableStateOf("") }
    var newLng by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { PontoLivreTopBar(title = "Parquímetros", onNavigateUp = onNavigateUp) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreate = true }) {
                Icon(Icons.Default.Add, contentDescription = "Novo parquímetro")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.successMessage?.let {
                Text(it, Modifier.padding(16.dp), color = SuccessGreen,
                    style = MaterialTheme.typography.bodySmall)
            }
            state.error?.let { ErrorMessage(it) }

            if (state.isLoading) FullScreenLoading()
            else LazyColumn(Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.meters) { meter ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(meter.code, fontWeight = FontWeight.Bold)
                                    if (meter.orphan) {
                                        Surface(color = WarningAmber.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(50)) {
                                            Text("ÓRFÃO", Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = WarningAmber)
                                        }
                                    }
                                }
                                Text(meter.description ?: "Sem descrição",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (meter.latitude != null) {
                                    Text("${meter.latitude}, ${meter.longitude}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Column(horizontalAlignment = Alignment.End,
                                verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusBadge(meter.status)
                                IconButton(onClick = { viewModel.deleteMeter(meter.id) },
                                    modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Default.Delete, contentDescription = null,
                                        tint = ErrorRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Novo parquímetro") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = newCode, onValueChange = { newCode = it.uppercase() },
                        label = { Text("Código *") }, placeholder = { Text("PKM-006") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = newDesc, onValueChange = { newDesc = it },
                        label = { Text("Descrição") }, modifier = Modifier.fillMaxWidth(),
                        singleLine = true)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = newLat, onValueChange = { newLat = it },
                            label = { Text("Latitude") }, modifier = Modifier.weight(1f),
                            singleLine = true)
                        OutlinedTextField(value = newLng, onValueChange = { newLng = it },
                            label = { Text("Longitude") }, modifier = Modifier.weight(1f),
                            singleLine = true)
                    }
                    state.error?.let { ErrorMessage(it) }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createMeter(CreateParkingMeterRequest(
                            code = newCode, description = newDesc.ifBlank { null },
                            latitude = newLat.toDoubleOrNull(),
                            longitude = newLng.toDoubleOrNull()
                        )) { showCreate = false; newCode = ""; newDesc = ""; newLat = ""; newLng = "" }
                    },
                    enabled = newCode.isNotBlank() && !state.isSaving
                ) { Text("Criar") }
            },
            dismissButton = { OutlinedButton(onClick = { showCreate = false }) { Text("Cancelar") } }
        )
    }
}

// ── AdminOrphansScreen ────────────────────────────────────────────────────────

@Composable
fun AdminOrphansScreen(viewModel: AdminMetersViewModel, onNavigateUp: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var selected by remember { mutableStateOf<ParkingMeterModel?>(null) }
    var lat by remember { mutableStateOf("") }
    var lng by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { PontoLivreTopBar(title = "Parquímetros Órfãos", onNavigateUp = onNavigateUp) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            state.successMessage?.let {
                Card(Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f))) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen)
                        Text(it, color = SuccessGreen)
                    }
                }
            }

            if (state.orphans.isEmpty()) {
                EmptyState("Nenhum parquímetro órfão.", Icons.Default.LocationOn)
            } else {
                LazyColumn(Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.orphans) { meter ->
                        Card(Modifier.fillMaxWidth().clickable {
                            selected = meter; lat = ""; lng = ""; desc = meter.description ?: ""
                        }, shape = RoundedCornerShape(12.dp),
                            border = CardDefaults.outlinedCardBorder()) {
                            Row(Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text(meter.code, fontWeight = FontWeight.Bold)
                                    Text(meter.description ?: "Sem localização",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(Icons.Default.EditLocation, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }

    selected?.let { meter ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text("Configurar ${meter.code}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Defina as coordenadas para exibir este parquímetro no mapa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = desc, onValueChange = { desc = it },
                        label = { Text("Descrição / Endereço") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = lat, onValueChange = { lat = it },
                        label = { Text("Latitude *") }, placeholder = { Text("-23.5505") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                    OutlinedTextField(value = lng, onValueChange = { lng = it },
                        label = { Text("Longitude *") }, placeholder = { Text("-46.6333") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val latD = lat.toDoubleOrNull() ?: return@Button
                        val lngD = lng.toDoubleOrNull() ?: return@Button
                        viewModel.assignCoordinates(meter.id, latD, lngD, desc.ifBlank { null }) {
                            selected = null
                        }
                    },
                    enabled = lat.isNotBlank() && lng.isNotBlank() && !state.isSaving
                ) {
                    if (state.isSaving) CircularProgressIndicator(Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Salvar e exibir no mapa")
                }
            },
            dismissButton = { OutlinedButton(onClick = { selected = null }) { Text("Cancelar") } }
        )
    }
}

// ── AdminFinesScreen ──────────────────────────────────────────────────────────

@Composable
fun AdminFinesScreen(viewModel: AdminFinesViewModel, onNavigateUp: () -> Unit) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { PontoLivreTopBar(title = "Multas (${state.total})", onNavigateUp = onNavigateUp) }
    ) { padding ->
        if (state.isLoading) FullScreenLoading()
        else if (state.fines.isEmpty()) EmptyState("Nenhuma multa.", Icons.Default.CheckCircle)
        else LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.fines) { fine ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(fine.userName, fontWeight = FontWeight.Bold)
                                Text(fine.meterCode, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(fine.status)
                        }
                        Text(fine.reason, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("R$ ${"%.2f".format(fine.amount)}",
                                fontWeight = FontWeight.Bold, color = ErrorRed)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (fine.status == "PENDING") {
                                    OutlinedButton(onClick = { viewModel.updateStatus(fine.id, "PAID") },
                                        modifier = Modifier.height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                                        Text("Pago", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                OutlinedButton(onClick = { viewModel.delete(fine.id) },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)) {
                                    Text("Remover", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── AdminSupportScreen ────────────────────────────────────────────────────────

@Composable
fun AdminSupportScreen(viewModel: AdminSupportViewModel, onNavigateUp: () -> Unit) {
    val state by viewModel.state.collectAsState()
    var respondingTo by remember { mutableStateOf<SupportTicketModel?>(null) }
    var responseText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { PontoLivreTopBar(title = "Suporte (${state.total})", onNavigateUp = onNavigateUp) }
    ) { padding ->
        if (state.isLoading) FullScreenLoading()
        else if (state.tickets.isEmpty()) EmptyState("Nenhum chamado.", Icons.Default.SupportAgent)
        else LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.tickets) { ticket ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(ticket.subject, fontWeight = FontWeight.Bold)
                                Text(ticket.userName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(if (ticket.resolved) "CLOSED" else "ACTIVE")
                        }
                        Text(ticket.message, style = MaterialTheme.typography.bodySmall)
                        ticket.response?.let {
                            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                                Text(it, Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (!ticket.resolved) {
                            OutlinedButton(
                                onClick = { respondingTo = ticket; responseText = "" },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Responder") }
                        }
                    }
                }
            }
        }
    }

    respondingTo?.let { ticket ->
        AlertDialog(
            onDismissRequest = { respondingTo = null },
            title = { Text("Responder: ${ticket.subject}") },
            text = {
                OutlinedTextField(value = responseText, onValueChange = { responseText = it },
                    label = { Text("Resposta") }, modifier = Modifier.fillMaxWidth(),
                    minLines = 4, maxLines = 6)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.respond(ticket.id, responseText) { respondingTo = null }
                    },
                    enabled = responseText.isNotBlank() && !state.isResponding
                ) { Text("Enviar") }
            },
            dismissButton = { OutlinedButton(onClick = { respondingTo = null }) { Text("Cancelar") } }
        )
    }
}

// ── AdminSessionsScreen ───────────────────────────────────────────────────────

@Composable
fun AdminSessionsScreen(viewModel: AdminSessionsViewModel, onNavigateUp: () -> Unit) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.load() }

    Scaffold(
        topBar = { PontoLivreTopBar(title = "Sessões (${state.total})", onNavigateUp = onNavigateUp) }
    ) { padding ->
        if (state.isLoading) FullScreenLoading()
        else LazyColumn(Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.sessions) { session ->
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(session.userName, fontWeight = FontWeight.Bold)
                                Text(session.meterCode,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusBadge(session.status)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(session.startTime.take(16).replace("T", " "),
                                style = MaterialTheme.typography.bodySmall)
                            Text("R$ ${"%.2f".format(session.amountCharged)}",
                                fontWeight = FontWeight.Bold,
                                color = if (session.amountCharged > 0) ErrorRed else SuccessGreen)
                        }
                    }
                }
            }
        }
    }
}
