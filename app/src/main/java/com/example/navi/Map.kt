package com.example.navi

import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class Map : AppCompatActivity() {

    // Define your "logical" map dimensions
    private val logicalWidth = 251f
    private val logicalHeight = 390f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        val imageView: ImageView = findViewById(R.id.imageView)
        val mapOverlay: PinOverlayView = findViewById(R.id.mapOverlay)

        // Load your floor plan image (actual size = 2519x3901 in pixels)
        imageView.setImageResource(R.drawable.map_image)

        imageView.post {
            // 1) Get the actual on-screen bounds (after scaling/fitting)
            val drawable = imageView.drawable ?: return@post
            val matrix = imageView.imageMatrix
            val values = FloatArray(9)
            matrix.getValues(values)

            val scaleX = values[Matrix.MSCALE_X]
            val scaleY = values[Matrix.MSCALE_Y]
            val transX = values[Matrix.MTRANS_X]
            val transY = values[Matrix.MTRANS_Y]

            // The displayed rectangle of the image on the screen
            val intrinsicWidth = drawable.intrinsicWidth.toFloat()
            val intrinsicHeight = drawable.intrinsicHeight.toFloat()
            val imageBounds = RectF(
                transX,
                transY,
                transX + intrinsicWidth * scaleX,
                transY + intrinsicHeight * scaleY
            )

            // Pass to overlay if needed
            mapOverlay.setImageBounds(imageBounds)

            Log.d("MapDebug", "Image Bounds: $imageBounds")

            // 2) Suppose the user wants to plot a point at the new "logical" coords:
            //    (251, 390) => bottom-right in your 251×390 space
            //    or (125.5, 195.5) => near the center
            val logicalX = 100f
            val logicalY = 390f

            // 3) Convert from logical coords [0..251, 0..390] to on-screen coords
            val mappedX = imageBounds.left + (logicalX / logicalWidth) * imageBounds.width()
            val mappedY = imageBounds.top + (logicalY / logicalHeight) * imageBounds.height()

            Log.d("MapDebug", "Logical ($logicalX, $logicalY) -> Screen ($mappedX, $mappedY)")

            // 4) Plot a marker at that screen coordinate
            mapOverlay.addMarker(mappedX, mappedY)
        }
    }
}
