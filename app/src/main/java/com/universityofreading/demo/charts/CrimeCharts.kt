package com.universityofreading.demo.charts

import android.content.Context
import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter

@Composable
fun CrimeTypesPieChart(context: Context, entries: List<PieEntry>) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            PieChart(it).apply {
                description.isEnabled = false
                isDrawHoleEnabled = true
                setUsePercentValues(true)
                setEntryLabelTextSize(12f)
                setEntryLabelColor(Color.BLACK)

                legend.apply {
                    textSize = 12f
                    isEnabled = true
                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                    orientation = Legend.LegendOrientation.VERTICAL
                    setDrawInside(false)
                    yOffset = 10f
                    xOffset = 10f
                    yEntrySpace = 5f
                    textColor = Color.BLACK
                }

                setDrawCenterText(true)
                centerText = "Crime Types"
                setUsePercentValues(true)
                setDrawEntryLabels(false)
                setExtraOffsets(20f, 20f, 60f, 20f)
            }
        },
        update = { chart ->
            val dataSet = PieDataSet(entries, "Crime Types").apply {
                colors = listOf(
                    Color.rgb(255, 99, 71),
                    Color.rgb(106, 90, 205),
                    Color.rgb(60, 179, 113),
                    Color.rgb(238, 130, 238),
                    Color.rgb(255, 165, 0),
                    Color.rgb(30, 144, 255),
                    Color.rgb(255, 215, 0)
                )
                sliceSpace = 3f
                valueTextSize = 12f
                valueTextColor = Color.BLACK
                valueFormatter = PercentFormatter(chart)
                valueLinePart1Length = 0.4f
                valueLinePart2Length = 0.4f
                yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE
            }

            chart.data = PieData(dataSet)
            chart.invalidate()
        }
    )
}

@Composable
fun CrimeSeverityBarChart(context: Context, entries: List<BarEntry>, labels: List<String>) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            BarChart(it).apply {
                description.isEnabled = false
                setDrawGridBackground(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    valueFormatter = IndexAxisValueFormatter(labels)
                    textSize = 12f
                    setDrawGridLines(false)
                }

                axisLeft.apply {
                    axisMinimum = 0f
                    textSize = 12f
                    setDrawGridLines(true)
                }
                axisRight.isEnabled = false

                legend.apply {
                    textSize = 12f
                    isEnabled = true
                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                    orientation = Legend.LegendOrientation.VERTICAL
                    setDrawInside(false)
                    yOffset = 10f
                    xOffset = 10f
                }

                setPinchZoom(false)
                setScaleEnabled(false)
                setExtraOffsets(10f, 20f, 30f, 20f)
            }
        },
        update = { chart ->
            val dataSet = BarDataSet(entries, "Number of Crimes").apply {
                colors = listOf(
                    Color.rgb(255, 99, 71),
                    Color.rgb(255, 165, 0),
                    Color.rgb(60, 179, 113)
                )
                valueTextSize = 12f
                valueTextColor = Color.BLACK
            }

            val barData = BarData(dataSet)
            barData.barWidth = 0.6f

            chart.data = barData
            chart.animateY(1000)
            chart.invalidate()
        }
    )
}

@Composable
fun CrimeSeverityLineChart(context: Context, entries: List<Entry>, labels: List<String>) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = {
            LineChart(it).apply {
                description.isEnabled = false
                setDrawGridBackground(false)

                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    granularity = 1f
                    valueFormatter = IndexAxisValueFormatter(labels)
                    textSize = 10f
                    labelRotationAngle = 45f
                    setDrawGridLines(false)
                    setLabelCount(5, true)
                    spaceMin = 0.5f
                    spaceMax = 0.5f
                    labelCount = 5
                }

                axisLeft.apply {
                    axisMinimum = 0f
                    axisMaximum = 10f
                    textSize = 12f
                    setDrawGridLines(true)
                }
                axisRight.isEnabled = false

                legend.apply {
                    textSize = 12f
                    isEnabled = true
                    verticalAlignment = Legend.LegendVerticalAlignment.TOP
                    horizontalAlignment = Legend.LegendHorizontalAlignment.RIGHT
                    orientation = Legend.LegendOrientation.VERTICAL
                    setDrawInside(false)
                    yOffset = 10f
                    xOffset = 10f
                }

                setPinchZoom(true)
                setScaleEnabled(true)
                setExtraOffsets(35f, 20f, 60f, 35f)
                setVisibleXRangeMaximum(entries.size * 0.8f)
                minOffset = 30f
                viewPortHandler.setChartDimens(width * 1.2f, height.toFloat())
            }
        },
        update = { chart ->
            val dataSet = LineDataSet(entries, "Crime Severity").apply {
                color = Color.rgb(106, 90, 205)
                setCircleColor(Color.rgb(106, 90, 205))
                lineWidth = 2f
                circleRadius = 3f
                valueTextSize = 10f
                valueTextColor = Color.BLACK
                setDrawFilled(true)
                fillColor = Color.rgb(106, 90, 205)
                fillAlpha = 30
                setDrawValues(false)
                isHighlightEnabled = true
                highLightColor = Color.rgb(80, 80, 160)
            }

            val lineData = LineData(dataSet)
            lineData.setValueTextSize(9f)
            
            chart.data = lineData
            chart.moveViewToX(0f)
            chart.zoom(1.5f, 1f, 0f, 0f)
            chart.fitScreen()
            chart.animateX(1000)
            chart.invalidate()
        }
    )
}
