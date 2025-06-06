package com.universityofreading.demo.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.universityofreading.demo.R
import java.io.InputStreamReader

data class RegionList(val regions: List<String>)

fun loadRegions(context: Context): List<String> {
    return try {
        val inputStream = context.resources.openRawResource(R.raw.regions)
        val reader = InputStreamReader(inputStream)

        val regionList: RegionList = Gson().fromJson(reader, object : TypeToken<RegionList>() {}.type)
        reader.close()
        inputStream.close()

        regionList.regions
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
