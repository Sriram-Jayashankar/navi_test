package com.example.navi

object DestinationManager {
    private var destination: Pair<Float, Float>? = null

    fun setDestination(x: Float, y: Float) {
        destination = Pair(x, y)
    }

    fun getDestination(): Pair<Float, Float>? = destination
}
