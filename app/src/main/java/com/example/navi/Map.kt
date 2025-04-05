package com.example.navi

import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.pow

// The activity is named "Map" (we fully qualify kotlin.collections.Map where needed)
class Map : AppCompatActivity() {

    private val logicalWidth = 251f
    private val logicalHeight = 390f


    private lateinit var mapOverlay: PinOverlayView
    private lateinit var imageBounds: RectF
    private lateinit var wifiScanner: WifiScanner

    // Define three routers with known logical positions.
    private val routerPositions: kotlin.collections.Map<String, Pair<Float, Float>> = mapOf(
        "Xiaomi_0775_F88A" to Pair(175f, 275f),
        "Keerthan_dlink" to Pair(125f, 335f),
        "MATHRUSHREE-2.4GHZ" to Pair(75f, 300f)
    )

    // To store the latest raw RSSI values.
    private val lastResults = mutableMapOf<String, Int>()
    // Maintain a Kalman filter for each router.
    private val kalmanFilters = mutableMapOf<String, KalmanFilter1D>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val imageView: ImageView = findViewById(R.id.imageView)
        mapOverlay = findViewById(R.id.mapOverlay)
        imageView.setImageResource(R.drawable.map_image)

        // Initialize a Kalman filter for each router.
        for (ssid in routerPositions.keys) {
            // Example parameters: process noise q = 1, measurement noise r = 4,
            // initial estimate = -80 dBm, and initial error = 10.
            kalmanFilters[ssid] = KalmanFilter1D(q = 0.5f, r = 2f, initialEstimate = -60f, initialError = 5f)
        }

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

            //for clicking and getting grid coordinates for testing purposes
            /*val debugOverlay: CoordinateDebugger = findViewById(R.id.debugOverlay)
            debugOverlay.setImageBounds(imageBounds, logicalWidth, logicalHeight)*/

            startWifiScanner()
        }

        mapOverlay.setDebugMarker(180f, 120f) // logical coordinates

    }

    private fun startWifiScanner() {
        val targetSSIDs = routerPositions.keys.toList()
        wifiScanner = WifiScanner(this, targetSSIDs) { resultsMap: kotlin.collections.Map<String, Int> ->
            runOnUiThread {
                // Log raw scan results.
                Log.d("MapDebug", "Raw scan results: $resultsMap")

                // Update lastResults and Kalman filters.
                resultsMap.forEach { (ssid, rawRssi) ->
                    lastResults[ssid] = rawRssi
                    kalmanFilters[ssid]?.update(rawRssi.toFloat())
                    Log.d("MapDebug", "Kalman filtered RSSI for $ssid: ${kalmanFilters[ssid]?.xhat}")
                }

                // Clear previous markers.
                mapOverlay.clearMarkers()

                // Plot router markers (red) using filtered RSSI.
                for ((ssid, pos) in routerPositions) {
                    val (lx, ly) = pos
                    val screenX = imageBounds.left + (lx / logicalWidth) * imageBounds.width()
                    val screenY = imageBounds.top + (ly / logicalHeight) * imageBounds.height()
                    val filteredRssi = kalmanFilters[ssid]?.xhat ?: -999f
                    mapOverlay.addMarker(screenX, screenY, filteredRssi.toInt())
                    Log.d("MapDebug", "$ssid -> filtered RSSI: $filteredRssi dBm at ($screenX, $screenY)")
                }

                // Perform trilateration only if readings for all routers are available.
                if (lastResults.keys.containsAll(routerPositions.keys)) {
                    val filtered1 = kalmanFilters["Xiaomi_0775_F88A"]!!.xhat
                    val filtered2 = kalmanFilters["Keerthan_dlink"]!!.xhat
                    val filtered3 = kalmanFilters["MATHRUSHREE-2.4GHZ"]!!.xhat

                    val d1 = rssiToDistance(filtered1.toInt())
                    val d2 = rssiToDistance(filtered2.toInt())
                    val d3 = rssiToDistance(filtered3.toInt())
                    Log.d("MapDebug", "Distances: d1=$d1, d2=$d2, d3=$d3")
                    val p1 = routerPositions.getValue("Xiaomi_0775_F88A")
                    val p2 = routerPositions.getValue("Keerthan_dlink")
                    val p3 = routerPositions.getValue("MATHRUSHREE-2.4GHZ")

                    //val userLogicalPos = trilaterate(p1, d1, p2, d2, p3, d3)
                    val userLogicalPos = trilaterateCentroidWeighted(listOf(
                        Pair(p1, d1),
                        Pair(p2, d2),
                        Pair(p3, d3)
                    ))




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

    // Convert RSSI to estimated distance using a log-normal path-loss model.
    private fun rssiToDistance(rssi: Int, txPower: Int = -47, n: Double = 2.5): Float {
        // d = 10^((txPower - RSSI) / (10*n))
        return 10f.pow(((txPower - rssi) / (10 * n)).toFloat())
    }

    // Basic trilateration function.
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
    private fun trilaterateCentroidWeighted(
        routers: List<Pair<Pair<Float, Float>, Float>> // Pair<position, distance>
    ): Pair<Float, Float> {
        val weights = routers.map { 1f / it.second.coerceAtLeast(0.1f) } // Closer router = more weight
        val totalWeight = weights.sum()

        var x = 0f
        var y = 0f
        for (i in routers.indices) {
            val (pos, _) = routers[i]
            val w = weights[i]
            x += pos.first * w
            y += pos.second * w
        }
        return Pair(x / totalWeight, y / totalWeight)
    }


    override fun onDestroy() {
        super.onDestroy()
        wifiScanner.stop()
    }
}
