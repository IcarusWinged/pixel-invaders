package com.icarus.pixelinvaders.game

/** Player-selectable difficulty. */
enum class Difficulty(
    val label: String,
    val lives: Int,
    val alienSpeed: Float,
    val fireInterval: Float,
    val bombSpeed: Float,
) {
    EASY("Easy", lives = 5, alienSpeed = 0.8f, fireInterval = 1.3f, bombSpeed = 48f),
    NORMAL("Normal", lives = 3, alienSpeed = 1.0f, fireInterval = 1.0f, bombSpeed = 58f),
    HARD("Hard", lives = 2, alienSpeed = 1.25f, fireInterval = 0.75f, bombSpeed = 70f);

    companion object {
        fun fromOrdinal(i: Int): Difficulty = values().getOrElse(i) { NORMAL }
    }
}

/**
 * Fixed layout and tuning constants. The playfield is a 160x144 logical pixel grid (y grows
 * downward); the renderer scales it by an integer factor so pixels stay crisp.
 */
object World {
    const val WIDTH = 160f
    const val HEIGHT = 144f
    const val HUD_H = 10f
    const val GROUND_Y = 136f

    // Player ship
    const val SHIP_W = 13f
    const val SHIP_H = 8f
    const val SHIP_Y = 124f
    const val SHIP_MIN_X = 2f
    const val SHIP_MAX_X = WIDTH - 2f - SHIP_W
    const val SHIP_START_X = (WIDTH - SHIP_W) / 2f
    const val SHIP_SPEED = 120f
    const val MOVE_STEP = 24f
    const val SHIP_BOOM_TIME = 1.3f
    const val RESPAWN_INVULN = 1.6f

    // Alien formation
    const val COLS = 8
    const val ROWS = 4
    const val ALIEN_W = 11f
    const val ALIEN_H = 8f
    const val PITCH_X = 16f
    const val PITCH_Y = 12f
    const val FORM_LEFT = 10f
    const val FORM_TOP = 22f
    const val MARGIN = 3f
    const val DROP = 6f
    const val BASE_SPEED = 10f
    const val SPEED_RAMP = 1.8f
    const val FRAME_DISTANCE = 6f

    // Shields (pixel-destructible bunkers)
    const val SHIELD_W = 22
    const val SHIELD_H = 16
    const val SHIELD_Y = 102
    const val SHIELD_COUNT = 4
    const val SHIELD_X0 = 15
    const val SHIELD_PITCH = 36

    // Projectiles
    const val BULLET_W = 1f
    const val BULLET_H = 5f
    const val BULLET_SPEED = 170f
    const val BOMB_W = 3f
    const val BOMB_H = 6f
    const val MAX_BOMBS = 3

    // UFO
    const val UFO_W = 16f
    const val UFO_H = 7f
    const val UFO_Y = 12f

    // Timing
    const val STEP = 1f / 60f
    const val MAX_FRAME = 0.1f
    const val RESTART_DELAY = 0.7f
    const val WAVE_PAUSE = 1.6f
    const val ALIEN_BLAST_LIFE = 0.3f
    const val UFO_BLAST_LIFE = 0.5f
}
