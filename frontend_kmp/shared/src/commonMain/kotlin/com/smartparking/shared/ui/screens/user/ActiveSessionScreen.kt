package com.smartparking.shared.ui.screens.user

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartparking.shared.ui.components.*
import com.smartparking.shared.ui.theme.*
import com.smartparking.shared.util.format
import com.smartparking.shared.viewmodel.SessionViewModel
import kotlinx.coroutines.delay

@Composable
fun ActiveSessionScreen(
    viewModel: SessionViewModel,
    onNavigateUp: () -> Unit,
    onSessionEnded: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showEndConfirm by remember { mutableStateOf(false) }

    // Local elapsed timer — updates every second
    var elapsedSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(state.activeSession) {
        val session = state.activeSession ?: return@LaunchedEffect
        elapsedSeconds = (session.elapsedMinutes ?: 0L) * 60
        while (true) {
            delay(1000)
            elapsedSeconds++
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadActiveSession()
        viewModel.startPolling()
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPolling() }
    }

    LaunchedEffect(state.successMessage) {
        if (state.successMessage != null && state.activeSession == null) {
            onSessionEnded()
        }
    }

    Scaffold(
        topBar = {
            PontoLivreTopBar(title = "Sessão Ativa", onNavigateUp = onNavigateUp)
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                FullScreenLoading()
            } else if (state.activeSession == null) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    EmptyState("Nenhuma sessão ativa no momento.", Icons.Default.DirectionsCar)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNavigateUp) { Text("Voltar") }
                }
            } else {
                val session = state.activeSession!!
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Animated pulsing indicator
                    Box(contentAlignment = Alignment.Center) {
                        PulsingRing()
                        Box(
                            Modifier.size(96.dp).clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Text("EM ANDAMENTO", style = MaterialTheme.typography.labelLarge,
                        color = SuccessGreen, fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp)

                    // Timer
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tempo decorrido", style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                formatElapsed(elapsedSeconds),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            val freeMinutesLeft = maxOf(0, 15 - (elapsedSeconds / 60).toInt())
                            if (freeMinutesLeft > 0) {
                                Surface(
                                    color = SuccessGreen.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text("$freeMinutesLeft min de tolerância restantes",
                                        Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Cost estimate
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Row(Modifier.fillMaxWidth().padding(20.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Custo estimado",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                                Text("R$ 2,00 por hora iniciada",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f))
                            }
                            Text("R$ ${"%.2f".format(session.estimatedCost ?: 0.0)}",
                                fontSize = 24.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }

                    // Session info
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Detalhes da sessão",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold)
                            HorizontalDivider()
                            InfoRow("Vaga", session.meterCode, Icons.Default.LocalParking)
                            session.meterDescription?.let {
                                InfoRow("Local", it, Icons.Default.Place)
                            }
                            InfoRow("Início", formatTime(session.startTime), Icons.Default.Schedule)
                            InfoRow("Tolerância até", formatTime(session.freeUntil), Icons.Default.Timer)
                        }
                    }

                    if (state.error != null) {
                        ErrorMessage(state.error!!)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { showEndConfirm = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        enabled = !state.isEnding
                    ) {
                        if (state.isEnding) {
                            CircularProgressIndicator(Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onError)
                        } else {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("FINALIZAR SESSÃO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }

            if (state.isEnding) LoadingOverlay()
        }
    }

    if (showEndConfirm) {
        ConfirmDialog(
            title = "Finalizar sessão?",
            message = "O valor será debitado do seu saldo conforme o tempo de permanência.",
            confirmLabel = "Finalizar",
            onConfirm = {
                showEndConfirm = false
                state.activeSession?.let { viewModel.endSession(it.id, onSessionEnded) }
            },
            onDismiss = { showEndConfirm = false }
        )
    }
}

@Composable
private fun PulsingRing() {
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.4f, label = "scale",
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 0f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse)
    )
    Box(
        Modifier
            .size((96 * scale).dp)
            .clip(CircleShape)
            .background(SuccessGreen.copy(alpha = alpha))
    )
}

private fun formatElapsed(totalSeconds: Long): String {
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60

    val hh = h.toString().padStart(2, '0')
    val mm = m.toString().padStart(2, '0')
    val ss = s.toString().padStart(2, '0')

    return if (h > 0) "$hh:$mm:$ss" else "$mm:$ss"
}

private fun formatTime(isoString: String): String {
    return try {
        // "2024-06-03T10:30:00" → "10:30"
        val timePart = isoString.substringAfter("T").take(5)
        timePart
    } catch (e: Exception) { isoString }
}
