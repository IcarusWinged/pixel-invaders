package com.icarus.pixelinvaders.game

import kotlin.math.abs
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameEngineTest {

    private fun engine(d: Difficulty = Difficulty.NORMAL, seed: Int = 7) = GameEngine(d, 0, Random(seed))

    private fun GameEngine.advance(seconds: Float) {
        var t = 0f
        while (t < seconds) {
            step(World.STEP)
            t += World.STEP
        }
    }

    /** Starts the game and stops alien bombs/UFO from interfering with a scenario. */
    private fun GameEngine.running(): GameEngine {
        fire()
        state.fireTimer = 1e9f
        state.ufoTimer = 1e9f
        return this
    }

    private fun GameEngine.centreShipOn(x: Float) {
        state.shipX = x - World.SHIP_W / 2f
        state.shipTarget = state.shipX
    }

    // --- Flow ----------------------------------------------------------------------------------

    @Test
    fun startsReadyWithFullFormationThenFireStartsPlay() {
        val e = engine()
        assertEquals(Phase.READY, e.state.phase)
        assertEquals(World.COLS * World.ROWS, e.state.aliveCount())
        assertEquals(Difficulty.NORMAL.lives, e.state.lives)
        e.fire()
        assertEquals(Phase.RUNNING, e.state.phase)
    }

    @Test
    fun shipGlidesToTargetAndStaysInBounds() {
        val e = engine().running()
        repeat(20) { e.moveLeft() }
        e.advance(0.2f)
        assertTrue("ship should still be gliding", e.state.shipX > World.SHIP_MIN_X + 1f)
        e.advance(3f)
        assertTrue(abs(e.state.shipX - World.SHIP_MIN_X) < 0.5f)
        repeat(40) { e.moveRight() }
        e.advance(4f)
        assertTrue(abs(e.state.shipX - World.SHIP_MAX_X) < 0.5f)
    }

    @Test
    fun physicsIsIndependentOfFrameRate() {
        val a = engine(seed = 3).running()
        val b = engine(seed = 3).running()
        repeat(120) { a.step(1f / 60f) }
        repeat(30) { b.step(1f / 15f) } // same 2 seconds, four ticks per call
        assertEquals(a.state.formX, b.state.formX, 0.01f)
        assertEquals(a.state.formY, b.state.formY, 0.01f)
    }

    // --- Player shots --------------------------------------------------------------------------

    @Test
    fun bulletKillsBottomAlienAndScoresTenPoints() {
        val e = engine().running()
        // Column 2 sits in the gap between bunkers, so the shot has a clear lane.
        val target = e.state.aliens.first { it.row == 3 && it.col == 2 }
        e.centreShipOn(e.state.alienX(target) + World.ALIEN_W / 2f)
        e.fire()
        assertNotNull(e.state.bullet)
        e.advance(0.8f)
        assertFalse(target.alive)
        assertEquals(31, e.state.aliveCount())
        assertEquals(10, e.state.score)
        assertNull(e.state.bullet)
    }

    @Test
    fun onlyOneBulletAtATime() {
        val e = engine().running()
        e.fire()
        val first = e.state.bullet
        e.fire()
        assertTrue(first === e.state.bullet)
    }

    @Test
    fun bunkerAbsorbsShotAndErodes() {
        val e = engine().running()
        val shield = e.state.shields[0]
        val before = shield.pixelCount
        e.centreShipOn(shield.x + 3f) // a solid leg of the bunker
        e.fire()
        e.advance(0.5f)
        assertTrue(shield.pixelCount < before)
        assertNull(e.state.bullet)
        assertEquals(32, e.state.aliveCount())
    }

    @Test
    fun bulletDestroysUfoForItsPoints() {
        val e = engine().running()
        e.state.aliens.forEach { it.alive = it.row == 0 && it.col == 0 } // clear the lane, keep one alive
        e.state.ufo = Ufo(x = 70f, dir = 1, speed = 0f, points = 100)
        e.state.bullet = Bullet(x = 75f, y = 60f)
        e.advance(0.6f)
        assertNull(e.state.ufo)
        assertEquals(100, e.state.score)
    }

    @Test
    fun playerBulletCancelsAlienBomb() {
        val e = engine().running()
        e.state.bullet = Bullet(x = 40f, y = 90f)
        e.state.bombs.add(Bomb(x = 40f, y = 70f))
        e.advance(0.3f)
        assertTrue(e.state.bombs.isEmpty())
        assertNull(e.state.bullet)
        assertEquals(Difficulty.NORMAL.lives, e.state.lives)
    }

    @Test
    fun bulletVanishesAtTheTopOfTheScreen() {
        val e = engine().running()
        e.state.aliens.forEach { it.alive = it.row == 0 && it.col == 0 }
        e.centreShipOn(120f) // clear of everything above
        e.fire()
        e.advance(1.5f)
        assertNull(e.state.bullet)
    }

    // --- Alien bombs ---------------------------------------------------------------------------

    @Test
    fun bombErodesBunkerAndDisappears() {
        val e = engine().running()
        val shield = e.state.shields[1]
        val before = shield.pixelCount
        e.state.bombs.add(Bomb(x = (shield.x + 10).toFloat(), y = shield.y - 12f))
        e.advance(0.6f)
        assertTrue(e.state.bombs.isEmpty())
        assertTrue(shield.pixelCount < before)
        assertEquals(Difficulty.NORMAL.lives, e.state.lives)
    }

    @Test
    fun bombHitCostsALifeAndBlowsUpTheShip() {
        val e = engine().running()
        val x = e.state.shipX + 5f
        e.state.bombs.add(Bomb(x = x, y = World.SHIP_Y - 12f))
        e.advance(0.5f)
        assertEquals(Difficulty.NORMAL.lives - 1, e.state.lives)
        assertTrue(e.state.shipTimer > 0f)
        assertTrue(e.state.bombs.isEmpty())
    }

    @Test
    fun shipRespawnsWithBriefInvulnerability() {
        val e = engine().running()
        e.state.bombs.add(Bomb(x = e.state.shipX + 5f, y = World.SHIP_Y - 12f))
        e.advance(0.5f)
        e.advance(World.SHIP_BOOM_TIME)
        assertEquals(Phase.RUNNING, e.state.phase)
        assertEquals(0f, e.state.shipTimer, 0f)
        assertTrue(e.state.invuln > 0f)
        // A bomb dropped straight onto the invulnerable ship does nothing.
        val lives = e.state.lives
        e.state.bombs.add(Bomb(x = e.state.shipX + 5f, y = World.SHIP_Y - 12f))
        e.advance(0.4f)
        assertEquals(lives, e.state.lives)
    }

    @Test
    fun losingLastLifeEndsGameAndRestartNeedsADelay() {
        val e = engine().running()
        e.state.lives = 1
        e.state.bombs.add(Bomb(x = e.state.shipX + 5f, y = World.SHIP_Y - 12f))
        e.advance(0.5f)
        e.advance(World.SHIP_BOOM_TIME + 0.1f)
        assertEquals(Phase.OVER, e.state.phase)
        e.fire() // too soon: must not instantly restart
        assertEquals(Phase.OVER, e.state.phase)
        e.advance(World.RESTART_DELAY + 0.1f)
        e.fire()
        assertEquals(Phase.READY, e.state.phase)
        assertEquals(0, e.state.score)
        assertEquals(Difficulty.NORMAL.lives, e.state.lives)
    }

    @Test
    fun aliensAreCappedAtThreeBombsAtOnce() {
        val e = engine(Difficulty.HARD)
        e.fire()
        e.state.ufoTimer = 1e9f
        repeat(600) {
            e.step(World.STEP)
            assertTrue(e.state.bombs.size <= World.MAX_BOMBS)
        }
    }

    // --- Formation -----------------------------------------------------------------------------

    @Test
    fun formationBouncesOffTheWallsAndDropsEachTime() {
        val e = engine().running()
        val startY = e.state.formY
        var drops = 0
        var lastY = startY
        var minLeft = Float.MAX_VALUE
        var maxRight = Float.MIN_VALUE
        repeat(60 * 12) {
            e.step(World.STEP)
            if (e.state.phase != Phase.RUNNING) return@repeat
            if (e.state.formY > lastY) { drops++; lastY = e.state.formY }
            minLeft = minOf(minLeft, e.state.formX)
            maxRight = maxOf(maxRight, e.state.formX + 7 * World.PITCH_X + World.ALIEN_W)
        }
        assertTrue("should have bounced at least twice, got $drops", drops >= 2)
        assertEquals(startY + drops * World.DROP, e.state.formY, 0.001f)
        assertTrue(minLeft >= World.MARGIN - 0.01f)
        assertTrue(maxRight <= World.WIDTH - World.MARGIN + 0.01f)
    }

    @Test
    fun formationSpeedsUpAsAliensDie() {
        val full = engine().running()
        val thin = engine().running()
        thin.state.aliens.forEach { if (!(it.row == 0 && it.col < 2)) it.alive = false }
        full.step(0.05f)
        thin.step(0.05f)
        assertTrue(thin.state.formX - World.FORM_LEFT > full.state.formX - World.FORM_LEFT)
    }

    @Test
    fun aliensReachingTheShipLineEndsTheGame() {
        val e = engine().running()
        e.state.formY = World.SHIP_Y - 3 * World.PITCH_Y - World.ALIEN_H + 1f
        e.advance(0.1f)
        assertEquals(Phase.OVER, e.state.phase)
    }

    @Test
    fun marchingAliensGrindDownBunkers() {
        val e = engine().running()
        val shield = e.state.shields[0]
        val before = shield.pixelCount
        e.state.formY = World.SHIELD_Y - 2 * World.PITCH_Y.toFloat() // bottom row overlaps the bunker height
        e.advance(0.1f)
        assertTrue(shield.pixelCount < before)
    }

    // --- Waves ---------------------------------------------------------------------------------

    @Test
    fun clearingTheWaveStartsTheNextOneWithRestoredBunkers() {
        val e = engine().running()
        e.state.shields[0].erode(e.state.shields[0].x + 10, e.state.shields[0].y + 5, 6f, Random(1))
        e.state.aliens.forEach { it.alive = false }
        e.advance(World.WAVE_PAUSE + 0.5f)
        assertEquals(2, e.state.wave)
        assertEquals(World.COLS * World.ROWS, e.state.aliveCount())
        val fresh = Shield(0, 0).pixelCount
        assertEquals(fresh, e.state.shields[0].pixelCount)
        assertEquals(Phase.RUNNING, e.state.phase)
    }

    @Test
    fun scoreKeepsBestAcrossGames() {
        val e = GameEngine(Difficulty.NORMAL, best = 500, rng = Random(1))
        assertEquals(500, e.state.best)
        e.configure(Difficulty.HARD)
        assertEquals(500, e.state.best)
        assertEquals(Difficulty.HARD.lives, e.state.lives)
    }

    // --- Whole-game robustness -----------------------------------------------------------------

    @Test
    fun botPlaysManyGamesWithoutBreakingInvariants() {
        for (seed in 1..5) {
            val e = engine(Difficulty.EASY, seed)
            var scored = 0
            e.fire()
            repeat(60 * 90) { tickNo ->
                val s = e.state
                if (s.phase == Phase.RUNNING && tickNo % 6 == 0) {
                    val target = s.aliens.filter { it.alive }.maxByOrNull { s.alienY(it) * 100 - abs(s.alienX(it) - s.shipX) }
                    if (target != null) {
                        s.shipTarget = (s.alienX(target) + World.ALIEN_W / 2f - World.SHIP_W / 2f)
                            .coerceIn(World.SHIP_MIN_X, World.SHIP_MAX_X)
                    }
                    e.fire()
                }
                if (s.phase == Phase.OVER && s.overTime > World.RESTART_DELAY) e.fire()
                if (s.phase == Phase.READY) e.fire()
                e.step(World.STEP)

                assertTrue(s.shipX in World.SHIP_MIN_X..World.SHIP_MAX_X)
                assertTrue(s.bombs.size <= World.MAX_BOMBS)
                assertTrue(s.lives >= 0)
                assertTrue(s.score >= 0 && s.best >= s.score)
                assertTrue(s.aliens.none { it.alive && (s.alienX(it).isNaN() || s.alienY(it).isNaN()) })
                s.bullet?.let { assertTrue(it.y in 0f..World.HEIGHT) }
                scored = maxOf(scored, s.score)
            }
            assertTrue("bot never scored on seed $seed", e.state.best > 0)
        }
    }
}
