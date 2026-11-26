package com.icarus.pixelinvaders.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF
import com.icarus.pixelinvaders.game.BlastKind
import com.icarus.pixelinvaders.game.GameState
import com.icarus.pixelinvaders.game.Phase
import com.icarus.pixelinvaders.game.Shield
import com.icarus.pixelinvaders.game.World
import kotlin.math.roundToInt

/**
 * Draws a [GameState] from the monochrome sprite [atlas]. The 160x144 logical grid is scaled by a
 * whole number so every game pixel is a crisp square; sprites are tinted per theme.
 */
class GameRenderer(private val atlas: Bitmap) {
    private val src = Rect()
    private val dst = RectF()
    private val tints = HashMap<Int, Paint>()
    private val fill = Paint().apply { isAntiAlias = false }

    private fun tint(color: Int): Paint = tints.getOrPut(color) {
        Paint().apply {
            isAntiAlias = false
            isFilterBitmap = false
            isDither = false
            colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
        }
    }

    /** Integer scale that fits [widthPx], between 1x and 3x. */
    fun scaleFor(widthPx: Int): Int = (widthPx / World.WIDTH.toInt()).coerceIn(1, 3)

    @Synchronized
    fun render(state: GameState, palette: Palette, widthPx: Int): Bitmap {
        val k = scaleFor(widthPx)
        val bmp = Bitmap.createBitmap(World.WIDTH.toInt() * k, World.HEIGHT.toInt() * k, Bitmap.Config.RGB_565)
        val c = Canvas(bmp)
        c.scale(k.toFloat(), k.toFloat())
        c.drawColor(palette.bg)

        drawGround(c, palette)
        for (sh in state.shields) drawShield(c, sh, palette.shield)
        drawAliens(c, state, palette)
        drawUfo(c, state, palette)
        drawShip(c, state, palette)
        drawProjectiles(c, state, palette)
        drawBlasts(c, state, palette)
        drawHud(c, state, palette)
        drawOverlay(c, state, palette)
        return bmp
    }

    // --- Layers ------------------------------------------------------------------------------

    private fun drawGround(c: Canvas, p: Palette) {
        fill.color = p.ground
        c.drawRect(0f, World.GROUND_Y, World.WIDTH, World.GROUND_Y + 1f, fill)
    }

    private fun drawShield(c: Canvas, sh: Shield, color: Int) {
        fill.color = color
        for (r in 0 until Shield.H) {
            var run = -1
            for (col in 0..Shield.W) {
                val solid = col < Shield.W && sh.solidLocal(col, r)
                if (solid && run < 0) run = col
                if (!solid && run >= 0) {
                    c.drawRect((sh.x + run).toFloat(), (sh.y + r).toFloat(), (sh.x + col).toFloat(), (sh.y + r + 1).toFloat(), fill)
                    run = -1
                }
            }
        }
    }

    private fun drawAliens(c: Canvas, s: GameState, p: Palette) {
        val frame = s.animFrame()
        for (a in s.aliens) {
            if (!a.alive) continue
            val color = when (a.row) {
                0 -> p.alienTop
                1, 2 -> p.alienMid
                else -> p.alienLow
            }
            sprite(c, Sprites.alienFrames(a.row)[frame], s.alienX(a), s.alienY(a), color)
        }
    }

    private fun drawUfo(c: Canvas, s: GameState, p: Palette) {
        val u = s.ufo ?: return
        sprite(c, Sprites.UFO, u.x, World.UFO_Y, p.ufo)
    }

    private fun drawShip(c: Canvas, s: GameState, p: Palette) {
        if (s.phase == Phase.OVER && s.lives <= 0) {
            sprite(c, Sprites.SHIP_BOOM[0], s.shipX, World.SHIP_Y, p.ship)
            return
        }
        if (s.shipTimer > 0f) {
            val i = (s.shipTimer * 8f).toInt() and 1
            sprite(c, Sprites.SHIP_BOOM[i], s.shipX, World.SHIP_Y, p.ship)
            return
        }
        // Blink while invulnerable after a respawn.
        if (s.invuln > 0f && ((s.invuln * 10f).toInt() and 1) == 0) return
        sprite(c, Sprites.SHIP, s.shipX, World.SHIP_Y, p.ship)
    }

    private fun drawProjectiles(c: Canvas, s: GameState, p: Palette) {
        s.bullet?.let { sprite(c, Sprites.BULLET, it.x, it.y, p.bullet) }
        for (b in s.bombs) sprite(c, Sprites.BOMB[(b.anim * 8f).toInt() and 1], b.x, b.y, p.bomb)
    }

    private fun drawBlasts(c: Canvas, s: GameState, p: Palette) {
        for (b in s.blasts) {
            val sp = if (b.kind == BlastKind.UFO) Sprites.UFO_BOOM else Sprites.ALIEN_BOOM
            sprite(c, sp, b.x, if (b.kind == BlastKind.UFO) World.UFO_Y else b.y, p.blast)
        }
    }

    private fun drawHud(c: Canvas, s: GameState, p: Palette) {
        text(c, "SCORE ${s.score}", 3f, 2f, p.text)
        text(c, "W${s.wave}", 70f, 2f, p.text)
        // Remaining lives as small ships, right-aligned.
        val shown = if (s.phase == Phase.OVER) 0 else s.lives
        for (i in 0 until shown) {
            val x = World.WIDTH - 3f - (i + 1) * 15f + 2f
            sprite(c, Sprites.SHIP, x, 1f, p.ship)
        }
    }

    private fun drawOverlay(c: Canvas, s: GameState, p: Palette) {
        when {
            s.phase == Phase.READY -> {
                panel(c, p, 70f, 30f)
                textCentered(c, "PIXEL INVADERS", 74f, p.text, 1)
                textCentered(c, "FIRE TO START", 82f, p.text, 1)
                textCentered(c, "BEST ${s.best}", 90f, p.text, 1)
            }
            s.phase == Phase.OVER -> {
                panel(c, p, 68f, 42f)
                textCentered(c, "GAME OVER", 72f, p.text, 2)
                textCentered(c, "SCORE ${s.score}", 88f, p.text, 1)
                textCentered(c, "BEST ${s.best}", 95f, p.text, 1)
                textCentered(c, "FIRE TO RETRY", 102f, p.text, 1)
            }
            s.wavePause > 0f -> {
                panel(c, p, 74f, 18f)
                textCentered(c, "WAVE ${s.wave} CLEAR!", 80f, p.text, 1)
            }
        }
    }

    // --- Primitives --------------------------------------------------------------------------

    /** A bordered box that sits over the playfield so overlay text stays readable. */
    private fun panel(c: Canvas, p: Palette, top: Float, height: Float) {
        val left = PANEL_INSET
        val right = World.WIDTH - PANEL_INSET
        fill.color = p.bg
        c.drawRect(left, top, right, top + height, fill)
        fill.color = p.text
        c.drawRect(left, top, right, top + 1f, fill)
        c.drawRect(left, top + height - 1f, right, top + height, fill)
        c.drawRect(left, top, left + 1f, top + height, fill)
        c.drawRect(right - 1f, top, right, top + height, fill)
    }

    private fun sprite(c: Canvas, s: Sprite, x: Float, y: Float, color: Int) {
        src.set(s.x, s.y, s.x + s.w, s.y + s.h)
        val rx = x.roundToInt().toFloat()
        val ry = y.roundToInt().toFloat()
        dst.set(rx, ry, rx + s.w, ry + s.h)
        c.drawBitmap(atlas, src, dst, tint(color))
    }

    private fun text(c: Canvas, str: String, x: Float, y: Float, color: Int, scale: Int = 1) {
        var cx = x
        for (ch in str) {
            val g = Sprites.glyph(ch)
            if (g != null) {
                src.set(g.x, g.y, g.x + g.w, g.y + g.h)
                val rx = cx.roundToInt().toFloat()
                val ry = y.roundToInt().toFloat()
                dst.set(rx, ry, rx + g.w * scale, ry + g.h * scale)
                c.drawBitmap(atlas, src, dst, tint(color))
            }
            cx += Sprites.FONT_PITCH * scale
        }
    }

    private fun textCentered(c: Canvas, str: String, y: Float, color: Int, scale: Int) {
        val w = str.length * Sprites.FONT_PITCH * scale - scale
        text(c, str, (World.WIDTH - w) / 2f, y, color, scale)
    }

    private companion object {
        const val PANEL_INSET = 18f
    }
}
