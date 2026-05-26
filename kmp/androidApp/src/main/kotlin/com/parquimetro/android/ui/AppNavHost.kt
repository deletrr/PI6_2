package com.parquimetro.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController, startDestination = "login") {
        composable("login") { LoginScreen(navController) }
        composable("motorista") { MotoristaScreen(navController) }
        composable("fiscal") { FiscalScreen(navController) }
    }
}

@Composable
fun LoginScreen(nav: NavHostController) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Parquímetro V7", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(user, { user = it }, label = { Text("Usuário") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(pass, { pass = it }, label = { Text("Senha") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { nav.navigate("motorista") }, modifier = Modifier.fillMaxWidth()) {
            Text("Motorista")
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { nav.navigate("fiscal") }, modifier = Modifier.fillMaxWidth()) {
            Text("Fiscal")
        }
    }
}

@Composable
fun MotoristaScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Vagas Disponíveis", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        // TODO: LazyColumn com VagaCards + VagaMap
        Text("Mapa e listagem de vagas")
    }
}

@Composable
fun FiscalScreen(nav: NavHostController) {
    var status by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Painel do Fiscal", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        // CameraModule dispara FiscalViewModel.registrarInfracao
        Button(onClick = { /* lançar câmera */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Fotografar Infração")
        }
        if (status.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(status, color = MaterialTheme.colorScheme.primary)
        }
    }
}
