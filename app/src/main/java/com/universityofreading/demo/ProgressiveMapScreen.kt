package com.universityofreading.demo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.*
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.clustering.ClusterManager
import com.google.maps.android.clustering.view.DefaultClusterRenderer
import com.google.maps.android.heatmaps.*
import com.universityofreading.demo.data.CrimeData
import com.universityofreading.demo.data.CrimeDataRepository
import com.universityofreading.demo.data.api.TileCacheManager
import com.universityofreading.demo.data.api.BackendTileService
import kotlinx.coroutines.runBlocking
import com.universityofreading.demo.navigation.*
import com.universityofreading.demo.ui.theme.CompactTimeControls
import com.universityofreading.demo.ui.theme.SafeRouteBottomSheet
import com.universityofreading.demo.ui.theme.TimeRiskIndicator
import com.universityofreading.demo.ui.theme.TimeSimulationSlider
import com.universityofreading.demo.util.DebugLogger
import kotlinx.coroutines.*
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.DateTimeParseException
import kotlin.math.min
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.universityofreading.demo.navigation.AreaAnalysis
import com.universityofreading.demo.ui.theme.CustomAreaAnalysisDialog
import com.universityofreading.demo.ui.theme.DrawModeButton
import com.universityofreading.demo.ui.theme.DoneButton

// Define TAG constant for logging at the file level
private const val TAG = "ProgressiveMapScreen"

// Map crime types to emoji icons
private val crimeTypeIcons = mapOf(
    // Standard UK police crime categories
    "anti-social-behaviour" to "🔊",        // Loud speaker
    "bicycle-theft" to "🚲",               // Bicycle
    "burglary" to "🏠",                    // House
    "criminal-damage-arson" to "🔥",       // Fire
    "drugs" to "💊",                       // Pill
    "other-crime" to "❓",                 // Question mark
    "other-theft" to "💼",                 // Briefcase
    "possession-of-weapons" to "🔪",       // Knife
    "public-order" to "🧑‍⚖️",               // Judge
    "robbery" to "💰",                     // Money bag
    "shoplifting" to "🛒",                 // Shopping cart
    "theft-from-the-person" to "👜",       // Handbag
    "vehicle-crime" to "🚗",               // Car
    "violent-crime" to "👊",               // Fist
    "violence-and-sexual-offences" to "👊", // Fist for violence
    
    // Additional aliases and variations to improve matching
    "violence" to "👊",
    "sexual" to "⚠️",                      // Warning sign for sexual offenses
    "weapon" to "🔪",
    "theft" to "💼",
    "car" to "🚗",
    "vehicle" to "🚗",
    "bike" to "🚲",
    "cycle" to "🚲",
    "arson" to "🔥",
    "fire" to "🔥",
    "drug" to "💊",
    "public" to "🧑‍⚖️",
    "order" to "🧑‍⚖️",
    "shop" to "🛒",
    "burglary" to "🏠",
    "house" to "🏠",
    "residential" to "🏠",
    "anti-social" to "🔊",
    "noise" to "🔊"
)

/**
 * Extension function to get appropriate emoji for a crime type
 */
private fun CrimeData.getEmoji(): String {
    val crimeType = this.type.lowercase().trim()
    
    // Direct match attempt first
    val directMatch = crimeTypeIcons[crimeType]
    if (directMatch != null) {
        Log.d(TAG, "Found direct emoji match for crime type: $crimeType → $directMatch")
        return directMatch
    }
    
    // Try partial matching if direct match fails
    val partialMatch = crimeTypeIcons.entries.find { (key, _) ->
        crimeType.contains(key) || key.contains(crimeType)
    }
    
    // Log the matching process for debugging
    if (partialMatch != null) {
        Log.d(TAG, "Found partial emoji match for crime type: $crimeType → ${partialMatch.value} (matched with ${partialMatch.key})")
        return partialMatch.value
    } else {
        Log.d(TAG, "No emoji match found for crime type: $crimeType, using default ❓")
        return "❓"
    }
}

// Class to handle ProgressiveMapScreen related static properties
class ProgressiveMapScreenUtil {
    companion object {
        // Additional static properties can go here
    }
}

/**
 * Date filter enum with options that make sense for the Police API data
 * (which has a 2-month delay in data availability)
 */
enum class ProgressiveDateFilterOption {
    MOST_RECENT_MONTH,  // Only data from the most recent available month
    LAST_3_MONTHS,      // Data from the last 3 available months
    ALL_DATA            // All available data
}

/**
 * ClusterItem implementation for CrimeData to enable clustering
 */
class CrimeMarkerItem(
    val crimeData: CrimeData,
    private val position: LatLng = LatLng(crimeData.latitude, crimeData.longitude),
    private val title: String = crimeData.type,
    private val snippet: String = "Date: ${crimeData.date}"
) : ClusterItem {
    override fun getPosition(): LatLng = position
    override fun getTitle(): String = title
    override fun getSnippet(): String = snippet
    override fun getZIndex(): Float = crimeData.severity.toFloat()
}

/**
 * A version of MapScreen that adds features progressively
 * with proper error handling
 */
@Composable
fun ProgressiveMapScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val mapView = remember { MapView(context) }
    
    // State variables
    var googleMap by remember { mutableStateOf<GoogleMap?>(null) }
    var crimeData by remember { mutableStateOf<List<CrimeData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var debugInfo by remember { mutableStateOf("Initializing...") }
    var isShowingMarkers by remember { mutableStateOf(false) }
    var currentJob by remember { mutableStateOf<Job?>(null) }
    var selectedDateFilter by remember { mutableStateOf(ProgressiveDateFilterOption.ALL_DATA) }
    
    // Time simulation state
    var isUsingCurrentTime by remember { mutableStateOf(true) }
    var simulatedHour by remember { mutableStateOf(LocalTime.now().hour) }
    var showTimeSimulator by remember { mutableStateOf(false) }
    
    // Draw mode state variables
    var isDrawModeActive by remember { mutableStateOf(false) }
    var drawnPoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var drawnPolygon by remember { mutableStateOf<Polygon?>(null) }
    var showAnalysisDialog by remember { mutableStateOf(false) }
    var currentAnalysis by remember { mutableStateOf<AreaAnalysis?>(null) }
    
    // Update the SafeRoutePlanner with the simulated hour when it changes
    LaunchedEffect(simulatedHour, isUsingCurrentTime) {
        SafeRoutePlanner.setSimulatedHour(if (isUsingCurrentTime) null else simulatedHour)
    }
    
    // Time-based risk state (updated periodically)
    var currentTimePeriod by remember { mutableStateOf(SafeRoutePlanner.getCurrentTimePeriod()) }
    var currentRiskLevel by remember { mutableStateOf(SafeRoutePlanner.getCurrentTimeRiskLevel()) }
    
    // Update time period and risk level every minute
    LaunchedEffect(Unit) {
        while(true) {
            currentTimePeriod = SafeRoutePlanner.getCurrentTimePeriod()
            currentRiskLevel = SafeRoutePlanner.getCurrentTimeRiskLevel()
            
            // If using current time, update the simulated hour too
            if (isUsingCurrentTime) {
                simulatedHour = LocalTime.now().hour
            }
            
            delay(60000) // Update every minute
        }
    }
    
    // Routing state
    var startMarker by remember { mutableStateOf<Marker?>(null) }
    var destMarker by remember { mutableStateOf<Marker?>(null) }
    var routePolys by remember { mutableStateOf<List<Polyline>>(emptyList()) }
    var showBottomSheet by remember { mutableStateOf(false) }
    var isSafestMode by remember { mutableStateOf(true) } // Default to safest route
    var routeDrawJob by remember { mutableStateOf<Job?>(null) }
    var placingStart by remember { mutableStateOf(true) } // Flag to toggle start/dest marker placement
    
    // Helper function to clear route markers and polylines
    fun clearRoute() {
        startMarker?.remove()
        destMarker?.remove()
        startMarker = null
        destMarker = null
        
        // Clear polylines
        routePolys.forEach { it.remove() }
        routePolys = emptyList()
        
        // Reset UI state
        showBottomSheet = false
    }
    
    // Helper function to calculate the safest route between two points
    suspend fun calculateSafestRoute(map: GoogleMap, start: LatLng, dest: LatLng) {
        routeDrawJob?.cancel()
        debugInfo = "Calculating ${if (isSafestMode) "safest" else "fastest"} route..."
        DebugLogger.logDebug(TAG, "Starting route calculation from $start to $dest")
        
        routeDrawJob = scope.launch(Dispatchers.Default) {
            try {
                // Create temporary markers to get positions
                val startMarkerOpt = MarkerOptions().position(start)
                val destMarkerOpt = MarkerOptions().position(dest)
                
                // Add temporary markers in the main thread
                val tempStartMarker = withContext(Dispatchers.Main) { 
                    map.addMarker(startMarkerOpt)
                }
                val tempDestMarker = withContext(Dispatchers.Main) { 
                    map.addMarker(destMarkerOpt)
                }
                
                DebugLogger.logDebug(TAG, "Temporary markers added: start=${tempStartMarker != null}, dest=${tempDestMarker != null}")
                
                if (tempStartMarker != null && tempDestMarker != null) {
                    // Calculate route on default dispatcher
                    DebugLogger.logDebug(TAG, "Calling calculateAndDrawRoute...")
                    // Use launch instead of withContext to avoid returning Unit
                    launch(Dispatchers.Main) {
                        calculateAndDrawRoute(
                            map,
                            tempStartMarker,
                            tempDestMarker,
                            isSafestMode,
                            routePolys
                        ) { newPolys ->
                            routePolys = newPolys
                            DebugLogger.logDebug(TAG, "Route calculation complete. Got ${newPolys.size} polylines.")
                        }
                    }
                    
                    // Clean up temporary markers
                    withContext(Dispatchers.Main) {
                        tempStartMarker.remove()
                        tempDestMarker.remove()
                    }
                    
                    // Restore actual markers on main thread
                    withContext(Dispatchers.Main) {
                        // Remove previous markers if they exist
                        startMarker?.remove()
                        destMarker?.remove()
                        
                        // Create new markers
                        startMarker = map.addMarker(
                            MarkerOptions()
                                .position(start)
                                .title("Start")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                .draggable(true)
                        )
                        
                        destMarker = map.addMarker(
                            MarkerOptions()
                                .position(dest)
                                .title("Destination")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                .draggable(true)
                        )
                        
                        // Show the route options sheet
                        showBottomSheet = true
                        debugInfo = "Route calculated. Adjust safety preference as needed."
                        DebugLogger.logDebug(TAG, "Route options shown in bottom sheet")
                    }
                } else {
                    DebugLogger.logError(TAG, "Failed to create temporary markers for route calculation")
                    throw Exception("Failed to create temporary markers for route calculation")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage = "Error calculating route: ${e.message}"
                    DebugLogger.logError(TAG, "Route calculation failed", e)
                }
                throw e
            }
        }
    }
    
    // Constants
    val MAX_MARKERS = 500 // Maximum number of markers to display
    val ZOOM_THRESHOLD = 15.5f // Increased zoom threshold for showing markers
    
    // Lifecycle management for MapView
    DisposableEffect(mapView) {
        mapView.onCreate(null)
        mapView.onResume()
        onDispose {
            currentJob?.cancel()
            routeDrawJob?.cancel()
            mapView.onPause()
            mapView.onDestroy()
        }
    }
    
    val tileCache = remember { TileCacheManager(context.cacheDir) }
    // Initialize directions API and tile overlay instead of loading local crime JSON
    LaunchedEffect(Unit) {
        try {
            debugInfo = "Initializing map and backend clients..."
            isLoading = true
            DirectionsClient.init(context)
            DebugLogger.logDebug(TAG, "DirectionsClient initialized")
            debugInfo = "Map setup complete"
            isLoading = false
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing map or clients", e)
            isLoading = false
            errorMessage = "Initialization error: ${e.message}"
        }
    }

    // Effect to update map when map, data or filter changes
    LaunchedEffect(googleMap, crimeData, selectedDateFilter) {
        val map = googleMap
        if (map != null) {
            try {
                // Clear previous overlays
                map.clear()
                
                // Add backend tile overlay instead of client-side heatmap
                val tileProvider = TileProvider { x, y, zoom ->
                    val tileUrl = BackendTileService.getTileUrl(zoom, x, y)
                    val data = runBlocking { tileCache.getTile(zoom, x, y, tileUrl) }
                    if (data != null) Tile(256, 256, data) else null
                }
                map.addTileOverlay(TileOverlayOptions().tileProvider(tileProvider))
                setupInfoWindowAdapter(context, map)
                debugInfo = "Map setup complete with ${selectedDateFilter.name} filter"
                Log.d(TAG, "Map setup complete with ${selectedDateFilter.name} filter")
            } catch (e: Exception) {
                Log.e(TAG, "Error setting up map components", e)
                errorMessage = "Error setting up map: ${e.message}"
                debugInfo += "\nSetup error: ${e.message}"
            }
        }
    }

    // Effect to recalculate route when the simulated time changes
    LaunchedEffect(simulatedHour, isUsingCurrentTime) {
        // Only recalculate if we already have a route displayed
        if (startMarker != null && destMarker != null && googleMap != null) {
            // Avoid recalculating immediately at startup
            if (currentJob == null) {
                return@LaunchedEffect
            }
            
            delay(300) // Small delay to avoid too frequent recalculations
            
            DebugLogger.logDebug(TAG, "Recalculating route due to time change: ${if (isUsingCurrentTime) "current time" else "simulated $simulatedHour:00"}")
            calculateAndDrawRoute(
                googleMap!!,
                startMarker!!,
                destMarker!!,
                isSafestMode,
                routePolys
            ) { newPolys ->
                routePolys = newPolys
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Date filter options at the top
        DateFilterRow(
            selectedFilter = selectedDateFilter,
            onFilterSelected = { newFilter -> 
                selectedDateFilter = newFilter
                // Reset marker visibility when changing filters
                isShowingMarkers = false
            }
        )
        
        // Map view
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = {
                    mapView.apply {
                        getMapAsync { map ->
                            debugInfo = "Map loaded, waiting for crime data..."
                            Log.d(TAG, "Map loaded, waiting for crime data")
                            googleMap = map
                            map.uiSettings.apply {
                                isZoomControlsEnabled = true
                                isMapToolbarEnabled = true
                                isCompassEnabled = true
                            }
                            
                            // Set initial camera position to London
                            val london = LatLng(51.509865, -0.118092)
                            map.moveCamera(CameraUpdateFactory.newLatLngZoom(london, 11f))
                            
                            // Setup info window adapter for markers
                            setupInfoWindowAdapter(context, map)
                            
                            // Set up the map click listener handler
                            val mapClickHandler: (LatLng) -> Unit = { latLng ->
                                if (isDrawModeActive) {
                                    // Drawing mode logic...
                                    drawnPoints = drawnPoints + latLng
                                    
                                    // Update polygon on map
                                    drawnPolygon?.remove()
                                    
                                    if (drawnPoints.size >= 2) {
                                        // Connect points with a closed polygon if we have at least 3 points
                                        val polygonOptions = PolygonOptions()
                                            .addAll(drawnPoints)
                                            .strokeColor(android.graphics.Color.GREEN)
                                            .strokeWidth(5f)
                                            .fillColor(android.graphics.Color.argb(50, 0, 255, 0))
                                        
                                        // If we have 3+ points, close the polygon
                                        if (drawnPoints.size >= 3) {
                                            // Add the first point again to close the polygon
                                            polygonOptions.add(drawnPoints.first())
                                        }
                                        
                                        drawnPolygon = map.addPolygon(polygonOptions)
                                        
                                        // If we have 3+ points and click near first point, complete the polygon
                                        if (drawnPoints.size >= 3 && 
                                            isNearFirstPoint(latLng, drawnPoints.first())) {
                                            // Complete the polygon
                                            isDrawModeActive = false
                                            DebugLogger.logDebug(TAG, "Analysis mode disabled - analysis is performed by backend")
                                        }
                                    }
                                } else {
                                    // Use existing map click logic
                                    DebugLogger.logClick(TAG, "Map clicked at: ${latLng.latitude}, ${latLng.longitude}")
                                    
                                    // Check loading state first
                                    if (isLoading) {
                                        DebugLogger.logError(TAG, "Map click ignored: still loading data")
                                        errorMessage = "Please wait, still loading data"
                                    } else if (crimeData.isEmpty()) {
                                        // Check if crime data is available
                                        DebugLogger.logError(TAG, "Map click ignored: no crime data available")
                                        errorMessage = "No crime data available. Please check your network connection."
                                    } else {
                                        try {
                                            DebugLogger.logDebug(TAG, "Processing map click. startMarker=${startMarker != null}, destMarker=${destMarker != null}")
                                                
                                                // Check if placing start or destination
                                                if (startMarker == null) {
                                                    DebugLogger.logDebug(TAG, "Placing start marker")
                                                    // Remove previous start marker if it exists
                                                    startMarker?.remove()
                                                    
                                                    // Add new start marker
                                                    startMarker = map.addMarker(
                                                        MarkerOptions()
                                                            .position(latLng)
                                                            .title("Start")
                                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                                            .draggable(true)
                                                    )
                                                    
                                                    DebugLogger.logDebug(TAG, "Start marker placed at ${latLng.latitude}, ${latLng.longitude}, marker=${startMarker != null}")
                                                    debugInfo = "Start marker placed. Click to place destination."
                                                } else if (destMarker == null) {
                                                    DebugLogger.logDebug(TAG, "Placing destination marker")
                                                    // Remove previous destination marker if it exists
                                                    destMarker?.remove()
                                                    
                                                    // Add new destination marker
                                                    destMarker = map.addMarker(
                                                        MarkerOptions()
                                                            .position(latLng)
                                                            .title("Destination")
                                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                                            .draggable(true)
                                                    )
                                                    
                                                    DebugLogger.logDebug(TAG, "Destination marker placed at ${latLng.latitude}, ${latLng.longitude}, marker=${destMarker != null}")
                                                    debugInfo = "Destination marker placed. Calculating route..."
                                                    
                                                    // Calculate route once both markers are set
                                                    val start = startMarker?.position
                                                    val dest = destMarker?.position
                                                    
                                                    if (start != null && dest != null) {
                                                        DebugLogger.logDebug(TAG, "Both markers placed, calculating route from $start to $dest")
                                                        scope.launch {
                                                            try {
                                                                calculateSafestRoute(map, start, dest)
                                                                DebugLogger.logDebug(TAG, "Route calculation completed")
                                                            } catch (e: Exception) {
                                                                DebugLogger.logError(TAG, "Error calculating route", e)
                                                                errorMessage = "Error calculating route: ${e.message}"
                                                            }
                                                        }
                                                    } else {
                                                        DebugLogger.logError(TAG, "Unexpected null marker positions: start=$start, dest=$dest")
                                                        errorMessage = "Error: Could not get marker positions"
                                                    }
                                                } else {
                                                    // If both markers exist, remove them and start over
                                                    DebugLogger.logDebug(TAG, "Clearing existing route")
                                                    clearRoute()
                                                    
                                                    // Place new start marker
                                                    startMarker = map.addMarker(
                                                        MarkerOptions()
                                                            .position(latLng)
                                                            .title("Start")
                                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                                            .draggable(true)
                                                    )
                                                    DebugLogger.logDebug(TAG, "New start marker placed at ${latLng.latitude}, ${latLng.longitude}")
                                                    debugInfo = "Existing route cleared. Start marker placed."
                                                }
                                        } catch (e: Exception) {
                                            DebugLogger.logError(TAG, "Error handling map click", e)
                                            errorMessage = "Error handling map click: ${e.message}"
                                        }
                                    }
                                }
                                }
                            }
                            
                            // Set the click listener
                            map.setOnMapClickListener { latLng -> 
                                mapClickHandler(latLng)
                            }
                            
                            // Handle marker drag events
                            map.setOnMarkerDragListener(object : GoogleMap.OnMarkerDragListener {
                                override fun onMarkerDrag(marker: Marker) {
                                    // Nothing to do during drag
                                }
                                
                                override fun onMarkerDragStart(marker: Marker) {
                                    // Nothing to do at drag start
                                }
                                
                                override fun onMarkerDragEnd(marker: Marker) {
                                    // Recalculate route when marker drag ends
                                    if (startMarker != null && destMarker != null) {
                                        debugInfo = "Recalculating route..."
                                        routeDrawJob?.cancel()
                                        routeDrawJob = scope.launch {
                                            try {
                                                calculateAndDrawRoute(
                                                    map, 
                                                    startMarker!!, 
                                                    destMarker!!, 
                                                    isSafestMode, 
                                                    routePolys
                                                ) { newPolys ->
                                                    routePolys = newPolys
                                                }
                                                debugInfo = "Route recalculated."
                                            } catch (e: Exception) {
                                                errorMessage = "Error recalculating route: ${e.message}"
                                                Log.e(TAG, "Route recalculation failed", e)
                                            }
                                        }
                                    }
                                }
                            })
                            
                            // Modify the camera idle listener to ensure it reinstates the click listener
                            map.setOnCameraIdleListener {
                                // Run the original camera idle logic
                                if (crimeData.isEmpty()) return@setOnCameraIdleListener
                                
                                val zoom = map.cameraPosition.zoom
                                val visibleRegion = map.projection.visibleRegion.latLngBounds
                                
                                if (zoom >= ZOOM_THRESHOLD && !isShowingMarkers) {
                                    // Switch to marker view - cancel any previous loading job
                                    currentJob?.cancel()
                                    
                                    currentJob = scope.launch {
                                        try {
                                            debugInfo = "Zoom level: $zoom - Loading markers..."
                                            isShowingMarkers = true
                                            
                                            // Clear ONLY crime markers, not route markers
                                            // We need to preserve any existing route markers and polylines
                                            val preserveStartMarker = startMarker
                                            val preserveDestMarker = destMarker
                                            val preserveRoutePolys = ArrayList(routePolys)
                                            
                                            map.clear() // Clear all markers
                                            
                                            // Restore route elements if they exist
                                            startMarker = preserveStartMarker
                                            destMarker = preserveDestMarker
                                            routePolys = preserveRoutePolys
                                            
                                            // Filter by date first
                                            val dateFiltered = filterCrimesByDate(crimeData, selectedDateFilter)
                                            
                                            // Then filter by visible region and limit quantity
                                            val visibleCrimes = dateFiltered.filter { crime ->
                                                val pos = LatLng(crime.latitude, crime.longitude)
                                                visibleRegion.contains(pos)
                                            }
                                            
                                            val markersToShow = visibleCrimes.take(MAX_MARKERS)
                                            debugInfo = "Showing ${markersToShow.size} of ${visibleCrimes.size} visible markers (filter: ${selectedDateFilter.name})"
                                            
                                            addMarkers(context, map, markersToShow)
                                        } catch (e: Exception) {
                                            if (e is CancellationException) throw e
                                            Log.e(TAG, "Error loading markers", e)
                                            errorMessage = "Error loading markers: ${e.message}"
                                        }
                                    }
                                } else if (zoom < ZOOM_THRESHOLD && isShowingMarkers) {
                                    // Switch back to heatmap - cancel any marker loading
                                    currentJob?.cancel()
                                    
                                    currentJob = scope.launch {
                                        try {
                                            debugInfo = "Zoom level: $zoom - Showing heatmap"
                                            
                                            // Preserve route markers and polylines
                                            val preserveStartMarker = startMarker
                                            val preserveDestMarker = destMarker
                                            val preserveRoutePolys = ArrayList(routePolys)
                                            
                                            map.clear() // Clear all markers
                                            
                                            // Restore route elements
                                            startMarker = preserveStartMarker
                                            destMarker = preserveDestMarker
                                            routePolys = preserveRoutePolys
                                            
                                            // Add them back to the map if they exist
                                            if (preserveRoutePolys.isNotEmpty()) {
                                                // Re-add polylines
                                                val newPolylines = mutableListOf<Polyline>()
                                                for (oldPoly in preserveRoutePolys) {
                                                    val newPoly = map.addPolyline(
                                                        PolylineOptions()
                                                            .addAll(oldPoly.points)
                                                            .color(oldPoly.color)
                                                            .width(oldPoly.width)
                                                    )
                                                    if (oldPoly.pattern != null) {
                                                        newPoly.pattern = oldPoly.pattern
                                                    }
                                                    newPoly.tag = oldPoly.tag
                                                    newPoly.zIndex = oldPoly.zIndex
                                                    newPolylines.add(newPoly)
                                                }
                                                routePolys = newPolylines
                                            }
                                            
                                            // Re-add markers
                                            if (preserveStartMarker != null) {
                                                startMarker = map.addMarker(
                                                    MarkerOptions()
                                                        .position(preserveStartMarker.position)
                                                        .title("Start")
                                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                                                        .draggable(true)
                                                )
                                            }
                                            
                                            if (preserveDestMarker != null) {
                                                destMarker = map.addMarker(
                                                    MarkerOptions()
                                                        .position(preserveDestMarker.position)
                                                        .title("Destination")
                                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                                        .draggable(true)
                                                )
                                            }
                                            
                                            // Backend tile overlay handles wide-area visualization
                                            isShowingMarkers = false
                                        } catch (e: Exception) {
                                            if (e is CancellationException) throw e
                                            Log.e(TAG, "Error switching to heatmap", e)
                                            errorMessage = "Error switching to heatmap: ${e.message}"
                                        }
                                    }
                                }
                                
                                // Ensure the click listener is reinstated after any map operations
                                map.setOnMapClickListener { latLng -> 
                                    mapClickHandler(latLng)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(IntrinsicSize.Max)
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Loading Crime Data",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = debugInfo,
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "This may take a moment as we fetch data for all London boroughs.",
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Error message
            errorMessage?.let { error ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(Color.White.copy(alpha = 0.8f))
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = error, color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = debugInfo, color = Color.Black)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {
                            errorMessage = null
                            scope.launch {
                                try {
                                    isLoading = true
                                    debugInfo = "Retrying backend initialization..."
                                    // Warm tile URLs
                                    val tiles = com.universityofreading.demo.data.api.BackendCrimeClient.getTiles(zoom = 13)
                                    debugInfo = "Fetched ${tiles.size} tiles"
                                    isLoading = false
                                } catch (e: Exception) {
                                    isLoading = false
                                    errorMessage = "Error contacting backend: ${e.message}"
                                    debugInfo = "Error: ${e.message}"
                                }
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            }
            
            // Debug info (always visible in top corner)
            Text(
                text = debugInfo,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.White.copy(alpha = 0.7f))
                    .padding(4.dp),
                color = Color.Black
            )
            
            // Top right controls: Time controls and risk indicator in a row
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compact time controls
                CompactTimeControls(
                    simulatedHour = simulatedHour,
                    isUsingCurrentTime = isUsingCurrentTime,
                    onTimeClick = { showTimeSimulator = !showTimeSimulator },
                    modifier = Modifier
                        .clickable { showTimeSimulator = !showTimeSimulator }
                        .padding(end = 8.dp)
                )
                
                // Time-based risk indicator
                TimeRiskIndicator(
                    timePeriod = currentTimePeriod,
                    riskLevel = currentRiskLevel,
                    modifier = Modifier
                )
            }
            
            // Time simulation slider (shown when toggle is active)
            if (showTimeSimulator) {
                TimeSimulationSlider(
                    simulatedHour = simulatedHour,
                    onHourChange = { simulatedHour = it },
                    isUsingCurrentTime = isUsingCurrentTime,
                    onUseCurrentTime = { 
                        isUsingCurrentTime = it
                        if (it) simulatedHour = LocalTime.now().hour
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 60.dp, end = 8.dp, start = 8.dp)
                        .width(300.dp)
                )
            }
            
            // Show drawing instructions when in draw mode
            if (isDrawModeActive) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0x88000000))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tap to draw an area. Use the Done button to complete when finished.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            // Add draw mode button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(bottom = 16.dp, start = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Add Done button when in draw mode
                if (isDrawModeActive) {
                    DoneButton(
                        onClick = {
                            if (drawnPoints.size >= 3) {
                                // Complete the polygon
                                isDrawModeActive = false
                                DebugLogger.logDebug(TAG, "Area analysis is performed by backend")
                            } else {
                                // Show message that at least 3 points are needed
                                errorMessage = "Please draw at least 3 points to complete an area"
                            }
                        }
                    )
                }
                
                // Add the draw mode button 
                DrawModeButton(
                    isActive = isDrawModeActive,
                    onClick = {
                        // Toggle draw mode
                        isDrawModeActive = !isDrawModeActive
                        
                        if (!isDrawModeActive) {
                            // Clear any in-progress drawing
                            drawnPoints = emptyList()
                            drawnPolygon?.remove()
                            drawnPolygon = null
                        }
                    }
                )
                
                // Keep existing FABs
                // ... existing buttons ...
            }
        }
    }
    
    // Show bottom sheet for route options
    if (showBottomSheet && startMarker != null && destMarker != null) {
        // Extract route details for display
        val distanceKm = ((routePolys.firstOrNull()?.tag as? Double) ?: 0.0) / 1000.0
        
        // Retrieve risk score from polyline zIndex
        // NOTE: We no longer need to scale by 100 since our risk scale is more appropriate now
        val riskScore = routePolys.firstOrNull()?.zIndex?.toDouble() ?: 0.0
        
        // Count high-risk segments using the same logic as in calculateAndDrawRoute
        val highRiskSegments = routePolys.count { 
            it.color == android.graphics.Color.RED && it.pattern != null 
        }
        
        SafeRouteBottomSheet(
            distanceKm = distanceKm,
            riskScore = riskScore,
            highRiskSegments = highRiskSegments,
            isSafestMode = isSafestMode,
            
            // Handle toggle changes (safety vs. speed preference)
            onSafestModeChanged = { newMode ->
                isSafestMode = newMode
                googleMap?.let { map ->
                    startMarker?.let { start ->
                        destMarker?.let { dest ->
                            // Cancel any ongoing route calculations
                            routeDrawJob?.cancel()
                            
                            // Show loading indicator for the recalculation
                            debugInfo = "Recalculating route with ${if (newMode) "SAFEST" else "FASTEST"} mode..."
                            
                            // Force a complete recalculation from scratch with the new mode
                            routeDrawJob = scope.launch {
                                try {
                                    // Clear existing route lines first
                                    routePolys.forEach { it.remove() }
                                    
                                    // Get positions from current markers
                                    val startPos = start.position
                                    val destPos = dest.position
                                    
                                    // Do a full route recalculation with the new mode
                                    calculateSafestRoute(map, startPos, destPos)
                                } catch (e: Exception) {
                                    DebugLogger.logError(TAG, "Failed to recalculate route with new mode", e)
                                    errorMessage = "Error recalculating route: ${e.message}"
                                }
                            }
                        }
                    }
                }
            },
            
            // Start navigation (zoom to fit the route)
            onStartNavigationClick = {
                routePolys.firstOrNull()?.let { poly ->
                    val bounds = LatLngBounds.builder()
                    poly.points.forEach { bounds.include(it) }
                    googleMap?.animateCamera(
                        CameraUpdateFactory.newLatLngBounds(bounds.build(), 100)
                    )
                }
                showBottomSheet = false
            },
            
            // Dismiss the bottom sheet
            onDismissRequest = {
                showBottomSheet = false
            }
        )
    }
    
    // Show analysis dialog when ready
    if (showAnalysisDialog && currentAnalysis != null) {
        CustomAreaAnalysisDialog(
            analysis = currentAnalysis!!,
            onDismiss = {
                showAnalysisDialog = false
                // Clear drawing
                drawnPoints = emptyList()
                drawnPolygon?.remove()
                drawnPolygon = null
            }
        )
    }
}

// Date filter selection row UI component
@Composable
fun DateFilterRow(
    selectedFilter: ProgressiveDateFilterOption,
    onFilterSelected: (ProgressiveDateFilterOption) -> Unit
) {
    val options = listOf(
        "Recent" to ProgressiveDateFilterOption.MOST_RECENT_MONTH,
        "3 Months" to ProgressiveDateFilterOption.LAST_3_MONTHS,
        "All" to ProgressiveDateFilterOption.ALL_DATA
    )
    
    // Find the selected tab index
    val selectedTabIndex = options.indexOfFirst { it.second == selectedFilter }.coerceAtLeast(0)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Create a compact tab row with options
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier
                .width(280.dp) // Fixed width to save space
                .height(36.dp), // Reduced height
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            indicator = { tabPositions ->
                // Only show the indicator if a valid tab is selected
                if (selectedTabIndex >= 0 && selectedTabIndex < tabPositions.size) {
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTabIndex])
                            .height(2.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }
        ) {
            options.forEachIndexed { index, (text, option) ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { onFilterSelected(option) },
                    text = {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    modifier = Modifier.height(36.dp) // Match TabRow height
                )
            }
        }
    }
}

// Filter crime data based on selected date range 
private fun filterCrimesByDate(
    list: List<CrimeData>,
    option: ProgressiveDateFilterOption
): List<CrimeData> {
    // For empty list or when showing all dates, return as-is
    if (list.isEmpty() || option == ProgressiveDateFilterOption.ALL_DATA) return list
    
    // Get all parseable dates with their crimes
    val dateMap = list.mapNotNull { crime ->
        val date = parseCrimeDate(crime.date)
        if (date != null) {
            Pair(crime, date)
        } else {
            Log.w("ProgressiveMapScreen", "Unable to parse date: ${crime.date}")
            null
        }
    }
    
    // If no dates could be parsed, return the original list
    if (dateMap.isEmpty()) return list
    
    // Log date statistics for debugging
    logDateDistribution(dateMap.map { it.second })
    
    // Group crimes by year-month for easier filtering
    val crimesByYearMonth = dateMap.groupBy { 
        YearMonth.from(it.second)
    }
    
    // Sort year-months in descending order (most recent first)
    val sortedYearMonths = crimesByYearMonth.keys.sortedDescending()
    if (sortedYearMonths.isEmpty()) return list
    
    // Get the most recent year-month
    val mostRecentYearMonth = sortedYearMonths.first()
    
    // Apply filtering based on option
    return when (option) {
        ProgressiveDateFilterOption.MOST_RECENT_MONTH -> {
            // Only crimes from the most recent month
            crimesByYearMonth[mostRecentYearMonth]?.map { it.first } ?: emptyList()
        }
        ProgressiveDateFilterOption.LAST_3_MONTHS -> {
            // Get up to 3 most recent months
            val recentMonths = sortedYearMonths.take(3)
            recentMonths.flatMap { yearMonth ->
                crimesByYearMonth[yearMonth]?.map { it.first } ?: emptyList()
            }
        }
        ProgressiveDateFilterOption.ALL_DATA -> list
    }
}

// Parse date string safely, handling API's format "YYYY-MM-01" and possibly others
private fun parseCrimeDate(dateStr: String): LocalDate? {
    return try {
        // First try standard ISO format (YYYY-MM-DD)
        LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
    } catch (e: DateTimeParseException) {
        try {
            // If that fails, try just the year and month part (YYYY-MM)
            // The API data might have just year-month
            if (dateStr.length >= 7) {
                val yearMonth = dateStr.substring(0, 7)
                LocalDate.parse("$yearMonth-01", DateTimeFormatter.ISO_LOCAL_DATE)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    } catch (e: Exception) {
        null
    }
}

// Add this new function for debugging date distribution
private fun logDateDistribution(dates: List<LocalDate>) {
    if (dates.isEmpty()) {
        Log.d("ProgressiveMapScreen", "No valid dates found")
        return
    }
    
    val dateDistribution = dates.groupingBy { it }.eachCount().toSortedMap()
    val uniqueDateCount = dateDistribution.size
    val earliestDate = dateDistribution.firstKey()
    val latestDate = dateDistribution.lastKey()
    
    // Log overall stats
    Log.d("ProgressiveMapScreen", "Date statistics:")
    Log.d("ProgressiveMapScreen", "- Unique dates: $uniqueDateCount")
    Log.d("ProgressiveMapScreen", "- Date range: $earliestDate to $latestDate")
    
    // Log top 5 most common dates
    val mostCommonDates = dateDistribution.entries.sortedByDescending { it.value }.take(5)
    Log.d("ProgressiveMapScreen", "- Most common dates:")
    mostCommonDates.forEach { (date, count) ->
        Log.d("ProgressiveMapScreen", "  - $date: $count crimes")
    }
}

// Add markers to the map - limited to specific data points only
private fun addMarkers(context: Context, map: GoogleMap, crimeData: List<CrimeData>) {
    if (crimeData.isEmpty()) {
        Log.d("ProgressiveMapScreen", "No crime data points to display as markers")
        return
    }
    
    // Cache for marker icons (to avoid recreating the same emoji bitmaps multiple times)
    val iconCache = mutableMapOf<String, BitmapDescriptor>()
    
    // Create a question mark emoji bitmap for the default case
    val defaultIcon = createBitmapFromText(context, "❓")
    
    try {
        // Add each crime as a marker
        for (crime in crimeData) {
            // Get emoji for crime type
            val emoji = crime.getEmoji()
            
            // Get or create marker icon - always use our custom bitmap icons, never the default Android one
            val markerIcon = iconCache.getOrPut(emoji) {
                // Not in cache, create new bitmap
                createBitmapFromText(context, emoji)
            }
            
            Log.d(TAG, "Adding marker for crime type: ${crime.type}, emoji: $emoji")
            
            val marker = map.addMarker(
                MarkerOptions()
                    .position(LatLng(crime.latitude, crime.longitude))
                    .title(crime.type)
                    .snippet("Date: ${crime.date}")
                    .icon(markerIcon)
                    .alpha(0.9f)
            )
            
            // Attach the crime data to the marker for use in the info window
            marker?.tag = crime
        }
        
        Log.d("ProgressiveMapScreen", "Added ${crimeData.size} markers to map with emoji icons")
    } catch (e: Exception) {
        Log.e("ProgressiveMapScreen", "Error adding markers", e)
        throw e
    }
}

/**
 * Creates a bitmap from emoji text for use as marker icon
 */
private fun createBitmapFromText(context: Context, text: String): BitmapDescriptor {
    // Create a TextView with centered text and background
    val textView = TextView(context).apply {
        this.text = text
        this.textSize = 40f // Larger text size
        this.setPadding(8, 8, 8, 8) // Add padding
        this.gravity = android.view.Gravity.CENTER // Center the text
        this.setBackgroundResource(R.drawable.rounded_background) // Use a background drawable
        
        // Set layout parameters and measure
        val widthSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        measure(widthSpec, heightSpec)
        
        // Ensure minimum size
        val minSize = 96
        val width = Math.max(measuredWidth, minSize)
        val height = Math.max(measuredHeight, minSize)
        
        layout(0, 0, width, height)
    }
    
    // Create bitmap with proper size
    val bitmap = Bitmap.createBitmap(
        textView.width, 
        textView.height, 
        Bitmap.Config.ARGB_8888
    )
    
    // Draw the view on canvas
    val canvas = Canvas(bitmap)
    textView.draw(canvas)
    
    Log.d(TAG, "Created emoji marker bitmap for emoji: $text, size: ${bitmap.width}x${bitmap.height}")
    
    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

// Configure the popup info window for markers
private fun setupInfoWindowAdapter(context: Context, map: GoogleMap) {
    map.setInfoWindowAdapter(object : GoogleMap.InfoWindowAdapter {
        override fun getInfoWindow(marker: Marker): View? = null

        override fun getInfoContents(marker: Marker): View? {
            try {
                // Get the crime data attached to the marker
                val crime = marker.tag as? CrimeData
                if (crime != null) {
                    val view = LayoutInflater.from(context).inflate(R.layout.marker_info_window, null)
                    
                    // Get emoji for crime type
                    val emoji = crime.getEmoji()
                    
                    // Set emoji text instead of image resource
                    val iconTextView = TextView(context).apply {
                        text = emoji
                        textSize = 24f
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                        gravity = android.view.Gravity.CENTER
                        
                        // Apply styling similar to the marker
                        setBackgroundResource(R.drawable.rounded_background)
                        setPadding(8, 8, 8, 8)
                        
                        // Set layout width and height
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        )
                    }
                    
                    // Replace the ImageView with our TextView
                    val iconContainer = view.findViewById<ImageView>(R.id.info_window_icon)
                    (iconContainer.parent as? ViewGroup)?.apply {
                        val index = indexOfChild(iconContainer)
                        removeView(iconContainer)
                        addView(iconTextView, index)
                    }
                    
                    val text = buildString {
                        append("Type: ${crime.type}\n")
                        
                        // Split the date string to check for time information
                        val dateStr = crime.date
                        if (dateStr.contains("T")) {
                            // If date contains T, it likely has time information (ISO format)
                            val parts = dateStr.split("T")
                            append("Date: ${parts[0]}\n")
                            if (parts.size > 1) {
                                append("Time: ${parts[1].substringBefore("Z")}\n")
                            }
                        } else {
                            // No time information, just show the date
                            append("Date: $dateStr\n")
                        }
                        
                        append("Severity: ${crime.severity}\n")
                        append("Region: ${crime.region}")
                    }
                    
                    view.findViewById<TextView>(R.id.info_window_text).text = text
                    return view
                }
                
                // Default case - just show title and snippet
                val view = LayoutInflater.from(context).inflate(R.layout.marker_info_window, null)
                
                // Create default emoji icon for generic markers
                val iconTextView = TextView(context).apply {
                    text = "❓"
                    textSize = 24f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                    gravity = android.view.Gravity.CENTER
                    setBackgroundResource(R.drawable.rounded_background)
                    setPadding(8, 8, 8, 8)
                }
                
                // Replace the ImageView with our TextView
                val iconContainer = view.findViewById<ImageView>(R.id.info_window_icon)
                (iconContainer.parent as? ViewGroup)?.apply {
                    val index = indexOfChild(iconContainer)
                    removeView(iconContainer)
                    addView(iconTextView, index)
                }
                
                val text = "${marker.title ?: "Unknown"}\n${marker.snippet ?: ""}"
                view.findViewById<TextView>(R.id.info_window_text).text = text
                return view
            } catch (e: Exception) {
                Log.e("ProgressiveMapScreen", "Error creating info window", e)
                // Return a simple fallback view in case of error
                val view = LayoutInflater.from(context).inflate(R.layout.marker_info_window, null)
                view.findViewById<TextView>(R.id.info_window_text).text = "Error displaying details"
                return view
            }
        }
    })
}

// Display heatmap of crime data
private fun addHeatMap(map: GoogleMap, crimeData: List<CrimeData>) {
    if (crimeData.isEmpty()) {
        Log.e("ProgressiveMapScreen", "Cannot create heatmap with empty data")
        return
    }
    
    // Create weighted data points for the heatmap
    val weighted = crimeData.map {
        WeightedLatLng(LatLng(it.latitude, it.longitude), it.severity + 1)
    }
    
    // Create a gradient from green (low) to yellow to red (high)
    val colors = intArrayOf(
        android.graphics.Color.rgb(81, 255, 0),   // green
        android.graphics.Color.rgb(255, 165, 0),  // orange
        android.graphics.Color.rgb(255, 0, 0)     // red
    )
    
    try {
        val provider = HeatmapTileProvider.Builder()
            .weightedData(weighted)
            .gradient(Gradient(colors, floatArrayOf(0f, 0.5f, 1f)))
            .radius(50)
            .build()
            
        map.addTileOverlay(TileOverlayOptions().tileProvider(provider))
        Log.d("ProgressiveMapScreen", "Heatmap added with ${weighted.size} points")
    } catch (e: Exception) {
        Log.e("ProgressiveMapScreen", "Error creating heatmap provider", e)
        throw e
    }
}

/**
 * Calculates and draws the safest route between two markers
 */
private suspend fun calculateAndDrawRoute(
    map: GoogleMap,
    start: Marker,
    dest: Marker,
    isSafestMode: Boolean,
    oldPolys: List<Polyline>,
    update: (List<Polyline>) -> Unit
) {
    // Clear existing routes
    oldPolys.forEach { it.remove() }
    
    try {
        // Calculate the safest route and alternatives
        DebugLogger.logDebug(TAG, "Calling SafeRoutePlanner.safestRoute in ${if (isSafestMode) "SAFEST" else "FASTEST"} mode")
        val (bestRoute, alternativeRoutes) = SafeRoutePlanner.safestRoute(
            start.position, 
            dest.position, 
            isSafestMode
        )
        
        DebugLogger.logDebug(TAG, "Route calculation successful. Best route distance: ${bestRoute.distanceM}m, risk: ${bestRoute.riskScore}")
        
        val newPolylines = mutableListOf<Polyline>()
        
        // ENHANCED VISUAL DISTINCTION between route types
        val routeColor = if (isSafestMode) 
            android.graphics.Color.rgb(0, 160, 0) // Darker green for safest
        else 
            android.graphics.Color.rgb(0, 0, 220) // Blue for fastest

        // Set different line patterns based on route type
        val routePattern = if (isSafestMode) {
            // Safest route gets a solid line
            null
        } else {
            // Fastest route gets a slightly dashed pattern to indicate speed
            listOf(Gap(5f), Dot(), Gap(5f))
        }
            
        // Draw the main route polyline
        val routePoints = PolylineUtils.decode(bestRoute.route.overviewPolyline.encodedPath)
        val primaryRoute = map.addPolyline(
            PolylineOptions()
                .addAll(routePoints)
                .color(routeColor)
                .width(12f)
                .pattern(routePattern)
        )
        primaryRoute.tag = bestRoute.distanceM
        primaryRoute.zIndex = bestRoute.riskScore.toFloat()
        newPolylines.add(primaryRoute)
        
        // Add visual indicators about the route for better user understanding
        if (isSafestMode) {
            // For safest route: Find and highlight high-risk segments
            if (routePoints.size >= 10) {
                try {
                    // Use a reasonable sample size (not too many points)
                    val sampleDistance = 50.0 // 50 meters between points
                    val sampledPoints = PolylineUtils.sampleEvery(routePoints, sampleDistance)
                    
                    // Limit to a reasonable number of samples for performance
                    val limitedSamples = if (sampledPoints.size > 20) sampledPoints.take(20) else sampledPoints
                    
                    // Use backend segment risk data instead of on-device spatial index
                    // The route segments are already scored by backend; extract risk from segment data
                    val highRiskPointPairs = mutableListOf<Pair<LatLng, LatLng>>()
                    
                    // Iterate through consecutive points and mark segments with high risk from backend data
                    sampledPoints.zipWithNext { point1, point2 ->
                        // Check if this segment is high-risk based on backend segment scores
                        // This requires integrating with bestRoute.segments risk data
                        // For now, just use the overall route risk as a placeholder
                        if (bestRoute.riskScore > SafeRoutePlanner.HIGH_RISK_THRESHOLD) {
                            highRiskPointPairs.add(point1 to point2)
                        }
                    }
                    
                    // Draw high-risk segments
                    if (highRiskPointPairs.isNotEmpty()) {
                        DebugLogger.logDebug(TAG, "Highlighting ${highRiskPointPairs.size} potentially risky segments")
                        
                        // Draw each high-risk segment with a distinctive pattern
                        highRiskPointPairs.forEach { (start, end) ->
                            val highRiskSegment = map.addPolyline(
                                PolylineOptions()
                                    .add(start, end)
                                    .color(android.graphics.Color.RED)
                                    .width(12f)
                                    .pattern(listOf(Gap(10f), Dot(), Gap(10f)))
                                    .zIndex(15f) // Higher than the main route
                            )
                            newPolylines.add(highRiskSegment)
                        }
                    }
                } catch (e: Exception) {
                    DebugLogger.logError(TAG, "Failed to analyze route segments: ${e.message}", e)
                }
            }
        } else {
            // For fastest route: Skip additional markers and focus on route visibility
            DebugLogger.logDebug(TAG, "Rendering fastest route")
        }
        
        // Add alternative routes as dashed gray lines
        DebugLogger.logDebug(TAG, "Adding ${alternativeRoutes.size} alternative routes")
        alternativeRoutes.forEach { route ->
            val altPolyline = map.addPolyline(
                PolylineOptions()
                    .addAll(PolylineUtils.decode(route.route.overviewPolyline.encodedPath))
                    .color(android.graphics.Color.GRAY)
                    .pattern(listOf(Dot(), Gap(20f)))
                    .width(8f)
            )
            altPolyline.tag = route.distanceM
            altPolyline.zIndex = route.riskScore.toFloat()
            newPolylines.add(altPolyline)
        }
        
        // Update the polylines list
        DebugLogger.logDebug(TAG, "Updating UI with ${newPolylines.size} polylines")
        update(newPolylines)
    } catch (e: Exception) {
        DebugLogger.logError(TAG, "Failed to calculate route", e)
        // Return empty list rather than throwing - this prevents crashes
        update(emptyList())
    }
}

/**
 * Check if a point is within a certain distance of the first point to complete polygon
 */
private fun isNearFirstPoint(point: LatLng, firstPoint: LatLng): Boolean {
    val distance = calculateDistance(point, firstPoint)
    return distance < 50.0 // Within 50 meters
}

/**
 * Calculate distance between two points using the Haversine formula
 */
private fun calculateDistance(point1: LatLng, point2: LatLng): Double {
    val R = 6371e3 // Earth radius in meters
    val lat1 = Math.toRadians(point1.latitude)
    val lat2 = Math.toRadians(point2.latitude)
    val deltaLat = Math.toRadians(point2.latitude - point1.latitude)
    val deltaLng = Math.toRadians(point2.longitude - point1.longitude)
    
    val a = Math.sin(deltaLat/2) * Math.sin(deltaLat/2) +
            Math.cos(lat1) * Math.cos(lat2) *
            Math.sin(deltaLng/2) * Math.sin(deltaLng/2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a))
    
    return R * c // Distance in meters
} 