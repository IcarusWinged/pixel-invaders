package com.icarus.pixelinvaders.game

import kotlin.math.max
import kotlin.math.min

/** Pure axis-aligned box tests. Everything that moves vertically is swept so nothing tunnels. */
object Collision {

    fun overlap(
        ax: Float, ay: Float, aw: Float, ah: Float,
        bx: Float, by: Float, bw: Float, bh: Float,
    ): Boolean = ax < bx + bw && ax + aw > bx && ay < by + bh && ay + ah > by

    /**
     * True if a box of size ([w] x [h]) at column [x] moving vertically from [y0] to [y1] touches the
     * target box at any point along the way, however far it travels in one step.
     */
    fun sweptVertical(
        x: Float, w: Float, h: Float, y0: Float, y1: Float,
        tx: Float, ty: Float, tw: Float, th: Float,
    ): Boolean {
        val top = min(y0, y1)
        val bottom = max(y0, y1) + h
        return overlap(x, top, w, bottom - top, tx, ty, tw, th)
    }
}
