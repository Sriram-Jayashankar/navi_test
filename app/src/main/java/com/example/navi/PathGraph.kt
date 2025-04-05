package com.example.navi

import kotlin.math.*

data class Node(val id: Int, val x: Float, val y: Float)
data class Edge(val fromId: Int, val toId: Int)

object PathGraph {
    private const val STEP = 10f
    private var nextId = 0

    val nodes = mutableListOf<Node>()
    val edges = mutableListOf<Edge>()

    init {
        generateSegment(10f, 60f, 230f, 60f)
        generateSegment(10f, 330f, 230f, 330f)
        generateSegment(80f, 60f, 80f, 330f)
        generateSegment(170f, 60f, 170f, 330f)

        // Example of manual edge
        addEdgeByCoords(50f, 200f, 50f, 0f)
    }

    fun generateSegment(x1: Float, y1: Float, x2: Float, y2: Float) {
        val dx = x2 - x1
        val dy = y2 - y1
        val distance = sqrt(dx * dx + dy * dy)
        val steps = (distance / STEP).toInt().coerceAtLeast(1)

        var prevNodeId: Int? = null

        for (i in 0..steps) {
            val t = i.toFloat() / steps
            val x = x1 + t * dx
            val y = y1 + t * dy

            // Check if this coordinate already exists
            val existing = nodes.find { it.x == x && it.y == y }
            val node = existing ?: Node(nextId++, x, y).also { nodes.add(it) }

            if (prevNodeId != null) {
                edges.add(Edge(prevNodeId, node.id))
            }
            prevNodeId = node.id
        }
    }


    fun addEdgeByCoords(x1: Float, y1: Float, x2: Float, y2: Float) {
        val fromNode = nodes.minByOrNull { (it.x - x1).pow(2) + (it.y - y1).pow(2) }
        val toNode = nodes.minByOrNull { (it.x - x2).pow(2) + (it.y - y2).pow(2) }

        if (fromNode != null && toNode != null) {
            edges.add(Edge(fromNode.id, toNode.id))
        }
    }

    fun snapToNearest(x: Float, y: Float): Pair<Float, Float> {
        return nodes.minByOrNull { (nx, ny) ->
            (nx - x).pow(2) + (ny - y).pow(2)
        }?.let { Pair(it.x, it.y) } ?: Pair(x, y)
    }
}
