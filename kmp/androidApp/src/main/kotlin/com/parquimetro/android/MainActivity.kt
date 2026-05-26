package com.parquimetro.android

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.core.content.ContextCompat
import com.parquimetro.android.ui.AppNavHost

class MainActivity : ComponentActivity() {

    private val permissaoLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* sem-op: UI reage ao state */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        solicitarPermissoes()
        setContent {
            MaterialTheme {
                AppNavHost()
            }
        }
    }

    private fun solicitarPermissoes() {
        val needed = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA
        ).filter { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }
            .toTypedArray()
        if (needed.isNotEmpty()) permissaoLauncher.launch(needed)
    }
}
