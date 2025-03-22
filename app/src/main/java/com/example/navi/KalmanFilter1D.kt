package com.example.navi

class KalmanFilter1D(var q: Float, var r: Float, initialEstimate: Float, initialError: Float) {
    var xhat: Float = initialEstimate
    var p: Float = initialError

    fun update(z: Float) {
        // Prediction step (constant system: xhat_minus = xhat)
        val xhatMinus = xhat
        val pMinus = p + q
        // Kalman Gain
        val k = pMinus / (pMinus + r)
        // Update estimate with measurement z
        xhat = xhatMinus + k * (z - xhatMinus)
        // Update error covariance
        p = (1 - k) * pMinus
    }
}
