package com.universityofreading.demo.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.universityofreading.demo.R
import com.universityofreading.demo.data.CrimeData
import java.io.IOException

fun loadCrimeData(context: Context): List<CrimeData> {
    return try {
        val inputStream = context.resources.openRawResource(R.raw.crime_data_updated)
        val size = inputStream.available()
        val buffer = ByteArray(size)
        inputStream.read(buffer)
        inputStream.close()

        val jsonString = String(buffer, Charsets.UTF_8)
        val gson = Gson()
        val listType = object : TypeToken<List<CrimeData>>() {}.type
        val crimeList: List<CrimeData> = gson.fromJson(jsonString, listType)

        // This step adds region info based on your custom getRegionFromCoordinates
        crimeList.map { crime ->
            CrimeData(
                latitude = crime.latitude,
                longitude = crime.longitude,
                severity = crime.severity,
                date = crime.date,
                type = crime.type,
                region = getRegionFromCoordinates(crime.latitude, crime.longitude)
            )
        }
    } catch (e: IOException) {
        e.printStackTrace()
        emptyList()
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

private fun getRegionFromCoordinates(latitude: Double, longitude: Double): String {
    val boroughs = mapOf(
        "Westminster" to Pair(51.5074, -0.1278),
        "Camden" to Pair(51.5390, -0.1425),
        "Islington" to Pair(51.5465, -0.1058),
        "Hackney" to Pair(51.5450, -0.0554),
        "Tower Hamlets" to Pair(51.5096, -0.0177),
        "Greenwich" to Pair(51.4826, 0.0077),
        "Lewisham" to Pair(51.4526, -0.0154),
        "Southwark" to Pair(51.5055, -0.0907),
        "Lambeth" to Pair(51.4900, -0.1221),
        "Wandsworth" to Pair(51.4567, -0.1910),
        "Hammersmith and Fulham" to Pair(51.4927, -0.2339),
        "Kensington and Chelsea" to Pair(51.5000, -0.1919),
        "Brent" to Pair(51.5588, -0.2817),
        "Ealing" to Pair(51.5130, -0.3089),
        "Hounslow" to Pair(51.4746, -0.3680),
        "Richmond upon Thames" to Pair(51.4479, -0.3260),
        "Kingston upon Thames" to Pair(51.4085, -0.2861),
        "Merton" to Pair(51.4097, -0.1978),
        "Sutton" to Pair(51.3618, -0.1945),
        "Croydon" to Pair(51.3762, -0.0982),
        "Bromley" to Pair(51.4039, 0.0198),
        "Barnet" to Pair(51.6252, -0.1517),
        "Harrow" to Pair(51.5898, -0.3346),
        "Hillingdon" to Pair(51.5441, -0.4760),
        "Enfield" to Pair(51.6521, -0.0807),
        "Waltham Forest" to Pair(51.5908, -0.0134),
        "Redbridge" to Pair(51.5590, 0.0741),
        "Havering" to Pair(51.5812, 0.1837),
        "Barking and Dagenham" to Pair(51.5462, 0.1313),
        "Newham" to Pair(51.5076, 0.0343),
        "Bexley" to Pair(51.4549, 0.1505),
        "Haringey" to Pair(51.5906, -0.1110)
    )

    return boroughs.minByOrNull { (_, coords) ->
        val latDiff = coords.first - latitude
        val lonDiff = coords.second - longitude
        (latDiff * latDiff) + (lonDiff * lonDiff)
    }?.key ?: "Unknown"
}
