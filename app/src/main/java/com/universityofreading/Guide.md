Below is a step-by-step guide on refining your existing heatmap in MapScreen.kt so that it effectively highlights both “high-crime” and “relatively safe” (low-crime) areas in a traffic-light fashion. The key changes are:

Define a custom crime index for weighting each location.
Apply a custom color gradient (green→yellow→red).
Optionally tweak radius, opacity, or other settings for better visualization.
You already have the foundation in MapScreen.kt with addHeatMap(...), which uses HeatmapTileProvider. We’ll build on that.

1) Create a Crime Weight Function (Optional but Recommended)
Right now, you use crime.severity directly as the weight. That’s a start, but maybe you want to incorporate both frequency and severity, or just modify severity to ensure a better distribution. You can keep using crime.severity if that’s enough for your project. If you want a refined calculation, create something like:

kotlin
Copy
Edit
private fun computeCrimeWeight(crime: CrimeData): Double {
    // Example: Weighted by severity + a base factor so every crime counts
    // Tweak or expand to incorporate date recency, type weighting, etc.
    return crime.severity + 1.0
}
If you only care about severity, just use crime.severity. If you want to factor in recency, type of crime, etc., expand this function accordingly.

2) Modify addHeatMap(...) to Use This Weight
In MapScreen.kt, you already have:

kotlin
Copy
Edit
private fun addHeatMap(googleMap: GoogleMap, context: Context) {
    val crimeDataList = loadCrimeDataFromRaw(context)
    val weightedData = crimeDataList.map { crime ->
        WeightedLatLng(
            LatLng(crime.latitude, crime.longitude),
            crime.severity // <-- This is currently your weight
        )
    }

    // Build HeatmapTileProvider
    val heatmapProvider = HeatmapTileProvider.Builder()
        .weightedData(weightedData)
        .radius(50)
        .build()

    googleMap.addTileOverlay(TileOverlayOptions().tileProvider(heatmapProvider))
}
Replace crime.severity with either computeCrimeWeight(crime) or whichever formula you decide:

kotlin
Copy
Edit
val weightedData = crimeDataList.map { crime ->
    WeightedLatLng(
        LatLng(crime.latitude, crime.longitude),
        computeCrimeWeight(crime)  // or just crime.severity if you prefer
    )
}
3) Add a Custom Traffic-Light Gradient
The built-in heatmap defaults to a green-red scale, but you can explicitly define the gradient so it’s clear which areas are “cool” (safe) vs. “hot” (high-crime). For example:

kotlin
Copy
Edit
// 1) Define colors from green to yellow to red
val colors = intArrayOf(
    android.graphics.Color.rgb(0, 255, 0),   // Green
    android.graphics.Color.rgb(255, 255, 0), // Yellow
    android.graphics.Color.rgb(255, 0, 0)    // Red
)

// 2) Positions in [0..1] where each color is used
// 0.0 = green, 0.5 = yellow, 1.0 = red
val startPoints = floatArrayOf(0.0f, 0.5f, 1.0f)

// 3) Build a Gradient object
val gradient = com.google.maps.android.heatmaps.Gradient(colors, startPoints)
Then apply it to your HeatmapTileProvider:

kotlin
Copy
Edit
val heatmapProvider = HeatmapTileProvider.Builder()
    .weightedData(weightedData)
    .gradient(gradient)      // <--- Add this line
    .radius(50)
    .build()
If you want a smoother or different distribution (like 0.3 for yellow, 0.8 for red, etc.), tweak startPoints.

4) Adjust the Radius & Opacity as Needed
Radius (radius(…)) defines how wide the heat influence extends around each crime point in pixels. A larger radius can create broader “hot zones;” a smaller radius yields more detailed or “spiky” areas.
Opacity (opacity(…)) is also available on HeatmapTileProvider.Builder(). If you want your safe (green) zones to be more faint, you could reduce overall opacity. Example:
kotlin
Copy
Edit
.heatmapProvider.setOpacity(0.7)
This is optional, depending on how dense your crime data is and how visible you want the heatmap to be over your map tiles.

5) Keep Your Existing Zoom Toggle Logic
You’re already switching between:

Heatmap (lower zoom)
Detailed markers (higher zoom)
That logic is in googleMap.setOnCameraIdleListener { ... }. You don’t need to remove or break any of that. The only difference is that your heatmap will now highlight low-crime areas in green rather than just relying on the default color ramp.

6) Validate & Tweak
Check the distribution of your final weights. If everything is bunched at the high end, you might see mostly red, so you could reduce the factor or apply a log transform to spread the values out.
Visually confirm that known “safer” areas appear more green or faint on the heatmap, and “hot spots” appear red.
If you need explicit “ranking” (like top 25% vs. bottom 25%), you can create a more manual approach—e.g. set weights or color breaks accordingly. But typically, the gradient approach is enough.
Final Example Code Snippet (Putting It All Together)
Below is a condensed sample from your MapScreen.kt’s addHeatMap(...):

kotlin
Copy
Edit
private fun addHeatMap(googleMap: GoogleMap, context: Context) {
    val crimeDataList = loadCrimeDataFromRaw(context)

    // STEP 1: map crime data to WeightedLatLng with your custom or simple weight
    val weightedData = crimeDataList.map { crime ->
        WeightedLatLng(
            LatLng(crime.latitude, crime.longitude),
            computeCrimeWeight(crime) // or crime.severity
        )
    }

    // STEP 2: define traffic-light gradient
    val colors = intArrayOf(
        Color.rgb(0, 255, 0),    // Green
        Color.rgb(255, 255, 0),  // Yellow
        Color.rgb(255, 0, 0)     // Red
    )
    val startPoints = floatArrayOf(0.0f, 0.5f, 1.0f)
    val gradient = Gradient(colors, startPoints)

    // STEP 3: build heatmap provider with custom gradient & optional radius or opacity
    val heatmapProvider = HeatmapTileProvider.Builder()
        .weightedData(weightedData)
        .gradient(gradient)
        .radius(50)         // adjust for desired spread
        .build()

    // STEP 4: add as tile overlay
    googleMap.addTileOverlay(TileOverlayOptions().tileProvider(heatmapProvider))
}
That’s it! Now, areas with minimal or no crime points become green (or effectively transparent/faint if the underlying weighting is near zero), while heavy or severe crime clusters show in yellow-to-red. This satisfies both the “no go zones” (red) and “relatively safe areas” (green) in one cohesive map layer—exactly matching your traffic-light shading specification.







