package com.icarus.pixelinvaders.render

/** A rectangle on the sprite sheet. */
data class Sprite(val x: Int, val y: Int, val w: Int, val h: Int)

/**
 * Sprite sheet layout. Mirrors tools/make_assets.py (the generator); SpriteSheetTest checks that
 * every rectangle here holds pixels and that no pixel exists outside them.
 */
object Sprites {
    const val SHEET_W = 160
    const val SHEET_H = 32

    val JELLY = arrayOf(Sprite(0, 0, 11, 8), Sprite(12, 0, 11, 8))
    val BEETLE = arrayOf(Sprite(24, 0, 11, 8), Sprite(36, 0, 11, 8))
    val SKULL = arrayOf(Sprite(48, 0, 11, 8), Sprite(60, 0, 11, 8))
    val ALIEN_BOOM = Sprite(72, 0, 11, 8)
    val UFO = Sprite(84, 0, 16, 7)
    val UFO_BOOM = Sprite(102, 0, 16, 8)

    val SHIP = Sprite(0, 10, 13, 8)
    val SHIP_BOOM = arrayOf(Sprite(14, 10, 13, 8), Sprite(28, 10, 13, 8))
    val BULLET = Sprite(42, 10, 1, 5)
    val BOMB = arrayOf(Sprite(44, 10, 3, 6), Sprite(48, 10, 3, 6))

    const val FONT_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ-:!"
    const val FONT_Y = 22
    const val FONT_PITCH = 4
    const val GLYPH_W = 3
    const val GLYPH_H = 5

    /** The alien sprite pair for a formation row (0 = top). */
    fun alienFrames(row: Int): Array<Sprite> = when (row) {
        0 -> JELLY
        1, 2 -> BEETLE
        else -> SKULL
    }

    /** Glyph for [c], or null for characters without one (spaces and so on). */
    fun glyph(c: Char): Sprite? {
        val i = FONT_CHARS.indexOf(c.uppercaseChar())
        return if (i < 0) null else Sprite(i * FONT_PITCH, FONT_Y, GLYPH_W, GLYPH_H)
    }

    /** Every sprite rectangle, including all font glyphs. */
    fun all(): List<Sprite> {
        val list = ArrayList<Sprite>()
        list += JELLY
        list += BEETLE
        list += SKULL
        list += ALIEN_BOOM
        list += UFO
        list += UFO_BOOM
        list += SHIP
        list += SHIP_BOOM
        list += BULLET
        list += BOMB
        for (c in FONT_CHARS) list += glyph(c)!!
        return list
    }
}
