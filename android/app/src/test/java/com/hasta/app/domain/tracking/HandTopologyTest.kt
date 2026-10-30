package com.hasta.app.domain.tracking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HandTopologyTest {

    @Test
    fun `there are 21 landmarks and 21 connections`() {
        assertEquals(21, HandTopology.LANDMARK_COUNT)
        assertEquals(21, HandTopology.connections.size)
    }

    @Test
    fun `every connection index is a valid landmark`() {
        HandTopology.connections.forEach { (start, end) ->
            assertTrue("start $start out of range", start in 0 until HandTopology.LANDMARK_COUNT)
            assertTrue("end $end out of range", end in 0 until HandTopology.LANDMARK_COUNT)
        }
    }

    @Test
    fun `no connection joins a landmark to itself`() {
        HandTopology.connections.forEach { (start, end) ->
            assertTrue("self connection at $start", start != end)
        }
    }

    @Test
    fun `no duplicate connections`() {
        val normalised = HandTopology.connections.map { (a, b) -> minOf(a, b) to maxOf(a, b) }
        assertEquals(normalised.size, normalised.toSet().size)
    }

    @Test
    fun `every landmark is part of the skeleton`() {
        val used = HandTopology.connections.flatMap { (a, b) -> listOf(a, b) }.toSet()
        assertEquals((0 until HandTopology.LANDMARK_COUNT).toSet(), used)
    }

    @Test
    fun `wrist has the expected three connections`() {
        val wristConnections = HandTopology.connections.count { (a, b) -> a == 0 || b == 0 }
        assertEquals(3, wristConnections)
    }

    @Test
    fun `every fingertip is reachable from the wrist`() {
        val adjacency = mutableMapOf<Int, MutableSet<Int>>()
        HandTopology.connections.forEach { (a, b) ->
            adjacency.getOrPut(a) { mutableSetOf() }.add(b)
            adjacency.getOrPut(b) { mutableSetOf() }.add(a)
        }
        val visited = mutableSetOf(0)
        val queue = ArrayDeque(listOf(0))
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            adjacency[current].orEmpty().forEach { next ->
                if (visited.add(next)) queue.addLast(next)
            }
        }
        assertEquals((0 until 21).toSet(), visited)
        listOf(4, 8, 12, 16, 20).forEach { tip ->
            assertTrue("tip $tip unreachable", tip in visited)
        }
    }
}
