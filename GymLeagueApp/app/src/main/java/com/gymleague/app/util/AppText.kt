package com.gymleague.app.util

import android.content.res.Configuration
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import com.gymleague.app.GymLeagueApplication

/**
 * Looks up a translated string from code that has no Activity (ViewModels, repositories).
 * It uses the language the user picked in Settings, on every Android version, so the text
 * matches the rest of the screen.
 */
object AppText {

    fun get(@StringRes id: Int, vararg args: Any): String {
        val app = GymLeagueApplication.instance
        val chosen = AppCompatDelegate.getApplicationLocales()

        val context = if (!chosen.isEmpty && chosen[0] != null) {
            val config = Configuration(app.resources.configuration)
            config.setLocale(chosen[0])
            app.createConfigurationContext(config)
        } else {
            app
        }

        return context.getString(id, *args)
    }
}
