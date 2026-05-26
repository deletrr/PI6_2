package com.parquimetro.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.parquimetro.android.app
import com.parquimetro.dto.VagaResponse
import com.parquimetro.feature.motorista.MotoristaViewModel
import kotlinx.coroutines.launch

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("motorista") { MotoristaScreen() }
        composable("fiscal") { FiscalScreen() }
    }
}

@Composable
fun LoginScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Parquimetro V7", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(32.dp))
        OutlinedTextField(
            value = username, onValueChange = { username = it },
            label = { Text("Usuario") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it },
            label = { Text("Senha") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !carregando,
            onClick = {
                carregando = true
                scope.launch {
                    runCatching {
                        val resp = ctx.app.apiClient.http
                        ctx.app.setToken("token_placeholder")
                        nav.navigate("motorista")
                    }.onFailure { carregando = false }
                }
            }
        ) {
            if (carregando) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Entrar como Motorista")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { nav.navigate("fiscal") }
        ) { Text("Entrar como Fiscal") }
    }
}

@Composable
fun MotoristaScreen() {
    val ctx = LocalContext.current
    val vm = remember { ctx.app.motoristaViewModel() }
    val vagas by vm.vagas.collectAsState()
    val erro by vm.erro.collectAsState()

    LaunchedEffect(Unit) { vm.carregarVagas() }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Vagas Disponiveis") })
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            if (erro != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    Text(erro!!, Modifier.padding(12.dp))
                }
            }
            LazyColumn {
                items(vagas) { vaga -> VagaCard(vaga, vm) }
            }
        }
    }
}

@Composable
fun VagaCard(vaga: VagaResponse, vm: MotoristaViewModel) {
    val scope = rememberCoroutineScope()
    var pagando by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.LocationOn, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(vaga.codigo, style = MaterialTheme.typography.titleMedium)
                Text(vaga.status, style = MaterialTheme.typography.bodySmall)
                vaga.battery?.let { Text("Bateria: $it%", style = MaterialTheme.typography.bodySmall) }
            }
            if (vaga.status == "LIVRE") {
                Button(
                    enabled = !pagando,
                    onClick = {
                        pagando = true
                        vm.pagar(vaga.id, "000.000.000-00", "ABC1D23", 60) { pagando = false }
                    }
                ) { Text("Pagar 1h") }
            }
        }
    }
}

@Composable
fun FiscalScreen() {
    val ctx = LocalContext.current
    val vm = remember { ctx.app.fiscalViewModel("fiscal-001") }
    val estado by vm.estado.collectAsState()
    var vagas by remember { mutableStateOf<List<VagaResponse>>(emptyList()) }
    var vagaSelecionada by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        runCatching { vagas = ctx.app.vagaRepository.listar() }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Painel do Fiscal") }) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text("Selecionar vaga irregular:", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f)) {
                items(vagas.filter { it.status == "IRREGULAR" || it.status == "OCUPADA" }) { vaga ->
                    FilterChip(
                        selected = vagaSelecionada == vaga.id,
                        onClick = { vagaSelecionada = vaga.id },
                        label = { Text("${vaga.codigo} - ${vaga.status}") },
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = vagaSelecionada != null,
                onClick = {
                    val id = vagaSelecionada ?: return@Button
                    val imagemFake = ByteArray(1024)
                    vm.registrarInfracao(id, imagemFake)
                }
            ) { Text("Registrar Infracao com Foto") }
            if (estado != null) {
                Spacer(Modifier.height(8.dp))
                Text(estado!!, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
