package com.example.navi

import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

// Activity class remains "Map"
class Map : AppCompatActivity() {

    private val logicalWidth = 251f
    private val logicalHeight = 390f

    private lateinit var mapOverlay: PinOverlayView
    private lateinit var imageBounds: RectF
    private lateinit var wifiScanner: WifiScanner

    // Rename the variable to avoid conflict with kotlin.collections.Map
    private val routerPositions: kotlin.collections.Map<String, Pair<Float, Float>> = mapOf(
        "sanath" to Pair(50f, 50f),
        "Vishnu5G-google" to Pair(100f, 100f)
    )

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
                // Clear previous markers before updating.
                mapOverlay.clearMarkers()
                // Iterate through all known routers.
                for ((ssid, pos) in routerPositions) {
                    val (lx, ly) = pos
                    // Convert logical coordinates to on-screen coordinates.
                    val screenX = imageBounds.left + (lx / logicalWidth) * imageBounds.width()
                    val screenY = imageBounds.top + (ly / logicalHeight) * imageBounds.height()
                    // Use the scanned RSSI if available; default to -999 if not found.
                    val rssi = resultsMap[ssid] ?: -999
                    mapOverlay.addMarker(screenX, screenY, rssi)
                    Log.d("MapDebug", "$ssid -> $rssi dBm at ($screenX, $screenY)")
                }
            }
        }
        wifiScanner.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        wifiScanner.stop()
    }
}
