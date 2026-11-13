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

enum class ThemeMode { DARK, LIGHT, AUTO }
enum class AppLanguage { FA, EN }

object SettingsManager {
    private val DEFAULT_HEAL_MODE = stringPreferencesKey("default_heal_mode")
    private val TEST_TIMEOUT = intPreferencesKey("test_timeout")
    private val CLOUDFLARE_IPS = stringPreferencesKey("cloudflare_ips")
    private val LAST_IP_UPDATE = stringPreferencesKey("last_ip_update")
    
    // تم و زبان
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val APP_LANGUAGE = stringPreferencesKey("app_language")
    private val ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
    private val HEAL_TUTORIAL_SHOWN = booleanPreferencesKey("heal_tutorial_shown")

    // ─── Heal Mode ───
    fun getDefaultHealMode(context: Context): Flow<String> =
        context.dataStore.data.map { it[DEFAULT_HEAL_MODE] ?: "DEEP" }

    suspend fun setDefaultHealMode(context: Context, mode: String) {
        context.dataStore.edit { it[DEFAULT_HEAL_MODE] = mode }
    }

    // ─── Timeout ───
    fun getTestTimeout(context: Context): Flow<Int> =
        context.dataStore.data.map { it[TEST_TIMEOUT] ?: 10000 }

    suspend fun setTestTimeout(context: Context, timeout: Int) {
        context.dataStore.edit { it[TEST_TIMEOUT] = timeout }
    }

    // ─── تم ───
    fun getThemeMode(context: Context): Flow<ThemeMode> =
        context.dataStore.data.map { 
            when (it[THEME_MODE]) {
                "LIGHT" -> ThemeMode.LIGHT
                "DARK" -> ThemeMode.DARK
                else -> ThemeMode.AUTO
            }
        }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.dataStore.edit { it[THEME_MODE] = mode.name }
    }

    // ─── زبان ───
    fun getLanguage(context: Context): Flow<AppLanguage> =
        context.dataStore.data.map { 
            when (it[APP_LANGUAGE]) {
                "EN" -> AppLanguage.EN
                else -> AppLanguage.FA
            }
        }

    suspend fun setLanguage(context: Context, lang: AppLanguage) {
        context.dataStore.edit { it[APP_LANGUAGE] = lang.name }
    }

    // ─── Onboarding ───
    fun isOnboardingShown(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_SHOWN] ?: false }

    suspend fun setOnboardingShown(context: Context) {
        context.dataStore.edit { it[ONBOARDING_SHOWN] = true }
    }

    // ─── Heal Tutorial ───
    fun isHealTutorialShown(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[HEAL_TUTORIAL_SHOWN] ?: false }

    suspend fun setHealTutorialShown(context: Context) {
        context.dataStore.edit { it[HEAL_TUTORIAL_SHOWN] = true }
    }

    // ─── IPها ───
    fun getCloudflareIps(context: Context): Flow<String?> =
        context.dataStore.data.map { it[CLOUDFLARE_IPS] }

    suspend fun setCloudflareIps(context: Context, ips: String) {
        context.dataStore.edit {
            it[CLOUDFLARE_IPS] = ips
            it[LAST_IP_UPDATE] = System.currentTimeMillis().toString()
        }
    }

    fun getLastIpUpdate(context: Context): Flow<Long> =
        context.dataStore.data.map { it[LAST_IP_UPDATE]?.toLongOrNull() ?: 0L }
}