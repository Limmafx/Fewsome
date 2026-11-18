package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "french_numbers_prefs")

enum class ThemePreference(val labelRu: String) {
    SYSTEM("Системная"),
    LIGHT("Светлая"),
    DARK("Тёмная")
}

data class UserPreferences(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val diagnosticPromptDismissed: Boolean = false,
    val voiceExercisesEnabled: Boolean = true,
    val varyVoices: Boolean = true,
    val speedTargetMs: Long = 2800L,
    val pressureFreeSpeed: Boolean = true,
    val currentStageNumber: Int = 2,
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val customMin: Int = 0,
    val customMax: Int = 100
)

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme_pref")
        val DIAGNOSTIC_DISMISSED = booleanPreferencesKey("diagnostic_dismissed")
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val VARY_VOICES = booleanPreferencesKey("vary_voices")
        val SPEED_TARGET_MS = longPreferencesKey("speed_target_ms")
        val PRESSURE_FREE = booleanPreferencesKey("pressure_free_speed")
        val STAGE_NUMBER = intPreferencesKey("stage_number")
        val CURRENT_STREAK = intPreferencesKey("current_streak")
        val BEST_STREAK = intPreferencesKey("best_streak")
        val CUSTOM_MIN = intPreferencesKey("custom_min")
        val CUSTOM_MAX = intPreferencesKey("custom_max")
    }

    val preferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val themeName = prefs[Keys.THEME] ?: ThemePreference.SYSTEM.name
        val theme = runCatching { ThemePreference.valueOf(themeName) }.getOrDefault(ThemePreference.SYSTEM)
        UserPreferences(
            themePreference = theme,
            diagnosticPromptDismissed = prefs[Keys.DIAGNOSTIC_DISMISSED] ?: false,
            voiceExercisesEnabled = prefs[Keys.VOICE_ENABLED] ?: true,
            varyVoices = prefs[Keys.VARY_VOICES] ?: true,
            speedTargetMs = prefs[Keys.SPEED_TARGET_MS] ?: 2800L,
            pressureFreeSpeed = prefs[Keys.PRESSURE_FREE] ?: true,
            currentStageNumber = prefs[Keys.STAGE_NUMBER] ?: 2,
            currentStreak = prefs[Keys.CURRENT_STREAK] ?: 0,
            bestStreak = prefs[Keys.BEST_STREAK] ?: 0,
            customMin = prefs[Keys.CUSTOM_MIN] ?: 0,
            customMax = prefs[Keys.CUSTOM_MAX] ?: 100
        )
    }

    suspend fun setTheme(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME] = theme.name }
    }

    suspend fun setDiagnosticDismissed(dismissed: Boolean) {
        context.dataStore.edit { it[Keys.DIAGNOSTIC_DISMISSED] = dismissed }
    }

    suspend fun setVoiceExercisesEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.VOICE_ENABLED] = enabled }
    }

    suspend fun setVaryVoices(vary: Boolean) {
        context.dataStore.edit { it[Keys.VARY_VOICES] = vary }
    }

    suspend fun setSpeedSettings(targetMs: Long, pressureFree: Boolean) {
        context.dataStore.edit {
            it[Keys.SPEED_TARGET_MS] = targetMs.coerceIn(1500L, 10000L)
            it[Keys.PRESSURE_FREE] = pressureFree
        }
    }

    suspend fun setStageNumber(stage: Int) {
        context.dataStore.edit { it[Keys.STAGE_NUMBER] = stage.coerceIn(1, 6) }
    }

    suspend fun setCustomRange(min: Int, max: Int) {
        val lo = min.coerceIn(0, 9999)
        val hi = max.coerceIn(lo, 9999)
        context.dataStore.edit {
            it[Keys.CUSTOM_MIN] = lo
            it[Keys.CUSTOM_MAX] = hi
        }
    }

    suspend fun updateStreak(isCorrect: Boolean) {
        context.dataStore.edit { prefs ->
            val current = if (isCorrect) (prefs[Keys.CURRENT_STREAK] ?: 0) + 1 else 0
            val best = maxOf(prefs[Keys.BEST_STREAK] ?: 0, current)
            prefs[Keys.CURRENT_STREAK] = current
            prefs[Keys.BEST_STREAK] = best
        }
    }

    suspend fun resetAll() {
        context.dataStore.edit { it.clear() }
    }
}
