package com.example.navi

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

class PinOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paintGrid = Paint().apply {
        color = Color.GRAY
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }

    private val paintMarker = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val paintUser = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.FILL
    }

    private val paintText = Paint().apply {
        color = Color.BLACK
        textSize = 28f
        isAntiAlias = true
    }

    private val paintPathNode = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
    }

    private val paintDebug = Paint().apply {
        color = Color.MAGENTA
        style = Paint.Style.FILL
    }

    private val paintPath = Paint().apply {
        color = Color.BLUE
        strokeWidth = 5f
        style = Paint.Style.STROKE
    }

    private val markers = mutableListOf<Triple<Float, Float, Int>>()
    private var imageBounds: RectF? = null
    private var logicalWidth = 251f
    private var logicalHeight = 390f

    private var userMarker: Pair<Float, Float>? = null
    private var debugMarker: Pair<Float, Float>? = null
    private var pathPoints: List<Pair<Float, Float>> = listOf()

    fun setDebugMarker(logicalX: Float, logicalY: Float) {
        debugMarker = Pair(logicalX, logicalY)
        invalidate()
    }

    fun setPath(points: List<Pair<Float, Float>>) {
        pathPoints = points
        invalidate()
    }

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
        userMarker = null
        invalidate()
    }

    fun setUserMarker(x: Float, y: Float) {
        Log.d("UserMarker", "Setting user marker at screen=($x, $y)")
        userMarker = Pair(x, y)
        invalidate()
    }

    fun clearUserMarker() {
        userMarker = null
        invalidate()
    }

    private fun logicalToScreen(pos: Pair<Float, Float>): Pair<Float, Float> {
        val bounds = imageBounds ?: return pos
        val screenX = bounds.left + (pos.first / logicalWidth) * bounds.width()
        val screenY = bounds.top + (pos.second / logicalHeight) * bounds.height()
        return Pair(screenX, screenY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        imageBounds?.let { bounds ->
            val stepX = bounds.width() / logicalWidth
            val stepY = bounds.height() / logicalHeight

            /*// Draw grid
            for (i in 0..logicalWidth.toInt()) {
                val x = bounds.left + i * stepX
                canvas.drawLine(x, bounds.top, x, bounds.bottom, paintGrid)
            }
            for (i in 0..logicalHeight.toInt()) {
                val y = bounds.top + i * stepY
                canvas.drawLine(bounds.left, y, bounds.right, y, paintGrid)
            }*/

            // Draw router markers
            for ((x, y, rssi) in markers) {
                canvas.drawCircle(x, y, 15f, paintMarker)
                canvas.drawText("$rssi dBm", x + 20f, y - 10f, paintText)
            }

            // Draw user marker
            userMarker?.let { (ux, uy) ->
                canvas.drawCircle(ux, uy, 15f, paintUser)
                canvas.drawText("You", ux + 20f, uy - 10f, paintText)
            }

            /*// Draw path graph nodes
            for (node in PathGraph.nodes) {
                val (sx, sy) = logicalToScreen(Pair(node.x, node.y))
                canvas.drawCircle(sx, sy, 5f, paintPathNode)
            }*/

//            // Draw debug marker
//            debugMarker?.let { (lx, ly) ->
//                val (sx, sy) = logicalToScreen(Pair(lx, ly))
//                canvas.drawCircle(sx, sy, 12f, paintDebug)
//            }

            // Draw path
            if (pathPoints.size >= 2) {
                for (i in 0 until pathPoints.size - 1) {
                    val (x1, y1) = logicalToScreen(pathPoints[i])
                    val (x2, y2) = logicalToScreen(pathPoints[i + 1])
                    canvas.drawLine(x1, y1, x2, y2, paintPath)
                }
            }
        }
    }
}
