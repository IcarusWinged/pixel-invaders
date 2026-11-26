package com.icarus.pixelinvaders.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ShieldTest {

    private fun shield() = Shield(x = 50, y = 100)

    @Test
    fun shapeHasChamferedTopAndArchedBottom() {
        val s = shield()
        assertFalse(s.solidLocal(0, 0))            // chamfered corner
        assertTrue(s.solidLocal(10, 0))            // top middle
        assertTrue(s.solidLocal(3, 14))            // bottom left leg
        assertFalse(s.solidLocal(10, 14))          // arch
        assertFalse(s.solidLocal(-1, 5))           // out of bounds
        assertTrue(s.pixelCount > 200)
    }

    @Test
    fun erodeRemovesPixelsAroundImpact() {
        val s = shield()
        val before = s.pixelCount
        s.erode(60, 105, 3f, Random(1))
        assertTrue(s.pixelCount < before)
        assertFalse(s.solid(60, 105)) // the impact pixel itself is always gone
    }

    @Test
    fun clearRectRemovesEverythingUnderneath() {
        val s = shield()
        s.clearRect(52f, 102f, 6f, 4f)
        for (x in 52 until 58) for (y in 102 until 106) assertFalse(s.solid(x, y))
        assertTrue(s.solid(55, 110)) // untouched solid pixel elsewhere in the bunker
    }

    @Test
    fun resetRestoresFullShield() {
        val s = shield()
        val full = s.pixelCount
        s.erode(60, 105, 5f, Random(2))
        s.reset()
        assertEquals(full, s.pixelCount)
    }

    @Test
    fun upwardScanFindsTopmostPixelNotTheFirstBelow() {
        val s = shield()
        // Column 60 is solid from the shield's top row; a bullet coming from below hits the lowest solid pixel.
        val hit = s.firstSolidUp(60, 130, 90)
        assertNotNull(hit)
        assertEquals(60, hit!!.x)
        assertTrue(s.solid(hit.x, hit.y))
        assertFalse(s.solid(hit.x, hit.y + 1))
    }

    @Test
    fun scansSkipEmptySpaceAndOtherColumns() {
        val s = shield()
        assertNull(s.firstSolidUp(10, 130, 90))        // column outside the shield
        assertNull(s.firstSolidUp(60, 99, 90))         // range entirely above the shield
        assertNull(s.firstSolidDown(60, 3, 90, 99))    // range entirely above the shield
        assertNotNull(s.firstSolidDown(60, 3, 90, 110))
    }

    @Test
    fun downwardScanReportsFirstRowContactAcrossAllBombColumns() {
        val s = shield()
        s.clearRect(60f, 100f, 1f, 16f)                // carve a hole in column 60
        val hit = s.firstSolidDown(60, 3, 95, 115)     // columns 60..62: 61 and 62 still solid
        assertNotNull(hit)
        assertTrue(hit!!.x in 61..62)
    }
}
