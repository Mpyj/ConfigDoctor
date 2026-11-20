package com.configdoctor

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
            var isSettingsLoaded by remember { mutableStateOf(false) }
            var isChangingLanguage by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                SettingsManager.getThemeMode(this@MainActivity).collect { themeMode = it }
            }
            LaunchedEffect(Unit) {
                SettingsManager.getLanguage(this@MainActivity).collect { lang ->
                    if (lang.isBlank()) {
                        language = null
                    } else {
                        val currentLocale = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                        val targetLocale = if (lang == "FA") "fa" else "en"
                        
                        if (!currentLocale.startsWith(targetLocale)) {
                            isChangingLanguage = true
                            AppCompatDelegate.setApplicationLocales(
                                LocaleListCompat.forLanguageTags(targetLocale)
                            )
                        } else {
                            language = lang
                        }
                    }
                    isSettingsLoaded = true 
                }
            }
            LaunchedEffect(Unit) {
                SettingsManager.isOnboardingShown(this@MainActivity).collect { 
                    if (showOnboarding == null) showOnboarding = !it
                }
            }

            if (!isSettingsLoaded) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6C63FF))
                }
                return@setContent
            }

            if (isChangingLanguage) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF6C63FF))
                }
                return@setContent
            }

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
                        if (showOnboarding == true) {
                            OnboardingScreen(
                                onFinish = {
                                    showOnboarding = false
                                    lifecycleScope.launch {
                                        SettingsManager.setOnboardingShown(this@MainActivity)
                                    }
                                }
                            )
                        } else {
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
                                    if (newLang != currentLang) {
                                        isChangingLanguage = true
                                        lifecycleScope.launch {
                                            SettingsManager.setLanguage(this@MainActivity, newLang)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}