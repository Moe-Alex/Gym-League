package com.gymleague.app

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferences {

    private const val PREF_NAME =
        "gymleague_preferences"

    private const val KEY_APPEARANCE_MODE =
        "appearance_mode"

    fun saveAppearanceMode(
        context: Context,
        mode: String
    ) {

        context
            .getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )
            .edit()
            .putString(
                KEY_APPEARANCE_MODE,
                mode
            )
            .apply()
    }

    fun getAppearanceMode(
        context: Context
    ): String {

        return context
            .getSharedPreferences(
                PREF_NAME,
                Context.MODE_PRIVATE
            )
            .getString(
                KEY_APPEARANCE_MODE,
                "system"
            )
            ?: "system"
    }

    fun applySavedTheme(
        context: Context
    ) {

        val mode =
            getAppearanceMode(context)

        applyTheme(mode)
    }

    fun applyTheme(
        mode: String
    ) {

        when (
            mode.lowercase()
        ) {

            "light" -> {

                AppCompatDelegate
                    .setDefaultNightMode(
                        AppCompatDelegate
                            .MODE_NIGHT_NO
                    )
            }

            "dark" -> {

                AppCompatDelegate
                    .setDefaultNightMode(
                        AppCompatDelegate
                            .MODE_NIGHT_YES
                    )
            }

            else -> {

                AppCompatDelegate
                    .setDefaultNightMode(
                        AppCompatDelegate
                            .MODE_NIGHT_FOLLOW_SYSTEM
                    )
            }
        }
    }
}