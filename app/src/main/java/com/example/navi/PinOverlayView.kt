package com.example.navi

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class PinOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paintGrid = Paint().apply {
        color = Color.GRAY  // Grid color
        strokeWidth = 1f    // Thin grid lines
        style = Paint.Style.STROKE
    }

    private val paintMarker = Paint().apply {
        color = Color.RED   // Marker color
        style = Paint.Style.FILL
    }

    private val markers = mutableListOf<Pair<Float, Float>>()
    private var imageBounds: RectF? = null

    // Logical map dimensions (must match Map.kt)
    private val logicalWidth = 25f
    private val logicalHeight = 39f

    fun setImageBounds(bounds: RectF) {
        imageBounds = bounds
        invalidate()  // Redraw
    }

    fun addMarker(x: Float, y: Float) {
        markers.add(Pair(x, y))
        invalidate()  // Redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        imageBounds?.let { bounds ->
            val stepX = bounds.width() / logicalWidth
            val stepY = bounds.height() / logicalHeight

            // Draw vertical grid lines (for 251 columns)
            for (i in 0..logicalWidth.toInt()) {
                val x = bounds.left + i * stepX
                canvas.drawLine(x, bounds.top, x, bounds.bottom, paintGrid)
            }

            // Draw horizontal grid lines (for 390 rows)
            for (i in 0..logicalHeight.toInt()) {
                val y = bounds.top + i * stepY
                canvas.drawLine(bounds.left, y, bounds.right, y, paintGrid)
            }

            // Draw markers
            for ((x, y) in markers) {
                canvas.drawCircle(x, y, 15f, paintMarker)
            }
        }
    }
}
