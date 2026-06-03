package com.smartparking.shared.ui.screens.user

import androidx.compose.runtime.Composable
import org.jetbrains.compose.web.dom.Div
import org.jetbrains.compose.web.dom.Text
import com.smartparking.shared.model.ParkingMeterModel

@Composable
actual fun ParkingMapView(
    meters: List<ParkingMeterModel>,
    onMeterSelected: (String) -> Unit
) {
    Div {
        Text("O mapa não está disponível na versão Web.")
    }
}
