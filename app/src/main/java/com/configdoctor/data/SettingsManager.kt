package com.configdoctor.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsManager {
    private val DEFAULT_HEAL_MODE = stringPreferencesKey("default_heal_mode")
    private val TEST_TIMEOUT = intPreferencesKey("test_timeout")
    private val IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")

    fun getDefaultHealMode(context: Context): Flow<String> =
        context.dataStore.data.map { it[DEFAULT_HEAL_MODE] ?: "DEEP" }

    suspend fun setDefaultHealMode(context: Context, mode: String) {
        context.dataStore.edit { it[DEFAULT_HEAL_MODE] = mode }
    }

    fun getTestTimeout(context: Context): Flow<Int> =
        context.dataStore.data.map { it[TEST_TIMEOUT] ?: 10000 }

    suspend fun setTestTimeout(context: Context, timeout: Int) {
        context.dataStore.edit { it[TEST_TIMEOUT] = timeout }
    }

    fun getTheme(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[IS_DARK_THEME] ?: true }

    suspend fun setTheme(context: Context, isDark: Boolean) {
        context.dataStore.edit { it[IS_DARK_THEME] = isDark }
    }
}