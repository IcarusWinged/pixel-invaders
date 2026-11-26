package com.icarus.pixelinvaders.render

enum class ThemeMode {
    PHOSPHOR, AMBER, PAPER;

    val palette: Palette
        get() = when (this) {
            PHOSPHOR -> Palette.PHOSPHOR
            AMBER -> Palette.AMBER
            PAPER -> Palette.PAPER
        }

    companion object {
        fun fromOrdinal(i: Int): ThemeMode = values().getOrElse(i) { PHOSPHOR }
    }
}

/** One flat colour per role. Sprites are tinted with these, so the sheet stays monochrome. */
class Palette(
    val bg: Int,
    val alienTop: Int,
    val alienMid: Int,
    val alienLow: Int,
    val ship: Int,
    val ufo: Int,
    val shield: Int,
    val bomb: Int,
    val bullet: Int,
    val blast: Int,
    val text: Int,
    val ground: Int,
    val buttonBg: Int,
    val buttonText: Int,
) {
    companion object {
        val PHOSPHOR = Palette(
            bg = 0xFF050A06.toInt(),
            alienTop = 0xFF9BFF9B.toInt(),
            alienMid = 0xFF6BE36B.toInt(),
            alienLow = 0xFF3FBF4F.toInt(),
            ship = 0xFFE8FFE8.toInt(),
            ufo = 0xFFFF6B6B.toInt(),
            shield = 0xFF3FBF4F.toInt(),
            bomb = 0xFFD4FFD4.toInt(),
            bullet = 0xFFFFFFFF.toInt(),
            blast = 0xFFFFFFFF.toInt(),
            text = 0xFF9BFF9B.toInt(),
            ground = 0xFF2E8B3E.toInt(),
            buttonBg = 0xFF0E1F10.toInt(),
            buttonText = 0xFF9BFF9B.toInt(),
        )
        val AMBER = Palette(
            bg = 0xFF0D0802.toInt(),
            alienTop = 0xFFFFD27A.toInt(),
            alienMid = 0xFFFFB347.toInt(),
            alienLow = 0xFFE8892B.toInt(),
            ship = 0xFFFFF1D6.toInt(),
            ufo = 0xFFFF6B3D.toInt(),
            shield = 0xFFE8892B.toInt(),
            bomb = 0xFFFFE2B0.toInt(),
            bullet = 0xFFFFFFFF.toInt(),
            blast = 0xFFFFF1D6.toInt(),
            text = 0xFFFFB347.toInt(),
            ground = 0xFF9A5A18.toInt(),
            buttonBg = 0xFF1E1306.toInt(),
            buttonText = 0xFFFFB347.toInt(),
        )
        val PAPER = Palette(
            bg = 0xFFF4F1EA.toInt(),
            alienTop = 0xFF2B2B2B.toInt(),
            alienMid = 0xFF1F3A5F.toInt(),
            alienLow = 0xFF5C2E2E.toInt(),
            ship = 0xFFE8553D.toInt(),
            ufo = 0xFFB8860B.toInt(),
            shield = 0xFF2B2B2B.toInt(),
            bomb = 0xFFE8553D.toInt(),
            bullet = 0xFF2B2B2B.toInt(),
            blast = 0xFFE8553D.toInt(),
            text = 0xFF2B2B2B.toInt(),
            ground = 0xFF2B2B2B.toInt(),
            buttonBg = 0xFFE3DED2.toInt(),
            buttonText = 0xFF2B2B2B.toInt(),
        )
    }
}
