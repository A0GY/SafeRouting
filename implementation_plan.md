# Urban Safety Companion Implementation Plan

## Overview
This document outlines the implementation plan for enhancing the Urban Safety Companion app, focusing on high-priority missing features while considering technical constraints and available resources.

## 1. Time-Based Features Implementation

### Priority: HIGH
Time-based features are essential for a safety app, as safety concerns vary greatly throughout the day.

### Implementation Approach:
1. **Time-Based Risk Adjustment**
   ```kotlin
   fun getTimeBasedRiskMultiplier(): Double {
       val currentHour = LocalTime.now().hour
       return when(currentHour) {
           in 0..5 -> 1.8    // Very late night/early morning (highest risk)
           in 6..8 -> 1.0    // Morning commute (baseline risk)
           in 9..16 -> 0.7   // Daytime (lowest risk)
           in 17..19 -> 1.1  // Evening commute (slightly elevated)
           in 20..23 -> 1.5  // Evening/night (high risk)
           else -> 1.0       // Fallback
       }
   }
   ```

2. **UI Time Indicator**
   - Add a chip/badge in the top right corner showing:
     - Current time period (e.g., "Evening", "Night", "Day")
     - Associated risk level icon
   - Color code the indicator based on general risk level for that time

3. **Time Simulation Feature**
   - Add a simple time slider allowing users to simulate different times
   - Update routes and risk assessments based on selected time
   - Include a "Use Current Time" button to reset

## 2. Enhanced Visualization

### Priority: MEDIUM-HIGH
Better visual representation will greatly improve the "at-a-glance" aspect of the app.

### Implementation Approach:
1. **Use System Emoji/Icons**
   - Map crime types to system emoji/Unicode characters:
     ```kotlin
     val crimeTypeIcons = mapOf(
         "violent-crime" to "👊",       // Fist
         "robbery" to "💰",             // Money bag
         "burglary" to "🏠",            // House
         "vehicle-crime" to "🚗",       // Car
         "bicycle-theft" to "🚲",       // Bicycle
         "shoplifting" to "🛒",         // Shopping cart
         "drugs" to "💊",               // Pill
         "anti-social-behaviour" to "🔊" // Loud speaker
     )
     ```
   - Replace generic markers with these icons when in detailed view

2. **Risk Level Visual Cues**
   - Enhance the current color scheme with additional visual cues:
     - Low risk: Green + shield icon
     - Medium risk: Yellow + caution icon
     - High risk: Red + warning icon
   
3. **Route Visualization Enhancements**
   - Add color gradients to routes to show varying risk levels along the route
   - Highlight particularly risky segments with additional markers or patterns
   - Add directional arrows to indicate walking direction

## 3. Student-Specific Features

### Priority: MEDIUM (Optional based on constraints)
While valuable, these features require substantial additional data gathering that may not be feasible for all covered universities.

### Implementation Approach:
1. **Limited Initial Scope**
   - Start with just one university (e.g., University of Reading) as a proof of concept
   - Manually compile key locations:
     - Main campus buildings
     - Popular student accommodation 
     - Common student venues

2. **Simple POI Layer**
   - Add a toggle to show/hide university-specific POIs
   - Use standard icons for university buildings, housing, etc.
   - Include basic safety information for these locations

3. **Future Scalability Strategy**
   - Document the process for adding new universities
   - Create a data format for easy addition of new university POIs
   - Consider a community contribution model for scaling

## 4. Additional Priority Enhancements

### 4.1 Offline Support (Priority: HIGH)
```kotlin
// Implement local storage of recently viewed areas
class SafetyDataCache(context: Context) {
    private val db = Room.databaseBuilder(context, CrimeDatabase::class.java, "crime_data").build()
    
    suspend fun cacheAreaData(latitude: Double, longitude: Double, radiusKm: Double, data: List<CrimeData>) {
        db.crimeDao().insertAll(data.map { it.toEntity() })
        db.metadataDao().insert(CacheMetadata(latitude, longitude, radiusKm, System.currentTimeMillis()))
    }
    
    suspend fun getOfflineData(latitude: Double, longitude: Double): List<CrimeData>? {
        // Check if we have cached data near this location
        // Return null if not available or too old
    }
}
```

### 4.2 User Tips & Safety Information (Priority: MEDIUM)
- Add a "Safety Tips" section with general urban safety advice
- Include university-specific safety resources where available
- Provide emergency contact information for local police

### 4.3 Accessibility Improvements (Priority: MEDIUM-HIGH)
- Add high-contrast mode for visually impaired users
- Ensure icons have text alternatives
- Add haptic feedback for important safety alerts

## Implementation Timeline

### Phase 1 (2-3 weeks)
- Implement time-based risk adjustments
- Add time period indicator to the UI
- Implement system emoji for crime types

### Phase 2 (2-3 weeks)
- Develop enhanced route visualization
- Add offline support for core areas
- Implement accessibility improvements

### Phase 3 (Optional, 3-4 weeks)
- Implement University of Reading POI data as pilot
- Add safety tips and information section
- Develop time simulation feature

## Technical Constraints & Considerations

### Data Limitations
- Police API provides limited historical data and no real-time updates
- Crime reports typically have a reporting delay of days or weeks
- Time-of-day for crimes is often not available, requiring statistical modeling

### Performance Considerations
- Offline mode will require careful storage management to avoid excessive space usage
- Complex visualizations may impact performance on older devices
- Battery usage concerns with continuous location tracking

### Workarounds for Missing Data
- For missing crime times, use statistical distributions based on crime type
- For university locations, use a combination of OpenStreetMap tags and manual data
- Consider using general crime patterns by time when specific data isn't available

## Evaluation Metrics

To measure the success of these implementations:
1. **Usability**: Can users quickly understand safety information at a glance?
2. **Accuracy**: Do the time-based risk adjustments reflect realistic safety concerns?
3. **Performance**: Does the app maintain responsiveness with new features?
4. **Value**: Do student users find the university-specific features helpful?

## Future Considerations (Beyond Current Scope)

While outside our immediate implementation plan, these features could be considered for future versions:
- Predictive risk modeling using machine learning
- User-contributed safety reports with moderation
- Integration with university security services
- AR (Augmented Reality) visualization of safety information
- Smartwatch companion app for discreet safety checking 