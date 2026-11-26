package com.icarus.pixelinvaders.game

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt
import kotlin.random.Random

/** A solid pixel found by a scan, in world coordinates. */
data class PixelHit(val x: Int, val y: Int)

/**
 * A destructible bunker stored as a bitmask. Projectiles scan it pixel by pixel (so a fast shot
 * cannot skip through) and blasts erode it with a ragged edge.
 */
class Shield(val x: Int, val y: Int) {
    private val mask = BooleanArray(W * H)

    init {
        reset()
    }

    fun reset() {
        for (r in 0 until H) for (c in 0 until W) mask[r * W + c] = shape(c, r)
    }

    val pixelCount: Int get() = mask.count { it }

    fun solid(wx: Int, wy: Int): Boolean = solidLocal(wx - x, wy - y)

    fun solidLocal(c: Int, r: Int): Boolean = c in 0 until W && r in 0 until H && mask[r * W + c]

    /** Scans a single column upward (y decreasing) and returns the first solid pixel. */
    fun firstSolidUp(wx: Int, fromY: Int, toY: Int): PixelHit? {
        var yy = fromY
        while (yy >= toY) {
            if (solid(wx, yy)) return PixelHit(wx, yy)
            yy--
        }
        return null
    }

    /** Scans [cols] columns downward (y increasing) and returns the first solid pixel. */
    fun firstSolidDown(wx: Int, cols: Int, fromY: Int, toY: Int): PixelHit? {
        for (yy in fromY..toY) {
            for (dx in 0 until cols) {
                if (solid(wx + dx, yy)) return PixelHit(wx + dx, yy)
            }
        }
        return null
    }

    /** Removes a ragged blob: solid core, random edge. */
    fun erode(wx: Int, wy: Int, radius: Float, rng: Random) {
        val r = ceil(radius).toInt()
        for (dy in -r..r) {
            for (dx in -r..r) {
                val d = sqrt((dx * dx + dy * dy).toFloat())
                if (d > radius) continue
                if (d <= radius * 0.55f || rng.nextFloat() < 0.55f) clearPixel(wx + dx - x, wy + dy - y)
            }
        }
    }

    /** Removes every pixel under a rectangle (used when aliens march through a bunker). */
    fun clearRect(rx: Float, ry: Float, rw: Float, rh: Float) {
        val c0 = floor(rx).toInt() - x
        val c1 = ceil(rx + rw).toInt() - 1 - x
        val r0 = floor(ry).toInt() - y
        val r1 = ceil(ry + rh).toInt() - 1 - y
        for (r in r0..r1) for (c in c0..c1) clearPixel(c, r)
    }

    private fun clearPixel(c: Int, r: Int) {
        if (c in 0 until W && r in 0 until H) mask[r * W + c] = false
    }

    companion object {
        const val W = World.SHIELD_W
        const val H = World.SHIELD_H

        /** Bunker outline: chamfered top corners and an arched notch at the bottom. */
        fun shape(c: Int, r: Int): Boolean {
            if (r < 4 && (c < 4 - r || c >= W - (4 - r))) return false
            if (r >= 11 && c in 7..14) return false
            if (r == 10 && c in 9..12) return false
            return true
        }
    }
}
