package com.icarus.pixelinvaders.render

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Keeps Sprites.kt and the generated PNG (tools/make_assets.py) in lockstep. */
class SpriteSheetTest {

    private val image = PngReader.read(File("src/main/res/drawable-nodpi/spritesheet.png"))

    private fun opaque(x: Int, y: Int) = image.alpha(x, y) != 0

    @Test
    fun sheetHasExpectedSize() {
        assertNotNull(image)
        assertEquals(Sprites.SHEET_W, image.width)
        assertEquals(Sprites.SHEET_H, image.height)
    }

    @Test
    fun everySpriteRectIsInsideTheSheetAndHoldsPixels() {
        for (s in Sprites.all()) {
            assertTrue("outside sheet: $s", s.x >= 0 && s.y >= 0 && s.x + s.w <= image.width && s.y + s.h <= image.height)
            var count = 0
            for (y in s.y until s.y + s.h) for (x in s.x until s.x + s.w) if (opaque(x, y)) count++
            assertTrue("empty sprite: $s", count > 0)
        }
    }

    @Test
    fun noStrayPixelsOutsideSpriteRects() {
        val covered = Array(image.height) { BooleanArray(image.width) }
        for (s in Sprites.all()) for (y in s.y until s.y + s.h) for (x in s.x until s.x + s.w) covered[y][x] = true
        for (y in 0 until image.height) for (x in 0 until image.width) {
            if (opaque(x, y)) assertTrue("stray pixel at $x,$y", covered[y][x])
        }
    }

    @Test
    fun spritesDoNotOverlapEachOther() {
        val seen = Array(image.height) { BooleanArray(image.width) }
        for (s in Sprites.all()) for (y in s.y until s.y + s.h) for (x in s.x until s.x + s.w) {
            assertTrue("overlap at $x,$y in $s", !seen[y][x])
            seen[y][x] = true
        }
    }

    @Test
    fun spritePixelsAreWhiteSoTheyTintCleanly() {
        for (y in 0 until image.height) for (x in 0 until image.width) {
            if (opaque(x, y)) assertEquals(0xFFFFFF, image.rgb(x, y))
        }
    }

    @Test
    fun fontCoversDigitsAndLettersButNotSpace() {
        for (c in "SCORE 0123456789") {
            if (c == ' ') assertEquals(null, Sprites.glyph(c)) else assertNotNull("glyph $c", Sprites.glyph(c))
        }
        assertNotNull(Sprites.glyph('g')) // lower case maps to upper
    }
}
