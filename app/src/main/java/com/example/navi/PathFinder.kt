package com.example.navi

import kotlin.math.*

object PathFinder {
    fun aStar(start: Node, goal: Node, nodes: List<Node>, edges: List<Edge>): List<Node> {
        val openSet = mutableSetOf(start.id)
        val cameFrom = mutableMapOf<Int, Int>()
        val gScore = nodes.associate { it.id to Float.POSITIVE_INFINITY }.toMutableMap()
        val fScore = nodes.associate { it.id to Float.POSITIVE_INFINITY }.toMutableMap()

        gScore[start.id] = 0f
        fScore[start.id] = heuristic(start, goal)

        while (openSet.isNotEmpty()) {
            val currentId = openSet.minByOrNull { fScore[it] ?: Float.POSITIVE_INFINITY } ?: break
            val current = nodes.first { it.id == currentId }

            if (current.id == goal.id) {
                return reconstructPath(cameFrom, current.id, nodes)
            }

            openSet.remove(current.id)

            val neighbors = edges.filter { it.fromId == current.id }.mapNotNull { edge ->
                nodes.find { it.id == edge.toId }
            }

            for (neighbor in neighbors) {
                val tentativeG = gScore[current.id]!! + distance(current, neighbor)
                if (tentativeG < gScore[neighbor.id]!!) {
                    cameFrom[neighbor.id] = current.id
                    gScore[neighbor.id] = tentativeG
                    fScore[neighbor.id] = tentativeG + heuristic(neighbor, goal)
                    openSet.add(neighbor.id)
                }
            }
        }
        return emptyList()
    }

    private fun heuristic(a: Node, b: Node): Float {
        return distance(a, b) // Euclidean distance
    }

    private fun distance(a: Node, b: Node): Float {
        return sqrt((a.x - b.x).pow(2) + (a.y - b.y).pow(2))
    }

    private fun reconstructPath(cameFrom: kotlin.collections.Map<Int, Int>, currentId: Int, nodes: List<Node>): List<Node> {
        var current = currentId
        val path = mutableListOf<Node>()
        while (cameFrom.containsKey(current)) {
            path.add(nodes.first { it.id == current })
            current = cameFrom[current]!!
        }
        path.add(nodes.first { it.id == current }) // add start node
        return path.reversed()
    }
}
