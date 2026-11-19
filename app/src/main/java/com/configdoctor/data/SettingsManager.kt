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
    private val CLOUDFLARE_IPS = stringPreferencesKey("cloudflare_ips")
    private val CACHED_WORKING_IPS = stringPreferencesKey("cached_working_ips") // ← جدید
    private val LAST_IP_UPDATE = stringPreferencesKey("last_ip_update")
    
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val APP_LANGUAGE = stringPreferencesKey("app_language")
    
    private val ONBOARDING_SHOWN = booleanPreferencesKey("onboarding_shown")
    private val HEAL_TUTORIAL_SHOWN = booleanPreferencesKey("heal_tutorial_shown")

    // Heal Mode
    fun getDefaultHealMode(context: Context): Flow<String> =
        context.dataStore.data.map { it[DEFAULT_HEAL_MODE] ?: "DEEP" }
    suspend fun setDefaultHealMode(context: Context, mode: String) {
        context.dataStore.edit { it[DEFAULT_HEAL_MODE] = mode }
    }

    // Timeout
    fun getTestTimeout(context: Context): Flow<Int> =
        context.dataStore.data.map { it[TEST_TIMEOUT] ?: 10000 }
    suspend fun setTestTimeout(context: Context, timeout: Int) {
        context.dataStore.edit { it[TEST_TIMEOUT] = timeout }
    }

    // Theme
    fun getThemeMode(context: Context): Flow<String> =
        context.dataStore.data.map { it[THEME_MODE] ?: "AUTO" }
    suspend fun setThemeMode(context: Context, mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    // Language
    fun getLanguage(context: Context): Flow<String> =
        context.dataStore.data.map { it[APP_LANGUAGE] ?: "" }
    suspend fun setLanguage(context: Context, lang: String) {
        context.dataStore.edit { it[APP_LANGUAGE] = lang }
    }

    // Onboarding
    fun isOnboardingShown(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[ONBOARDING_SHOWN] ?: false }
    suspend fun setOnboardingShown(context: Context) {
        context.dataStore.edit { it[ONBOARDING_SHOWN] = true }
    }

    // Heal Tutorial
    fun isHealTutorialShown(context: Context): Flow<Boolean> =
        context.dataStore.data.map { it[HEAL_TUTORIAL_SHOWN] ?: false }
    suspend fun setHealTutorialShown(context: Context) {
        context.dataStore.edit { it[HEAL_TUTORIAL_SHOWN] = true }
    }

    // IPs
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

    // ─── کش IPهای موفق ───
    fun getCachedWorkingIps(context: Context): Flow<String?> =
        context.dataStore.data.map { it[CACHED_WORKING_IPS] }

    suspend fun addCachedWorkingIp(context: Context, ip: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[CACHED_WORKING_IPS]?.split("\n")?.filter { it.isNotBlank() }?.toMutableList() ?: mutableListOf()
            if (!current.contains(ip)) {
                current.add(0, ip) // جدیدترین‌ها اول لیست
                prefs[CACHED_WORKING_IPS] = current.take(20).joinToString("\n") // فقط ۲۰ تای آخر رو نگه دار
            }
        }
    }
    
    suspend fun clearCachedWorkingIps(context: Context) {
        context.dataStore.edit { it.remove(CACHED_WORKING_IPS) }
    }
}