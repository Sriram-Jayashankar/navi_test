package com.example.navi

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import android.util.Log

class PinOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paintGrid = Paint().apply {
        color = Color.GRAY   // Grid color
        strokeWidth = 1f     // Thin grid lines
        style = Paint.Style.STROKE
    }

    private val paintMarker = Paint().apply {
        color = Color.RED    // Router marker color
        style = Paint.Style.FILL
    }

    private val paintUser = Paint().apply {
        color = Color.BLUE   // User marker color
        style = Paint.Style.FILL
    }

    private val paintText = Paint().apply {
        color = Color.BLACK
        textSize = 28f
        isAntiAlias = true
    }
    // List of router markers stored as Triple<x, y, rssi>
    private val markers = mutableListOf<Triple<Float, Float, Int>>()
    private var imageBounds: RectF? = null

    // Logical map dimensions (must match Map.kt)
    private var logicalWidth = 251f
    private var logicalHeight = 390f


    // User marker position (if set)
    private var userMarker: Pair<Float, Float>? = null

    fun setImageBounds(bounds: RectF) {
        imageBounds = bounds
        invalidate()
    }

    fun addMarker(x: Float, y: Float, rssi: Int) {
        markers.add(Triple(x, y, rssi))
        invalidate()
    }

    fun clearMarkers() {
        markers.clear()
        // Also clear the user marker.
        userMarker = null
        invalidate()
    }

    // Set the user marker (drawn in blue)
    fun setUserMarker(x: Float, y: Float) {
        Log.d("UserMarker", "Setting user marker at screen=($x, $y)")
        userMarker = Pair(x, y)
        invalidate()
    }

    // Clear only the user marker.
    fun clearUserMarker() {
        userMarker = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        imageBounds?.let { bounds ->
            val stepX = bounds.width() / logicalWidth
            val stepY = bounds.height() / logicalHeight

            // Draw vertical grid lines.
            for (i in 0..logicalWidth.toInt()) {
                val x = bounds.left + i * stepX
                canvas.drawLine(x, bounds.top, x, bounds.bottom, paintGrid)
            }
            // Draw horizontal grid lines.
            for (i in 0..logicalHeight.toInt()) {
                val y = bounds.top + i * stepY
                canvas.drawLine(bounds.left, y, bounds.right, y, paintGrid)
            }
            // Draw router markers (red) with their RSSI values.
            for ((x, y, rssi) in markers) {
                canvas.drawCircle(x, y, 15f, paintMarker)
                canvas.drawText("$rssi dBm", x + 20f, y - 10f, paintText)
            }
            // Draw the user marker (blue) if set.
            userMarker?.let { (ux, uy) ->
                Log.d("Drawing", "Drawing user at ($ux, $uy)")
                canvas.drawCircle(ux, uy, 15f, paintUser)
                canvas.drawText("You", ux + 20f, uy - 10f, paintText)
            }
        }
    }
}
