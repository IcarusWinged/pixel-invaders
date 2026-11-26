package com.icarus.pixelinvaders.game

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.random.Random

/**
 * Pure game logic with no Android dependencies, so it runs in JVM unit tests.
 *
 * Time advances in fixed 1/60 s ticks regardless of how often [step] is called, so physics and
 * collisions behave identically at 15 fps or 60 fps. Every moving projectile is swept along its
 * path each tick, which means nothing can tunnel through a target.
 */
class GameEngine(
    difficulty: Difficulty = Difficulty.NORMAL,
    best: Int = 0,
    private val rng: Random = Random.Default,
) {
    val state = GameState()

    var difficulty: Difficulty = difficulty
        private set

    private var accumulator = 0f

    init {
        state.best = best
        newGame()
    }

    /** Changes difficulty and returns to the ready screen (best score is kept). */
    fun configure(newDifficulty: Difficulty) {
        difficulty = newDifficulty
        newGame()
    }

    // --- Input -------------------------------------------------------------------------------

    fun moveLeft() = nudge(-World.MOVE_STEP)

    fun moveRight() = nudge(World.MOVE_STEP)

    private fun nudge(dx: Float) {
        val s = state
        if (s.phase == Phase.OVER) return
        s.shipTarget = (s.shipTarget + dx).coerceIn(World.SHIP_MIN_X, World.SHIP_MAX_X)
    }

    /** Start, shoot, or restart depending on the phase. */
    fun fire() {
        val s = state
        when (s.phase) {
            Phase.READY -> s.phase = Phase.RUNNING
            Phase.RUNNING -> shoot()
            Phase.OVER -> if (s.overTime >= World.RESTART_DELAY) newGame()
        }
    }

    private fun shoot() {
        val s = state
        if (s.shipTimer > 0f || s.wavePause > 0f || s.bullet != null) return
        s.bullet = Bullet(floor(s.shipX + World.SHIP_W / 2f), World.SHIP_Y - World.BULLET_H)
    }

    // --- Time --------------------------------------------------------------------------------

    fun step(dt: Float) {
        accumulator += dt.coerceIn(0f, World.MAX_FRAME)
        while (accumulator >= World.STEP) {
            tick(World.STEP)
            accumulator -= World.STEP
        }
    }

    private fun tick(dt: Float) {
        val s = state
        s.time += dt
        updateBlasts(dt)
        when (s.phase) {
            Phase.READY -> easeShip(dt)
            Phase.OVER -> s.overTime += dt
            Phase.RUNNING -> tickRunning(dt)
        }
    }

    private fun tickRunning(dt: Float) {
        val s = state

        if (s.shipTimer > 0f) {
            s.shipTimer = max(0f, s.shipTimer - dt)
            if (s.shipTimer == 0f) {
                if (s.lives <= 0) endGame() else respawn()
            }
            return
        }

        easeShip(dt)
        if (s.invuln > 0f) s.invuln -= dt

        if (s.wavePause > 0f) {
            s.wavePause -= dt
            if (s.wavePause <= 0f) {
                s.wave++
                setupWave(resetShields = true)
            }
            return
        }

        marchAliens(dt)
        if (state.phase != Phase.RUNNING) return
        moveBullet(dt)
        moveBombs(dt)
        moveUfo(dt)
        alienFire(dt)

        if (s.aliveCount() == 0 && s.wavePause <= 0f) {
            s.wavePause = World.WAVE_PAUSE
            s.bombs.clear()
            s.bullet = null
            s.ufo = null
        }
    }

    // --- Ship --------------------------------------------------------------------------------

    private fun easeShip(dt: Float) {
        val s = state
        val v = ((s.shipTarget - s.shipX) * 12f).coerceIn(-World.SHIP_SPEED, World.SHIP_SPEED)
        s.shipX = (s.shipX + v * dt).coerceIn(World.SHIP_MIN_X, World.SHIP_MAX_X)
    }

    private fun hitShip() {
        val s = state
        s.lives--
        s.shipTimer = World.SHIP_BOOM_TIME
        s.invuln = 0f
        s.bullet = null
        s.bombs.clear()
    }

    private fun respawn() {
        val s = state
        s.shipX = World.SHIP_START_X
        s.shipTarget = World.SHIP_START_X
        s.invuln = World.RESPAWN_INVULN
    }

    private fun endGame() {
        val s = state
        s.phase = Phase.OVER
        s.overTime = 0f
        s.bullet = null
        s.bombs.clear()
    }

    // --- Aliens ------------------------------------------------------------------------------

    private fun marchAliens(dt: Float) {
        val s = state
        var minCol = World.COLS
        var maxCol = -1
        var alive = 0
        for (a in s.aliens) {
            if (!a.alive) continue
            alive++
            if (a.col < minCol) minCol = a.col
            if (a.col > maxCol) maxCol = a.col
        }
        if (alive == 0) return

        val total = World.COLS * World.ROWS
        val tempo = World.BASE_SPEED + (total - alive) * World.SPEED_RAMP
        val waveBoost = (1f + 0.1f * (s.wave - 1)).coerceAtMost(2f)
        val speed = tempo * waveBoost * difficulty.alienSpeed

        var dx = s.formDir * speed * dt
        val left = s.formX + minCol * World.PITCH_X
        val right = s.formX + maxCol * World.PITCH_X + World.ALIEN_W
        if (s.formDir > 0 && right + dx >= World.WIDTH - World.MARGIN) {
            dx = World.WIDTH - World.MARGIN - right
            s.formDir = -1
            s.formY += World.DROP
        } else if (s.formDir < 0 && left + dx <= World.MARGIN) {
            dx = World.MARGIN - left
            s.formDir = 1
            s.formY += World.DROP
        }
        s.formX += dx
        s.animDist += abs(dx)

        // Aliens grind bunkers away as they pass over them, and win if they reach the ship line.
        var landed = false
        for (a in s.aliens) {
            if (!a.alive) continue
            val ax = s.alienX(a)
            val ay = s.alienY(a)
            if (ay + World.ALIEN_H >= World.SHIELD_Y) {
                for (sh in s.shields) sh.clearRect(ax, ay, World.ALIEN_W, World.ALIEN_H)
            }
            if (ay + World.ALIEN_H >= World.SHIP_Y) landed = true
        }
        if (landed) endGame()
    }

    private fun alienFire(dt: Float) {
        val s = state
        s.fireTimer -= dt
        if (s.fireTimer > 0f || s.bombs.size >= World.MAX_BOMBS) return

        // Lowest living alien of each column can shoot.
        val shooters = ArrayList<Alien>()
        for (col in 0 until World.COLS) {
            var lowest: Alien? = null
            for (a in s.aliens) {
                if (a.alive && a.col == col && (lowest == null || a.row > lowest.row)) lowest = a
            }
            if (lowest != null) shooters.add(lowest)
        }
        if (shooters.isEmpty()) return

        val shooter = if (rng.nextFloat() < 0.4f) {
            val shipCenter = s.shipX + World.SHIP_W / 2f
            shooters.minByOrNull { abs(s.alienX(it) + World.ALIEN_W / 2f - shipCenter) }!!
        } else {
            shooters[rng.nextInt(shooters.size)]
        }
        val bx = floor(s.alienX(shooter) + World.ALIEN_W / 2f - 1f)
        s.bombs.add(Bomb(bx, s.alienY(shooter) + World.ALIEN_H))

        val alive = s.aliveCount()
        val base = max(0.35f, 1.2f - 0.05f * (s.wave - 1) - (World.COLS * World.ROWS - alive) * 0.01f)
        s.fireTimer = base * difficulty.fireInterval * (0.7f + 0.6f * rng.nextFloat())
    }

    // --- UFO ---------------------------------------------------------------------------------

    private fun moveUfo(dt: Float) {
        val s = state
        s.ufoTimer -= dt
        val u = s.ufo
        if (u == null) {
            if (s.ufoTimer <= 0f && s.aliveCount() >= 6) {
                val dir = if (rng.nextBoolean()) 1 else -1
                val x = if (dir > 0) -World.UFO_W else World.WIDTH
                val points = intArrayOf(50, 100, 150)[rng.nextInt(3)]
                s.ufo = Ufo(x, dir, 32f + 2f * s.wave, points)
            }
            return
        }
        u.x += u.dir * u.speed * dt
        if (u.x > World.WIDTH || u.x + World.UFO_W < 0f) {
            s.ufo = null
            s.ufoTimer = nextUfoDelay()
        }
    }

    private fun nextUfoDelay(): Float = 15f + 15f * rng.nextFloat()

    // --- Projectiles -------------------------------------------------------------------------

    private enum class Target { NONE, SHIELD, ALIEN, UFO, BOMB }

    /**
     * Sweeps the player's bullet from its old to its new position and resolves the first thing it
     * would have touched (the contact with the largest y, since it travels upward).
     */
    private fun moveBullet(dt: Float) {
        val s = state
        val b = s.bullet ?: return
        val y0 = b.y
        val y1 = y0 - World.BULLET_SPEED * dt

        var contact = Float.NEGATIVE_INFINITY
        var kind = Target.NONE
        var hitAlien: Alien? = null
        var hitBomb: Bomb? = null
        var hitShield: Shield? = null
        var hitPixel: PixelHit? = null

        val col = b.x.toInt()
        for (sh in s.shields) {
            val p = sh.firstSolidUp(col, floor(y0).toInt(), floor(y1).toInt()) ?: continue
            val c = p.y + 1f
            if (c > contact) {
                contact = c; kind = Target.SHIELD; hitShield = sh; hitPixel = p
            }
        }
        for (a in s.aliens) {
            if (!a.alive) continue
            val ax = s.alienX(a)
            val ay = s.alienY(a)
            if (Collision.sweptVertical(b.x, World.BULLET_W, World.BULLET_H, y0, y1, ax, ay, World.ALIEN_W, World.ALIEN_H)) {
                val c = ay + World.ALIEN_H
                if (c > contact) {
                    contact = c; kind = Target.ALIEN; hitAlien = a
                }
            }
        }
        val u = s.ufo
        if (u != null &&
            Collision.sweptVertical(b.x, World.BULLET_W, World.BULLET_H, y0, y1, u.x, World.UFO_Y, World.UFO_W, World.UFO_H)
        ) {
            val c = World.UFO_Y + World.UFO_H
            if (c > contact) {
                contact = c; kind = Target.UFO
            }
        }
        for (bm in s.bombs) {
            if (Collision.sweptVertical(b.x, World.BULLET_W, World.BULLET_H, y0, y1, bm.x, bm.y, World.BOMB_W, World.BOMB_H)) {
                val c = bm.y + World.BOMB_H
                if (c > contact) {
                    contact = c; kind = Target.BOMB; hitBomb = bm
                }
            }
        }

        when (kind) {
            Target.SHIELD -> {
                val p = hitPixel!!
                hitShield!!.erode(p.x, p.y, 3f, rng)
                s.bullet = null
            }
            Target.ALIEN -> {
                val a = hitAlien!!
                a.alive = false
                addScore(a.points)
                s.blasts.add(Blast(BlastKind.ALIEN, s.alienX(a), s.alienY(a), World.ALIEN_BLAST_LIFE))
                s.bullet = null
            }
            Target.UFO -> {
                val ufo = s.ufo!!
                addScore(ufo.points)
                s.blasts.add(Blast(BlastKind.UFO, ufo.x, World.UFO_Y, World.UFO_BLAST_LIFE))
                s.ufo = null
                s.ufoTimer = nextUfoDelay()
                s.bullet = null
            }
            Target.BOMB -> {
                s.bombs.remove(hitBomb)
                s.bullet = null
            }
            Target.NONE -> {
                b.y = y1
                if (y1 <= World.HUD_H) s.bullet = null
            }
        }
    }

    private fun moveBombs(dt: Float) {
        val s = state
        val speed = difficulty.bombSpeed + 2f * (s.wave - 1)
        var shipHit = false
        val it = s.bombs.iterator()
        while (it.hasNext()) {
            val bm = it.next()
            val y0 = bm.y
            val y1 = y0 + speed * dt
            bm.anim += dt

            // Bunkers first: they sit above the ship, so they are always reached first.
            val fromRow = ceil(y0 + World.BOMB_H).toInt() - 1
            val toRow = ceil(y1 + World.BOMB_H).toInt() - 1
            var blocked = false
            for (sh in s.shields) {
                val p = sh.firstSolidDown(bm.x.toInt(), World.BOMB_W.toInt(), fromRow, toRow) ?: continue
                sh.erode(p.x, p.y, 3.5f, rng)
                blocked = true
                break
            }
            if (blocked) {
                it.remove()
                continue
            }

            if (s.invuln <= 0f &&
                Collision.sweptVertical(bm.x, World.BOMB_W, World.BOMB_H, y0, y1, s.shipX, World.SHIP_Y, World.SHIP_W, World.SHIP_H)
            ) {
                shipHit = true
                it.remove()
                continue
            }

            if (y1 + World.BOMB_H >= World.GROUND_Y) {
                it.remove()
                continue
            }
            bm.y = y1
        }
        if (shipHit) hitShip()
    }

    private fun updateBlasts(dt: Float) {
        val it = state.blasts.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.age += dt
            if (b.age >= b.life) it.remove()
        }
    }

    private fun addScore(points: Int) {
        val s = state
        s.score += points
        if (s.score > s.best) s.best = s.score
    }

    // --- Setup -------------------------------------------------------------------------------

    private fun newGame() {
        val s = state
        s.phase = Phase.READY
        s.score = 0
        s.lives = difficulty.lives
        s.wave = 1
        s.time = 0f
        s.overTime = 0f
        s.shipX = World.SHIP_START_X
        s.shipTarget = World.SHIP_START_X
        s.shipTimer = 0f
        s.invuln = 0f
        s.wavePause = 0f
        s.blasts.clear()
        accumulator = 0f
        setupWave(resetShields = true)
    }

    private fun setupWave(resetShields: Boolean) {
        val s = state
        s.aliens.clear()
        for (row in 0 until World.ROWS) for (col in 0 until World.COLS) s.aliens.add(Alien(row, col))
        s.formX = World.FORM_LEFT
        s.formY = World.FORM_TOP + minOf(s.wave - 1, 4) * 6f
        s.formDir = 1
        s.animDist = 0f
        s.bullet = null
        s.bombs.clear()
        s.ufo = null
        s.ufoTimer = 12f + 8f * rng.nextFloat()
        s.fireTimer = 1.0f
        if (resetShields) s.shields.forEach { it.reset() }
    }
}
