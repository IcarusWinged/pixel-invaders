package com.icarus.pixelinvaders.game

import android.content.Context
import com.icarus.pixelinvaders.data.SettingsStore

/**
 * Process-wide holder for the single running game, shared by the widget (static redraws) and the
 * loop service (animation). All access is serialized on one lock.
 */
object GameSession {
    private val lock = Any()
    private var engine: GameEngine? = null

    private fun engine(context: Context): GameEngine {
        val existing = engine
        if (existing != null) return existing
        val store = SettingsStore(context)
        return GameEngine(store.difficulty, store.best).also { engine = it }
    }

    fun moveLeft(context: Context) = synchronized(lock) { engine(context).moveLeft() }

    fun moveRight(context: Context) = synchronized(lock) { engine(context).moveRight() }

    fun fire(context: Context) = synchronized(lock) { engine(context).fire() }

    /** Advances the game and persists a new best score. Returns the current phase. */
    fun step(context: Context, dt: Float): Phase = synchronized(lock) {
        val e = engine(context)
        e.step(dt)
        val store = SettingsStore(context)
        if (e.state.best > store.best) store.best = e.state.best
        e.state.phase
    }

    /** Applies the saved difficulty (resets to the ready screen if it changed). */
    fun applySettings(context: Context) = synchronized(lock) {
        val e = engine(context)
        val wanted = SettingsStore(context).difficulty
        if (e.difficulty != wanted) e.configure(wanted)
    }

    /** Runs [block] with a consistent view of the state (used by the renderer). */
    fun <T> read(context: Context, block: (GameState) -> T): T = synchronized(lock) {
        block(engine(context).state)
    }
}
