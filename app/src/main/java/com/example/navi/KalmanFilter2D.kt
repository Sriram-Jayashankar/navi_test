package com.example.navi

class KalmanFilter2D(
    private val q: Float = 0.1f,   // process noise
    private val r: Float = 4f,     // measurement noise
    initialEstimateX: Float = 0f,
    initialEstimateY: Float = 0f,
    initialError: Float = 10f
) {
    private var xhatX = initialEstimateX
    private var xhatY = initialEstimateY
    private var pX = initialError
    private var pY = initialError

    fun update(x: Float, y: Float): Pair<Float, Float> {
        // Prediction update
        pX += q
        pY += q

        // Measurement update
        val kX = pX / (pX + r)
        val kY = pY / (pY + r)

        xhatX += kX * (x - xhatX)
        xhatY += kY * (y - xhatY)

        pX *= (1 - kX)
        pY *= (1 - kY)

        return Pair(xhatX, xhatY)
    }
}
