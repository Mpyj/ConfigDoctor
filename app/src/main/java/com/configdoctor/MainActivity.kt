package com.configdoctor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.lifecycleScope
import com.configdoctor.data.SettingsManager
import com.configdoctor.ui.screens.LanguageSelectionScreen
import com.configdoctor.ui.screens.MainScreen
import com.configdoctor.ui.screens.OnboardingScreen
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            var themeMode by remember { mutableStateOf("AUTO") }
            var language by remember { mutableStateOf<String?>(null) }
            var showOnboarding by remember { mutableStateOf<Boolean?>(null) }
            var isChangingLanguage by remember { mutableStateOf(false) }

            // بارگذاری تنظیمات
            LaunchedEffect(Unit) {
                SettingsManager.getThemeMode(this@MainActivity).collect { themeMode = it }
            }
            LaunchedEffect(Unit) {
                SettingsManager.getLanguage(this@MainActivity).collect { lang ->
                    // اگه زبان خالی بود، یعنی کاربر هنوز انتخاب نکرده
                    if (lang.isBlank()) {
                        language = null
                    } else {
                        // فقط اگه زبان واقعاً عوض شده، AppCompatDelegate رو صدا بزن
                        val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                        val targetLocale = if (lang == "FA") "fa" else "en"
                        if (!currentLocale.startsWith(targetLocale)) {
                            isChangingLanguage = true
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(targetLocale)
                            )
                            // بعد از تغییر، Activity دوباره ساخته می‌شه، پس اینجا نیازی به آپدیت نیست
                        } else {
                            // زبان همونه، فقط state رو آپدیت کن
                            language = lang
                        }
                    }
                }
            }
            LaunchedEffect(Unit) {
                SettingsManager.isOnboardingShown(this@MainActivity).collect { 
                    if (showOnboarding == null) showOnboarding = !it
                }
            }

            // اگه در حال تغییر زبان هستیم، لودینگ نشون بده (جلوگیری از پرش)
            if (isChangingLanguage) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@setContent
            }

            // اگه زبان هنوز انتخاب نشده، صفحه انتخاب زبان
            if (language == null) {
                LanguageSelectionScreen(
                    onSelect = { lang ->
                        isChangingLanguage = true
                        lifecycleScope.launch {
                            SettingsManager.setLanguage(this@MainActivity, lang)
                        }
                    }
                )
                return@setContent
            }

            val currentLang = language!!

            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    hour < 7 || hour >= 19
                }
            }

            val layoutDirection = if (currentLang == "FA") LayoutDirection.Rtl else LayoutDirection.Ltr

            val colorScheme = if (isDarkTheme) {
                darkColorScheme(
                    primary = Color(0xFF6C63FF),
                    secondary = Color(0xFF00BFA5),
                    background = Color(0xFF1A1A2E),
                    surface = Color(0xFF16213E)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF6C63FF),
                    secondary = Color(0xFF00BFA5),
                    background = Color(0xFFF8F9FA),
                    surface = Color.White
                )
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme(colorScheme = colorScheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when (showOnboarding) {
                            true -> {
                                OnboardingScreen(
                                    onFinish = {
                                        showOnboarding = false
                                        lifecycleScope.launch {
                                            SettingsManager.setOnboardingShown(this@MainActivity)
                                        }
                                    }
                                )
                            }
                            false -> {
                                MainScreen(
                                    activity = this@MainActivity,
                                    isDarkTheme = isDarkTheme,
                                    themeMode = themeMode,
                                    language = currentLang,
                                    onToggleTheme = { newMode ->
                                        themeMode = newMode
                                        lifecycleScope.launch {
                                            SettingsManager.setThemeMode(this@MainActivity, newMode)
                                        }
                                    },
                                    onLanguageChange = { newLang ->
                                        // فقط اگه واقعاً عوض شده
                                        if (newLang != currentLang) {
                                            isChangingLanguage = true
                                            lifecycleScope.launch {
                                                SettingsManager.setLanguage(this@MainActivity, newLang)
                                            }
                                        }
                                    }
                                )
                            }
                            null -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}