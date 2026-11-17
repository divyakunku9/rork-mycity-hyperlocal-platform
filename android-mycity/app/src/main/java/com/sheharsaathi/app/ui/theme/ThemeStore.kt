package com.sheharsaathi.app.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** How the app decides between the light and dark themes. */
enum class ThemeChoice(val label: String, val detail: String) {
    LIGHT("Light", "Always light, whatever the phone is set to"),
    DARK("Dark", "Always dark, whatever the phone is set to"),
    SYSTEM("Follow phone", "Light or dark to match your phone");

    fun isDark(systemIsDark: Boolean): Boolean = when (this) {
        LIGHT -> false
        DARK -> true
        SYSTEM -> systemIsDark
    }
}

/**
 * Remembers the appearance the person chose in Profile.
 *
 * It belongs to the phone, not the account: it is kept apart from the sign-in details, so it
 * survives signing out and does not follow anyone to another device. The app has always been
 * dark, so that stays the default until someone chooses otherwise.
 */
object ThemeStore {
    private const val FILE = "mycity_settings"
    private const val KEY = "theme"

    private var prefs: SharedPreferences? = null

    var choice by mutableStateOf(ThemeChoice.DARK)
        private set

    /**
     * Read the saved choice. Called before the first screen is drawn; AppTheme then settles the
     * palette as it composes, so the first frame is already right and nothing flickers.
     */
    fun load(context: Context) {
        val p = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        prefs = p
        choice = runCatching { ThemeChoice.valueOf(p.getString(KEY, null) ?: "") }.getOrDefault(ThemeChoice.DARK)
    }

    fun set(value: ThemeChoice) {
        choice = value
        prefs?.edit()?.putString(KEY, value.name)?.apply()
    }
}
