package com.example.navi

import android.content.Context
import android.graphics.RectF
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

    fun setImageBounds(bounds: RectF, logicalW: Float, logicalH: Float) {
        imageBounds = bounds
        logicalWidth = logicalW
        logicalHeight = logicalH
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val touchX = event.x
            val touchY = event.y

            imageBounds?.let { bounds ->
                val logicalX = ((touchX - bounds.left) / bounds.width()) * logicalWidth
                val logicalY = ((touchY - bounds.top) / bounds.height()) * logicalHeight

                Log.d("CoordinateDebugger", "Grid coords: (%.2f, %.2f)".format(logicalX, logicalY))
            }
        }
        return true
    }
}
