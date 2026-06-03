package com.smartparking.shared.ui.screens.user

import androidx.compose.foundation.layout.*
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
import com.smartparking.shared.ui.components.*
import com.smartparking.shared.viewmodel.SessionViewModel
import com.smartparking.shared.api.ParkingMeterApi
import com.smartparking.shared.api.ApiResult

@Composable
fun MeterDetailScreen(
    meterId: String,
    viewModel: SessionViewModel,
    onNavigateUp: () -> Unit,
    onSessionStarted: () -> Unit
) {
    var meter by remember { mutableStateOf<ParkingMeterModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    var vehiclePlate by remember { mutableStateOf("") }
    var confirmCode by remember { mutableStateOf("") }

    val sessionState by viewModel.state.collectAsState()

    LaunchedEffect(meterId) {
        isLoading = true
        when (val result = ParkingMeterApi.getById(meterId)) {
            is ApiResult.Success -> {
                meter = result.data
                isLoading = false
            }
            is ApiResult.Error -> {
                error = result.message
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            PontoLivreTopBar(
                title = "Detalhes da Vaga",
                onNavigateUp = onNavigateUp
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                FullScreenLoading()
            } else if (error != null) {
                ErrorMessage(error!!, onRetry = { /* reload */ })
            } else {
                meter?.let { m ->
                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Meter Info Card
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(m.code, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                    StatusBadge(m.status)
                                }
                                Text(m.description ?: "Sem descrição", style = MaterialTheme.typography.bodyMedium)
                                
                                Divider(Modifier.padding(vertical = 8.dp))
                                
                                InfoRow("Status da Vaga", if(m.status == "FREE") "Disponível" else "Ocupada", Icons.Default.Info)
                                InfoRow("Localização", "${m.latitude ?: 0.0}, ${m.longitude ?: 0.0}", Icons.Default.LocationOn)

                                // Mini Mapa na tela de detalhes
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    ParkingMapView(
                                        meters = listOf(m),
                                        onMeterSelected = {} // Desativado no mini-mapa
                                    )
                                }
                            }
                        }

                        if (m.status == "FREE") {
                            Text("Para iniciar a sessão, confirme os dados abaixo:", 
                                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                            OutlinedTextField(
                                value = confirmCode,
                                onValueChange = { confirmCode = it.uppercase() },
                                label = { Text("Confirme o ID da Vaga (Ex: PKM-001)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            OutlinedTextField(
                                value = vehiclePlate,
                                onValueChange = { vehiclePlate = it.uppercase() },
                                label = { Text("Placa do Veículo") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                placeholder = { Text("ABC-1234") }
                            )

                            Spacer(Modifier.weight(1f))

                            if (sessionState.error != null) {
                                Text(sessionState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            Button(
                                onClick = {
                                    if (confirmCode == m.code) {
                                        viewModel.startSession(m.code, vehiclePlate, onSessionStarted)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = confirmCode == m.code && vehiclePlate.length >= 7 && !sessionState.isStarting
                            ) {
                                if (sessionState.isStarting) {
                                    CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Text("Iniciar Estacionamento")
                                }
                            }
                        } else {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Esta vaga já está ocupada.", style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }
    }
}
