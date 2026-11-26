package com.icarus.pixelinvaders.game

enum class Phase { READY, RUNNING, OVER }

enum class BlastKind { ALIEN, UFO }

/** An alien's grid slot. Its position comes from the formation origin in [GameState]. */
class Alien(val row: Int, val col: Int, var alive: Boolean = true) {
    val points: Int get() = when (row) {
        0 -> 30
        1, 2 -> 20
        else -> 10
    }
}

/** The player's shot. [y] is the top edge. */
class Bullet(val x: Float, var y: Float)

/** An alien bomb. [x] is the left edge, [y] the top edge. */
class Bomb(val x: Float, var y: Float, var anim: Float = 0f)

class Ufo(var x: Float, val dir: Int, val speed: Float, val points: Int)

/** A short-lived explosion marker, drawn by the renderer. */
class Blast(val kind: BlastKind, val x: Float, val y: Float, val life: Float, var age: Float = 0f)

/** Plain mutable state; only [GameEngine] changes it. */
class GameState {
    var phase = Phase.READY
    var score = 0
    var best = 0
    var lives = 3
    var wave = 1
    var time = 0f
    var overTime = 0f

    var shipX = World.SHIP_START_X
    var shipTarget = World.SHIP_START_X
    var shipTimer = 0f
    var invuln = 0f
    var wavePause = 0f

    val aliens = ArrayList<Alien>()
    var formX = World.FORM_LEFT
    var formY = World.FORM_TOP
    var formDir = 1
    var animDist = 0f

    var bullet: Bullet? = null
    val bombs = ArrayList<Bomb>()
    var ufo: Ufo? = null
    var ufoTimer = 15f
    var fireTimer = 1f
    val blasts = ArrayList<Blast>()

    val shields: List<Shield> = List(World.SHIELD_COUNT) {
        Shield(World.SHIELD_X0 + it * World.SHIELD_PITCH, World.SHIELD_Y)
    }

    fun aliveCount(): Int = aliens.count { it.alive }
    fun alienX(a: Alien): Float = formX + a.col * World.PITCH_X
    fun alienY(a: Alien): Float = formY + a.row * World.PITCH_Y

    /** 0 or 1, toggles as the formation marches. */
    fun animFrame(): Int = (animDist / World.FRAME_DISTANCE).toInt() and 1
}
