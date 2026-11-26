package com.icarus.pixelinvaders.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionTest {

    @Test
    fun overlapDetectsIntersectionAndIgnoresTouchingEdges() {
        assertTrue(Collision.overlap(0f, 0f, 10f, 10f, 5f, 5f, 10f, 10f))
        assertFalse(Collision.overlap(0f, 0f, 10f, 10f, 10f, 0f, 5f, 5f)) // touching edge only
        assertFalse(Collision.overlap(0f, 0f, 10f, 10f, 20f, 20f, 5f, 5f))
    }

    @Test
    fun fastBulletDoesNotTunnelThroughThinTarget() {
        // A 5px bullet travelling 80px in one step still hits a 2px target in the middle of its path.
        assertTrue(Collision.sweptVertical(10f, 1f, 5f, 100f, 20f, 8f, 60f, 6f, 2f))
    }

    @Test
    fun sweepMissesTargetOutsideItsPathOrColumn() {
        assertFalse(Collision.sweptVertical(10f, 1f, 5f, 100f, 20f, 8f, 5f, 6f, 2f)) // beyond the end
        assertFalse(Collision.sweptVertical(10f, 1f, 5f, 100f, 20f, 30f, 60f, 6f, 2f)) // wrong column
    }

    @Test
    fun sweepWorksDownwardToo() {
        assertTrue(Collision.sweptVertical(10f, 3f, 6f, 10f, 90f, 9f, 50f, 8f, 2f))
        assertFalse(Collision.sweptVertical(10f, 3f, 6f, 10f, 40f, 9f, 50f, 8f, 2f))
    }
}
