package com.smartparking.shared.ui.screens.user

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.smartparking.shared.model.ParkingMeterModel
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.annotations.MarkerOptions

@Composable
actual fun ParkingMapView(
    meters: List<ParkingMeterModel>,
    onMeterSelected: (String) -> Unit
) {
    // Usamos um estilo gratuito e público que não requer chave de API
    // O estilo "Demotiles" do MapLibre é garantido para funcionar sem chaves.
    val mapStyleUri = "https://demotiles.maplibre.org/style.json"

    AndroidView(
        factory = { context ->
            MapLibre.getInstance(context)
            
            MapView(context).apply {
                getMapAsync { map ->
                    map.setStyle(mapStyleUri) { style ->
                        // Adiciona marcadores para cada vaga
                        meters.forEach { meter ->
                            if (meter.latitude != null && meter.longitude != null) {
                                map.addMarker(
                                    MarkerOptions()
                                        .position(LatLng(meter.latitude, meter.longitude))
                                        .title(meter.code)
                                        .snippet(meter.description ?: "Vaga disponível")
                                )
                            }
                        }

                        // Centraliza na primeira vaga encontrada ou no Brasil
                        val firstMeter = meters.firstOrNull { it.latitude != null && it.longitude != null }
                        val initialPos = if (firstMeter != null) {
                            LatLng(firstMeter.latitude!!, firstMeter.longitude!!)
                        } else {
                            LatLng(-23.5505, -46.6333) // São Paulo
                        }

                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(initialPos, 14.0)
                        )

                        // Evento de clique no marcador
                        map.setOnMarkerClickListener { marker ->
                            onMeterSelected(marker.title)
                            true
                        }
                    }
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
