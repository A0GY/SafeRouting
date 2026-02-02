package com.universityofreading.demo

// ─── Android / Compose ──────────────────────────────────────
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import android.util.Log
import com.universityofreading.demo.util.DebugLogger
import android.widget.Toast

// ─── Google Maps SDK + utils ────────────────────────────────
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.model.Tile
import com.google.android.gms.maps.model.TileProvider
import kotlinx.coroutines.runBlocking
import com.universityofreading.demo.data.api.TileCacheManager
import com.universityofreading.demo.data.api.BackendTileService
import com.google.maps.android.heatmaps.*

// ─── Data / JSON / time ─────────────────────────────────────
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
// ─── Safest‑route imports ───────────────────────────────────
import com.universityofreading.demo.navigation.*
import com.universityofreading.demo.ui.theme.SafeRouteBottomSheet

/*────────────────────────────────────────────────────────────*/

enum class DateFilterOption { LAST_7_DAYS, LAST_30_DAYS, ALL }

@Composable
fun MapScreen() {

    /*── map & filter state ────────────────────────────────*/
    val context   = LocalContext.current
    val mapView   = remember { MapView(context) }
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    val isShowingMarkers = remember { mutableStateOf(false) }
    
    /*── safest‑route state ────────────────────────────────*/
    var startMarker  by remember { mutableStateOf<Marker?>(null) }
    var destMarker   by remember { mutableStateOf<Marker?>(null) }
    var routePolys   by remember { mutableStateOf<List<Polyline>>(emptyList()) }
    var bottomSheet  by remember { mutableStateOf(false) }
    var isSafestMode by remember { mutableStateOf(true) } // Default to safest route
    var drawJob      by remember { mutableStateOf<Job?>(null) }   // debounce handle
    // Flag to track which marker to place next
    var placingStart by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()
    
    // Initialize clients and tile cache on first composition
    LaunchedEffect(Unit) {
        // Initialize directions client for route calculation
        DirectionsClient.init(context)
        // Warm backend tile URLs (cache) for current zoom
        try {
            val tiles = com.universityofreading.demo.data.api.BackendCrimeClient.getTiles(zoom = 13)
            DebugLogger.logDebug("MapScreen", "Pre-fetched ${tiles.size} tile URLs from backend")
        } catch (e: Exception) {
            DebugLogger.logError("MapScreen", "Failed to fetch tile URLs: ${e.message}", e)
        }
        Log.d("MapScreen", "Directions API initialized, backend tile URLs warmed.")
    }
    
    // Note: route scoring is now performed by backend. We no longer create CrimeSpatialIndex here.
    val tileCache = remember { TileCacheManager(context.cacheDir) }

    /*── MapView lifecycle ─────────────────────────────────*/
    DisposableEffect(mapView) {
        mapView.onCreate(null); mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDestroy() }
    }

    /*──────────────── UI layout ───────────────────────────*/
    Column(Modifier.fillMaxSize()) {

        AndroidView(
            modifier = Modifier.weight(1f),
            factory  = {
                mapView.apply {
                    getMapAsync { map ->
                        googleMap = map
                        configureInfoWindow(map, context)
                        map.uiSettings.isZoomControlsEnabled = true
                        // Set initial camera position to London
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            LatLng(51.509865, -0.118092), 11f
                        ))
                        
                        // Add backend tile overlay (vector/raster) for crime visualization
                        // This replaces the client-side heatmap and bulk marker rendering
                        try {
                            val tileProvider = TileProvider { x, y, zoom ->
                                val tileUrl = BackendTileService.getTileUrl(zoom, x, y)
                                val data = runBlocking { tileCache.getTile(zoom, x, y, tileUrl) }
                                if (data != null) Tile(256, 256, data) else null
                            }
                            map.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
                            val persistentTileProvider = tileProvider
                            
                            // On zoom or pan, re-add tile overlay if cleared
                            map.setOnCameraIdleListener {
                                // Backend tile overlay provides all crime visualization
                                // No need to render client-side markers or heatmap
                            }
                        } catch (e: Exception) {
                            DebugLogger.logError("MapScreen", "Failed to add backend tile overlay: ${e.message}", e)
                        }

                        // Handle user clicks on map to place route markers
                        map.setOnMapClickListener { pt ->
                            DebugLogger.logClick("MapScreen", "Map clicked at: ${pt.latitude}, ${pt.longitude}")
                            
                            // Route planning does not depend on crime data
                            // Backend scoring will be performed when calculating routes
                            
                            if (placingStart) {
                                DebugLogger.logDebug("MapScreen", "Placing start marker")
                                // Remove existing start marker if there is one
                                startMarker?.remove()
                                
                                // Add draggable start marker
                                startMarker = map.addMarker(
                                    MarkerOptions()
                                        .position(pt)
                                        .title("Start")
                                        .draggable(true)
                                )
                                
                                DebugLogger.logDebug("MapScreen", "Start marker placed: ${startMarker != null}")
                                placingStart = false
                            } else {
                                DebugLogger.logDebug("MapScreen", "Placing destination marker")
                                // Remove existing destination marker if there is one
                                destMarker?.remove()
                                
                                // Add draggable destination marker
                                destMarker = map.addMarker(
                                    MarkerOptions()
                                        .position(pt)
                                        .title("Destination")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                                        .draggable(true)
                                )
                                
                                DebugLogger.logDebug("MapScreen", "Destination marker placed: ${destMarker != null}")
                                
                                // Show the bottom sheet with route options
                                bottomSheet = true
                                
                                // Calculate and draw the best route (backend-driven)
                                scope.launch {
                                    DebugLogger.logDebug("MapScreen", "Calculating route between markers")
                                    try {
                                        drawSafestRoute(
                                            map, startMarker!!, destMarker!!,
                                            isSafestMode, routePolys, context
                                        ) { routePolys = it }
                                        DebugLogger.logDebug("MapScreen", "Route calculation complete")
                                    } catch (e: Exception) {
                                        DebugLogger.logError("MapScreen", "Error drawing route", e)
                                    }
                                }
                            }
                        }
                        
                        // Handle marker drag events to recalculate routes
                        map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                            override fun onMarkerDrag(marker: Marker) {
                                // Do nothing while dragging
                            }
                            
                            override fun onMarkerDragStart(marker: Marker) {
                                // Do nothing when starting drag
                            }
                            
                            override fun onMarkerDragEnd(marker: Marker) {
                                // Only recalculate route if both markers exist
                                if (startMarker != null && destMarker != null) {
                                    DebugLogger.logDebug("MapScreen", "Marker dragged, recalculating route")
                                    
                                    scope.launch {
                                        try {
                                            drawSafestRoute(
                                                map, startMarker!!, destMarker!!,
                                                isSafestMode, routePolys, context
                                            ) { routePolys = it }
                                        } catch (e: Exception) {
                                            DebugLogger.logError("MapScreen", "Error recalculating route", e)
                                        }
                                    }
                                }
                            }
                        })
                    }
                }
            }
        )
    }

    /*── Bottom sheet with slider & buttons ───────────────*/
    if (bottomSheet && startMarker != null && destMarker != null) {

        // Extract route details for display
        val distKm = ((routePolys.firstOrNull()?.tag as? Double) ?: 0.0) / 1000
        val risk   = (routePolys.firstOrNull()?.zIndex?.toDouble() ?: 0.0) * 1_000_000  // scaled

        SafeRouteBottomSheet(
            distanceKm = distKm,
            riskScore = risk,
            highRiskSegments = 0, // We don't track high-risk segments in this screen
            isSafestMode = isSafestMode,
                onSafestModeChanged = { newMode ->
                isSafestMode = newMode
                googleMap?.let { map ->
                    drawJob?.cancel()          // stop previous coroutine
                    drawJob = scope.launch {
                        delay(100)             // small delay for UI responsiveness
                        drawSafestRoute(
                            map, startMarker!!, destMarker!!,
                            isSafestMode, routePolys, context
                        ) { routePolys = it }
                    }
                }
            },
            onStartNavigationClick = {
                // Zoom to fit the selected route
                routePolys.firstOrNull()?.let { poly ->
                    val bounds = LatLngBounds.builder()
                    poly.points.forEach { bounds.include(it) }
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds.build(), 80)
                    )
                }
                bottomSheet = false          // hide the sheet
            }
        )
    }
}

/*────────── Helper functions ───────────────────────────────*/

// Configure the popup info window for markers
private fun configureInfoWindow(map: GoogleMap, ctx: Context) {
    map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
        override fun getInfoWindow(marker: Marker) = null
        override fun getInfoContents(marker: Marker): View {
            val v = LayoutInflater.from(ctx).inflate(R.layout.marker_info_window, null)
            v.findViewById<ImageView>(R.id.info_window_icon)
                .setImageResource(R.drawable.ic_question_mark)
            // Show route marker details (start/destination)
            v.findViewById<TextView>(R.id.info_window_text).text = marker.title ?: "Location"
            return v
        }
    })
}

/*────── Helper functions ───────────────────────────────*/
    map: GoogleMap,
    start: Marker,
    dest: Marker,
    isSafestMode: Boolean,
    oldPolys: List<Polyline>,
    appContext: Context,
    update: (List<Polyline>) -> Unit
) {
    // Get the map's context for displaying Toast messages
    DebugLogger.logDebug("MapScreen", "drawSafestRoute called in ${if (isSafestMode) "SAFEST" else "FASTEST"} mode")
    // Remove existing polylines on the UI thread
    withContext(Dispatchers.Main) {
        oldPolys.forEach { it.remove() }
    }
    
    try {
        // Calculate safest route and alternatives (potentially heavy operation)
        DebugLogger.logDebug("MapScreen", "Calculating routes with SafeRoutePlanner")
        val routeResult = withContext(Dispatchers.Default) {
            SafeRoutePlanner.safestRoute(start.position, dest.position, isSafestMode)
        }
        
        val (best, alts) = routeResult
        DebugLogger.logDebug("MapScreen", "Routes calculated: 1 best route and ${alts.size} alternatives")
        
        // Verify that we have valid route data before proceeding
        if (best.route.overviewPolyline.encodedPath.isBlank()) {
            DebugLogger.logError("MapScreen", "Invalid route data: empty polyline")
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    appContext, 
                    "Could not calculate route: no path available", 
                    Toast.LENGTH_SHORT
                ).show()
            }
            return
        }
        
        // UI operations need to be on the Main thread
        withContext(Dispatchers.Main) {
            val newSet = mutableListOf<Polyline>()
            
            // Add primary route - color based on mode (green for safe, blue for fast)
            DebugLogger.logDebug("MapScreen", "Adding primary route to map")
            val routeColor = if (isSafestMode) 
                Color.rgb(0, 170, 0) // Green for safest
            else 
                Color.rgb(0, 0, 220) // Blue for fastest
                
            newSet += map.addPolyline(
                PolylineOptions().addAll(
                    PolylineUtils.decode(best.route.overviewPolyline.encodedPath)
                ).color(routeColor).width(12f)
            ).apply { 
                tag = best.distanceM 
                zIndex = best.riskScore.toFloat()
                DebugLogger.logDebug("MapScreen", "Primary route properties: distance=${best.distanceM}m, risk=${best.riskScore}")
            }
            
            // Add alternative routes (gray dashed lines)
            if (alts.isNotEmpty()) {
                DebugLogger.logDebug("MapScreen", "Adding ${alts.size} alternative routes")
                alts.forEach { c ->
                    if (c.route.overviewPolyline.encodedPath.isNotBlank()) {
                        newSet += map.addPolyline(
                            PolylineOptions().addAll(
                                PolylineUtils.decode(c.route.overviewPolyline.encodedPath)
                            ).color(Color.GRAY).pattern(listOf(Dot(), Gap(20f))).width(8f)
                        )
                    }
                }
            }
            
            // Update the UI with new polylines
            DebugLogger.logDebug("MapScreen", "Updating UI with ${newSet.size} route polylines")
            update(newSet)
        }
    } catch (e: Exception) {
        DebugLogger.logError("MapScreen", "Error in drawSafestRoute", e)
        withContext(Dispatchers.Main) {
            Toast.makeText(
                appContext, 
                "Error calculating route: ${e.message?.take(100)}", 
                Toast.LENGTH_SHORT
            ).show()
        }
        throw e
    }
}

// Add heatmap of crime data to the map
private fun addHeatMap(map: GoogleMap, crimeData: List<CrimeData>, opt: DateFilterOption) {
    // Load and filter crime data
    val crimes = filterCrimesByDate(crimeData, opt)
    
    // Convert to weighted points for heatmap
    val weighted = crimes.map {
        WeightedLatLng(LatLng(it.latitude, it.longitude), it.severity + 1)
    }
    
    // Create a gradient from green (low) to orange to red (high)
    val colors = intArrayOf(Color.rgb(81,255,0), Color.rgb(255,165,0), Color.rgb(255,0,0))
    
    val provider = HeatmapTileProvider.Builder()
        .weightedData(weighted)
        .gradient(Gradient(colors,floatArrayOf(0f,0.5f,1f)))
        .radius(50).build()
        
    map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
}

// Display individual markers for crimes when zoomed in
private fun showMarkers(map: GoogleMap, crimeData: List<CrimeData>, opt: DateFilterOption, markerIcon: BitmapDescriptor) {
    val crimes = filterCrimesByDate(crimeData, opt)
    
    // Add a marker for each crime
    crimes.forEach { c ->
        map.addMarker(
            MarkerOptions().position(LatLng(c.latitude,c.longitude)).icon(markerIcon)
        )?.tag = c
    }
}

// Filter crime data based on selected date range
private fun filterCrimesByDate(list: List<CrimeData>, opt: DateFilterOption): List<CrimeData> {
    if (opt == DateFilterOption.ALL) return list
    val now = LocalDate.now()
    return list.filter {
        val d = try { LocalDate.parse(it.date, DateTimeFormatter.ISO_LOCAL_DATE) } catch(e:Exception){return@filter false}
        when (opt) {
            DateFilterOption.LAST_7_DAYS  -> !d.isBefore(now.minusDays(7))
            DateFilterOption.LAST_30_DAYS -> !d.isBefore(now.minusDays(30))
            else -> true
        }
    }
}

// Convert vector drawable to a bitmap for map marker
private fun bitmapDescriptorFromVector(ctx: Context, id: Int, scale: Float): BitmapDescriptor {
    // For PNG resources, we can use BitmapDescriptorFactory directly
    if (id == R.drawable.ic_question_mark) {
        return BitmapDescriptorFactory.fromResource(R.drawable.ic_question_mark)
    }
    
    // For vector drawables (not used in this case but kept for flexibility)
    val d = ContextCompat.getDrawable(ctx,id) ?: return BitmapDescriptorFactory.defaultMarker()
    val w = (d.intrinsicWidth*scale).toInt(); val h=(d.intrinsicHeight*scale).toInt()
    d.setBounds(0,0,w,h)
    val bmp = Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888)
    d.draw(Canvas(bmp))
    return BitmapDescriptorFactory.fromBitmap(bmp)
}
