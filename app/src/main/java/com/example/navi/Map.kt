package com.example.navi

import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow

// Activity class remains "Map"
class Map : AppCompatActivity() {

    private val logicalWidth = 251f
    private val logicalHeight = 390f

    private lateinit var mapOverlay: PinOverlayView
    private lateinit var imageBounds: RectF
    private lateinit var wifiScanner: WifiScanner

    // Define three routers for triangulation.
    // Renamed variable to routerPositions to avoid conflict with kotlin.collections.Map.
    private val routerPositions: kotlin.collections.Map<String, Pair<Float, Float>> = mapOf(
        "sanath" to Pair(50f, 50f),
        "Vishnu5G-google" to Pair(150f, 100f),
        "Gadiya" to Pair(150f, 300f)
    )

    // To store the latest RSSI values from each router.
    private val lastResults = mutableMapOf<String, Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val imageView: ImageView = findViewById(R.id.imageView)
        mapOverlay = findViewById(R.id.mapOverlay)
        imageView.setImageResource(R.drawable.map_image)

        imageView.post {
            val drawable = imageView.drawable ?: return@post
            val matrix = imageView.imageMatrix
            val values = FloatArray(9)
            matrix.getValues(values)

            val scaleX = values[Matrix.MSCALE_X]
            val scaleY = values[Matrix.MSCALE_Y]
            val transX = values[Matrix.MTRANS_X]
            val transY = values[Matrix.MTRANS_Y]

            val intrinsicWidth = drawable.intrinsicWidth.toFloat()
            val intrinsicHeight = drawable.intrinsicHeight.toFloat()

            imageBounds = RectF(
                transX,
                transY,
                transX + intrinsicWidth * scaleX,
                transY + intrinsicHeight * scaleY
            )

            mapOverlay.setImageBounds(imageBounds)
            startWifiScanner()
        }
    }

    private fun startWifiScanner() {
        val targetSSIDs = routerPositions.keys.toList()
        wifiScanner = WifiScanner(this, targetSSIDs) { resultsMap: kotlin.collections.Map<String, Int> ->
            runOnUiThread {
                // Log the raw scan results.
                Log.d("MapDebug", "Scan results: $resultsMap")

                // Update lastResults with any new values.
                resultsMap.forEach { (ssid, rssi) ->
                    lastResults[ssid] = rssi
                }
                Log.d("MapDebug", "Last RSSI values: $lastResults")

                // Clear previous markers before updating.
                mapOverlay.clearMarkers()

                // Plot router markers (red) with their current RSSI.
                for ((ssid, pos) in routerPositions) {
                    val (lx, ly) = pos
                    val screenX = imageBounds.left + (lx / logicalWidth) * imageBounds.width()
                    val screenY = imageBounds.top + (ly / logicalHeight) * imageBounds.height()
                    val rssi = lastResults[ssid] ?: -999
                    mapOverlay.addMarker(screenX, screenY, rssi)
                    Log.d("MapDebug", "$ssid -> $rssi dBm at ($screenX, $screenY)")
                }

                // Check if we have all three router readings.
                if (lastResults.keys.containsAll(routerPositions.keys)) {
                    val rssi1 = lastResults["sanath"]!!
                    val rssi2 = lastResults["Vishnu5G-google"]!!
                    val rssi3 = lastResults["Gadiya"]!!

                    // Convert RSSI to estimated distances.
                    val d1 = rssiToDistance(rssi1)
                    val d2 = rssiToDistance(rssi2)
                    val d3 = rssiToDistance(rssi3)
                    Log.d("MapDebug", "Distances: d1=$d1, d2=$d2, d3=$d3")

                    val p1 = routerPositions["sanath"]!!
                    val p2 = routerPositions["Vishnu5G-google"]!!
                    val p3 = routerPositions["Gadiya"]!!

                    // Perform trilateration to get user logical position.
                    val userLogicalPos = trilaterate(p1, d1, p2, d2, p3, d3)
                    if (userLogicalPos != null) {
                        val (ux, uy) = userLogicalPos
                        Log.d("MapDebug", "User logical position: ($ux, $uy)")
                        val screenUX = imageBounds.left + (ux / logicalWidth) * imageBounds.width()
                        val screenUY = imageBounds.top + (uy / logicalHeight) * imageBounds.height()
                        mapOverlay.setUserMarker(screenUX, screenUY)
                        Log.d("MapDebug", "User mapped to screen: ($screenUX, $screenUY)")
                    } else {
                        Log.d("MapDebug", "Trilateration returned null")
                        mapOverlay.clearUserMarker()
                    }
                } else {
                    Log.d("MapDebug", "Not all router readings available")
                    mapOverlay.clearUserMarker()
                }
            }
        }
        wifiScanner.start()
    }

    // Convert RSSI to estimated distance using a simple path-loss model.
    private fun rssiToDistance(rssi: Int, txPower: Int = -40, n: Double = 2.0): Float {
        // Formula: distance = 10 ^ ((txPower - RSSI) / (10 * n))
        return 10f.pow(((txPower - rssi) / (10 * n)).toFloat())
    }

    // Perform trilateration to estimate user position from three routers.
    private fun trilaterate(
        p1: Pair<Float, Float>, d1: Float,
        p2: Pair<Float, Float>, d2: Float,
        p3: Pair<Float, Float>, d3: Float
    ): Pair<Float, Float>? {
        val x1 = p1.first.toDouble()
        val y1 = p1.second.toDouble()
        val x2 = p2.first.toDouble()
        val y2 = p2.second.toDouble()
        val x3 = p3.first.toDouble()
        val y3 = p3.second.toDouble()
        val r1 = d1.toDouble()
        val r2 = d2.toDouble()
        val r3 = d3.toDouble()

        val A = 2 * (x2 - x1)
        val B = 2 * (y2 - y1)
        val C = r1 * r1 - r2 * r2 - x1 * x1 + x2 * x2 - y1 * y1 + y2 * y2
        val D = 2 * (x3 - x2)
        val E = 2 * (y3 - y2)
        val F = r2 * r2 - r3 * r3 - x2 * x2 + x3 * x3 - y2 * y2 + y3 * y3

        val denominator = A * E - B * D
        if (denominator == 0.0) {
            Log.d("MapDebug", "Denominator zero in trilateration")
            return null
        }

        val x = (C * E - F * B) / denominator
        val y = (A * F - D * C) / denominator

        return Pair(x.toFloat(), y.toFloat())
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiScanner.stop()
    }
}
