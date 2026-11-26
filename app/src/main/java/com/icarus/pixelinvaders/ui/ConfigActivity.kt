package com.icarus.pixelinvaders.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import com.icarus.pixelinvaders.R
import com.icarus.pixelinvaders.data.SettingsStore
import com.icarus.pixelinvaders.game.Difficulty
import com.icarus.pixelinvaders.game.GameSession
import com.icarus.pixelinvaders.render.ThemeMode
import com.icarus.pixelinvaders.widget.WidgetUpdater

/**
 * Settings screen. Works both as the widget's configure screen (returns RESULT_OK with the widget
 * id) and as a normal launcher activity.
 */
class ConfigActivity : Activity() {

    private lateinit var store: SettingsStore
    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Cancel by default so backing out does not add the widget.
        setResult(RESULT_CANCELED)
        setContentView(R.layout.activity_config)

        store = SettingsStore(this)
        widgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID,
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        val difficulty = findViewById<RadioGroup>(R.id.group_difficulty)
        val theme = findViewById<RadioGroup>(R.id.group_theme)
        difficulty.check(
            when (store.difficulty) {
                Difficulty.EASY -> R.id.difficulty_easy
                Difficulty.NORMAL -> R.id.difficulty_normal
                Difficulty.HARD -> R.id.difficulty_hard
            },
        )
        theme.check(
            when (store.theme) {
                ThemeMode.PHOSPHOR -> R.id.theme_phosphor
                ThemeMode.AMBER -> R.id.theme_amber
                ThemeMode.PAPER -> R.id.theme_paper
            },
        )

        findViewById<TextView>(R.id.text_version).text = getString(R.string.version_format, versionName())
        findViewById<Button>(R.id.button_download).setOnClickListener { open(AppLinks.APK_URL) }
        findViewById<Button>(R.id.button_releases).setOnClickListener { open(AppLinks.RELEASES_URL) }

        findViewById<Button>(R.id.button_save).setOnClickListener {
            store.difficulty = when (difficulty.checkedRadioButtonId) {
                R.id.difficulty_easy -> Difficulty.EASY
                R.id.difficulty_hard -> Difficulty.HARD
                else -> Difficulty.NORMAL
            }
            store.theme = when (theme.checkedRadioButtonId) {
                R.id.theme_amber -> ThemeMode.AMBER
                R.id.theme_paper -> ThemeMode.PAPER
                else -> ThemeMode.PHOSPHOR
            }

            GameSession.applySettings(this)
            WidgetUpdater.updateAll(this)

            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                setResult(RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
            }
            finish()
        }
    }

    private fun versionName(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: "?"
    } catch (e: Exception) {
        "?"
    }

    /** Opens a link in the browser; a .apk link downloads the file. */
    private fun open(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, R.string.no_browser, Toast.LENGTH_LONG).show()
        }
    }
}
