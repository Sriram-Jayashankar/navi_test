package com.example.navi

import android.content.Context
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View

class CoordinateDebugger @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var imageBounds: RectF? = null
    private var logicalWidth: Float = 1f
    private var logicalHeight: Float = 1f
    private var longPressHandler: Handler? = null
    private var longPressRunnable: Runnable? = null

    fun setImageBounds(bounds: RectF, logicalW: Float, logicalH: Float) {
        imageBounds = bounds
        logicalWidth = logicalW
        logicalHeight = logicalH
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                longPressHandler = Handler(Looper.getMainLooper())
                val x = event.x
                val y = event.y
                longPressRunnable = Runnable {
                    imageBounds?.let { bounds ->
                        val logicalX = ((x - bounds.left) / bounds.width()) * logicalWidth
                        val logicalY = ((y - bounds.top) / bounds.height()) * logicalHeight

                        val snapped = PathGraph.snapToNearest(logicalX, logicalY)
                        DestinationManager.setDestination(snapped.first, snapped.second)
                        (context as? Map)?.triggerPathRecompute()

                        Log.d("BluePath", "Long press at logical coords (DEST): $snapped")
                    }
                }
                longPressHandler?.postDelayed(longPressRunnable!!, 600) // 600ms for long press
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                longPressHandler?.removeCallbacks(longPressRunnable!!)
            }
        }
        return true
    }
}
