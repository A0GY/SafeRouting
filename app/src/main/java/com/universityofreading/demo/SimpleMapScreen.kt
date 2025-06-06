package com.universityofreading.demo

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng

@Composable
fun SimpleMapScreen() {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    // Handle the MapView lifecycle
    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDestroy()
        }
    }

    // Display the map
    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { googleMap ->
                    // Set initial camera position to London
                    val london = LatLng(51.509865, -0.118092)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(london, 11f))
                    googleMap.uiSettings.isZoomControlsEnabled = true
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
} 