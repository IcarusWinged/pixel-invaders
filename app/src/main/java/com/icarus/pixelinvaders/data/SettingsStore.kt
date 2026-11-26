package com.icarus.pixelinvaders.data

import android.content.Context
import android.content.SharedPreferences
import com.icarus.pixelinvaders.game.Difficulty
import com.icarus.pixelinvaders.render.ThemeMode

/** Persists user settings and the best score in SharedPreferences. */
class SettingsStore(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var difficulty: Difficulty
        get() = Difficulty.fromOrdinal(prefs.getInt(KEY_DIFFICULTY, Difficulty.NORMAL.ordinal))
        set(value) = prefs.edit().putInt(KEY_DIFFICULTY, value.ordinal).apply()

    var theme: ThemeMode
        get() = ThemeMode.fromOrdinal(prefs.getInt(KEY_THEME, ThemeMode.PHOSPHOR.ordinal))
        set(value) = prefs.edit().putInt(KEY_THEME, value.ordinal).apply()

    var best: Int
        get() = prefs.getInt(KEY_BEST, 0)
        set(value) = prefs.edit().putInt(KEY_BEST, value).apply()

    private companion object {
        const val PREFS = "pixel_invaders"
        const val KEY_DIFFICULTY = "difficulty"
        const val KEY_THEME = "theme"
        const val KEY_BEST = "best"
    }
}
