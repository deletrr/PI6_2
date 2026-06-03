package com.smartparking.shared.ui.screens.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smartparking.shared.model.ParkingMeterModel
import com.smartparking.shared.repository.AppSession
import com.smartparking.shared.ui.components.*
import com.smartparking.shared.util.format
import com.smartparking.shared.viewmodel.HomeViewModel

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToQr: () -> Unit,
    onNavigateToActiveSession: () -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToMeterDetail: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val user by AppSession.currentUser.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var addressQuery by remember { mutableStateOf("") }
    var isMapView by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.loadMapMeters() }

    val displayMeters = remember(state.filteredMeters, searchQuery) {
        if (searchQuery.isBlank()) state.filteredMeters
        else state.filteredMeters.filter {
            it.code.contains(searchQuery, ignoreCase = true) ||
            it.description.orEmpty().contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            PontoLivreTopBar(
                title = "PontoLivre",
                actions = {
                    IconButton(onClick = onNavigateToWallet) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Carteira")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToQr,
                icon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null) },
                text = { Text("Escanear QR") }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // Greeting + balance card
            user?.let { u ->
                Card(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Olá, ${u.name.split(" ").first()}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("Saldo disponível",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("R$ ${"%.2f".format(u.balance)}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer)
                            TextButton(onClick = onNavigateToWallet,
                                contentPadding = PaddingValues(0.dp)) {
                                Text("Recarregar", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }

            // Quick actions row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.DirectionsCar,
                    label = "Sessão Ativa",
                    onClick = onNavigateToActiveSession
                )
                QuickActionCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.QrCode,
                    label = "Código Manual",
                    onClick = onNavigateToQr
                )
            }

            Spacer(Modifier.height(8.dp))

            // FILTRO DE ENDEREÇO / CEP
            OutlinedTextField(
                value = addressQuery,
                onValueChange = { addressQuery = it },
                placeholder = { Text("Filtrar por endereço ou CEP...") },
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                trailingIcon = {
                    IconButton(onClick = { viewModel.filterByAddress(addressQuery) }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar")
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            Spacer(Modifier.height(8.dp))

            // Search and View Toggle
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Buscar vaga...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                IconButton(
                    onClick = { isMapView = !isMapView },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (isMapView) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Icon(
                        if (isMapView) Icons.Default.List else Icons.Default.Map,
                        contentDescription = if (isMapView) "Ver lista" else "Ver mapa"
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            SectionHeader(title = "Vagas próximas (${displayMeters.count { it.status == "FREE" }}/${displayMeters.size})",
                action = {
                    IconButton(onClick = { viewModel.loadMapMeters() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                })

            if (state.isLoading) {
                FullScreenLoading()
            } else if (state.error != null) {
                ErrorMessage(state.error!!, onRetry = { viewModel.loadMapMeters() })
            } else {
                if (isMapView) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 4.dp).clip(RoundedCornerShape(16.dp))) {
                        ParkingMapView(
                            meters = displayMeters,
                            onMeterSelected = { code ->
                                state.meters.find { it.code == code }?.let { onNavigateToMeterDetail(it.id) }
                            }
                        )
                    }
                } else {
                    if (displayMeters.isEmpty()) {
                        EmptyState("Nenhuma vaga encontrada nesta região", Icons.Default.LocalParking)
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(displayMeters) { meter ->
                                MeterCard(meter = meter, onClick = { onNavigateToMeterDetail(meter.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
expect fun ParkingMapView(
    meters: List<ParkingMeterModel>,
    onMeterSelected: (String) -> Unit
)

@Composable
private fun QuickActionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(label, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun MeterCard(meter: ParkingMeterModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocalParking, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Column {
                    Text(meter.code, style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold)
                    Text(meter.description ?: "Sem descrição",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            StatusBadge(meter.status)
        }
    }
}
