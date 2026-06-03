package com.smartparking.shared.ui.screens.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.smartparking.shared.model.RechargeRequest
import com.smartparking.shared.model.WalletTransactionModel
import com.smartparking.shared.ui.components.*
import com.smartparking.shared.ui.theme.*
import com.smartparking.shared.util.format
import com.smartparking.shared.viewmodel.WalletViewModel

// ── WalletScreen ──────────────────────────────────────────────────────────────

@Composable
fun WalletScreen(
    viewModel: WalletViewModel,
    onNavigateUp: () -> Unit,
    onNavigateToRecharge: () -> Unit,
    onNavigateToExtract: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadWallet() }

    Scaffold(
        topBar = { PontoLivreTopBar(title = "Minha Carteira", onNavigateUp = onNavigateUp) }
    ) { padding ->
        if (state.isLoading) {
            FullScreenLoading()
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Balance card
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Column(
                        Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text("Saldo disponível",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
                        Text("R$ ${"%.2f".format(state.balance)}",
                            fontSize = 40.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary)
                    }
                }

                // Recharge options
                Text("Recarregar via", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RechargeOptionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.PixIcon,
                        label = "Pix",
                        description = "Aprovação imediata",
                        onClick = onNavigateToRecharge
                    )
                    RechargeOptionCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.CreditCard,
                        label = "Cartão",
                        description = "Crédito aprovado",
                        onClick = onNavigateToRecharge
                    )
                }

                // Recent transactions
                SectionHeader(title = "Últimas transações", action = {
                    TextButton(onClick = onNavigateToExtract) { Text("Ver todas") }
                })

                if (state.transactions.isEmpty()) {
                    EmptyState("Nenhuma transação ainda.", Icons.Default.ReceiptLong)
                } else {
                    state.transactions.take(5).forEach { tx ->
                        TransactionRow(tx)
                    }
                }
            }
        }
    }
}

@Composable
private fun RechargeOptionCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── RechargeScreen ────────────────────────────────────────────────────────────

@Composable
fun RechargeScreen(
    viewModel: WalletViewModel,
    onNavigateUp: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    var selectedMethod by remember { mutableStateOf("PIX") }
    var amount by remember { mutableStateOf("") }

    // Card fields
    var cardNumber by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var cardExpiry by remember { mutableStateOf("") }
    var cardCvv by remember { mutableStateOf("") }

    // Pix result dialog
    var showPixDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.rechargeResponse) {
        if (state.rechargeResponse?.paymentMethod == "PIX") {
            showPixDialog = true
        }
    }

    Scaffold(
        topBar = { PontoLivreTopBar(title = "Recarregar Carteira", onNavigateUp = onNavigateUp) }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Amount
            Text("Valor a recarregar", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)

            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("Valor (R$)") },
                leadingIcon = { Text("R$", Modifier.padding(start = 12.dp),
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Quick amounts
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("10", "20", "50", "100").forEach { v ->
                    FilterChip(
                        selected = amount == v,
                        onClick = { amount = v },
                        label = { Text("R$ $v") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            HorizontalDivider()

            // Payment method selector
            Text("Forma de pagamento", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("PIX" to Icons.Default.PixIcon, "CREDIT_CARD" to Icons.Default.CreditCard)
                    .forEach { (method, icon) ->
                        val selected = selectedMethod == method
                        OutlinedCard(
                            modifier = Modifier.weight(1f).clickable { selectedMethod = method },
                            border = if (selected)
                                CardDefaults.outlinedCardBorder().copy(
                                    width = 2.dp
                                ) else CardDefaults.outlinedCardBorder()
                        ) {
                            Row(Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(icon, contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(if (method == "PIX") "Pix" else "Cartão",
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
            }

            // Card fields (only when CREDIT_CARD selected)
            if (selectedMethod == "CREDIT_CARD") {
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Dados do cartão (fictícios)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)

                        OutlinedTextField(value = cardNumber,
                            onValueChange = { cardNumber = it.filter(Char::isDigit).take(16) },
                            label = { Text("Número do cartão") },
                            placeholder = { Text("0000 0000 0000 0000") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), singleLine = true)

                        OutlinedTextField(value = cardHolder,
                            onValueChange = { cardHolder = it.uppercase() },
                            label = { Text("Nome no cartão") },
                            modifier = Modifier.fillMaxWidth(), singleLine = true)

                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(value = cardExpiry,
                                onValueChange = { cardExpiry = it.take(5) },
                                label = { Text("Validade") },
                                placeholder = { Text("MM/AA") },
                                modifier = Modifier.weight(1f), singleLine = true)

                            OutlinedTextField(value = cardCvv,
                                onValueChange = { cardCvv = it.filter(Char::isDigit).take(3) },
                                label = { Text("CVV") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f), singleLine = true)
                        }
                    }
                }
            }

            if (state.error != null) {
                ErrorMessage(state.error!!)
            }

            state.successMessage?.let {
                if (state.rechargeResponse?.paymentMethod == "CREDIT_CARD") {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f))) {
                        Row(Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null,
                                tint = SuccessGreen)
                            Text(it, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val amountDouble = amount.toDoubleOrNull() ?: return@Button
                    viewModel.recharge(
                        RechargeRequest(
                            amount = amountDouble,
                            paymentMethod = selectedMethod,
                            cardNumber = cardNumber.ifBlank { null },
                            cardHolder = cardHolder.ifBlank { null },
                            cardExpiry = cardExpiry.ifBlank { null },
                            cardCvv = cardCvv.ifBlank { null }
                        ),
                        onSuccess = {}
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !state.isRecharging && amount.toDoubleOrNull() != null
            ) {
                if (state.isRecharging) {
                    CircularProgressIndicator(Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(
                        if (selectedMethod == "PIX") "Gerar QR Code Pix" else "Confirmar pagamento",
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // Pix QR Code dialog
    if (showPixDialog && state.rechargeResponse != null) {
        val resp = state.rechargeResponse!!
        AlertDialog(
            onDismissRequest = {
                showPixDialog = false
                viewModel.clearMessages()
            },
            title = { Text("QR Code Pix", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary)
                    Text("Escaneie o QR Code ou copie a chave Pix",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Column(Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Chave Pix:", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(resp.pixKey ?: "smartparking@pix.com.br",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold)
                            HorizontalDivider(Modifier.padding(vertical = 4.dp))
                            Text("Valor: R$ ${"%.2f".format(resp.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen)
                            Text("Ref: ${resp.referenceCode}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = SuccessGreen.copy(alpha = 0.12f))) {
                        Row(Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null,
                                tint = SuccessGreen, modifier = Modifier.size(20.dp))
                            Text("Pagamento aprovado! Saldo adicionado.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    showPixDialog = false
                    viewModel.clearMessages()
                }) { Text("Fechar") }
            }
        )
    }
}

// ── ExtractScreen ─────────────────────────────────────────────────────────────

@Composable
fun ExtractScreen(
    viewModel: WalletViewModel,
    onNavigateUp: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadWallet() }

    Scaffold(
        topBar = { PontoLivreTopBar(title = "Extrato", onNavigateUp = onNavigateUp) }
    ) { padding ->
        if (state.isLoading) {
            FullScreenLoading()
        } else if (state.transactions.isEmpty()) {
            EmptyState("Nenhuma transação encontrada.", Icons.Default.ReceiptLong)
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.transactions) { tx ->
                    TransactionRow(tx)
                }
            }
        }
    }
}

@Composable
fun TransactionRow(tx: WalletTransactionModel) {
    val isCredit = tx.type.startsWith("CREDIT")
    val color = if (isCredit) SuccessGreen else ErrorRed
    val sign = if (isCredit) "+" else "-"
    val icon = when (tx.type) {
        "CREDIT_PIX"   -> Icons.Default.PixIcon
        "CREDIT_CARD"  -> Icons.Default.CreditCard
        "DEBIT_SESSION"-> Icons.Default.LocalParking
        "DEBIT_FINE"   -> Icons.Default.Warning
        else           -> Icons.Default.SwapHoriz
    }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null,
                    tint = color, modifier = Modifier.size(28.dp))
                Column {
                    Text(tx.description,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2)
                    Text(tx.createdAt.take(10),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("$sign R$ ${"%.2f".format(tx.amount)}",
                    fontWeight = FontWeight.Bold, color = color)
                Text("Saldo: R$ ${"%.2f".format(tx.balanceAfter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// Placeholder icon for Pix (not in default Icons.Default)
internal val Icons.Filled.PixIcon get() = Icons.Filled.QrCode
