package com.configdoctor

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import com.configdoctor.data.AppLanguage
import com.configdoctor.data.SettingsManager
import com.configdoctor.data.ThemeMode
import com.configdoctor.ui.screens.MainScreen
import com.configdoctor.ui.screens.OnboardingScreen
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            val context = LocalContext.current
            var themeMode by remember { mutableStateOf(ThemeMode.AUTO) }
            var language by remember { mutableStateOf(AppLanguage.FA) }
            var showOnboarding by remember { mutableStateOf<Boolean?>(null) }

            // بارگذاری تنظیمات
            LaunchedEffect(Unit) {
                SettingsManager.getThemeMode(this@MainActivity).collect { themeMode = it }
            }
            LaunchedEffect(Unit) {
                SettingsManager.getLanguage(this@MainActivity).collect { language = it }
            }
            LaunchedEffect(Unit) {
                SettingsManager.isOnboardingShown(this@MainActivity).collect { 
                    if (showOnboarding == null) showOnboarding = !it
                }
            }

            // تعیین وضعیت تاریک/روشن
            val isDarkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.AUTO -> {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    hour < 7 || hour >= 19 // شب تاریک، روز روشن
                }
            }

            // تعیین جهت و لوکال
            val layoutDirection = if (language == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr
            val locale = if (language == AppLanguage.FA) Locale("fa", "IR") else Locale.ENGLISH

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

            // اعمال لوکال به Context
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            val localizedContext = context.createConfigurationContext(config)
            val localizedResources = localizedContext.resources

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme(colorScheme = colorScheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when (showOnboarding) {
                            true -> {
                                OnboardingScreen(
                                    resources = localizedResources,
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
                                    language = language,
                                    onToggleTheme = { newMode ->
                                        themeMode = newMode
                                        lifecycleScope.launch {
                                            SettingsManager.setThemeMode(this@MainActivity, newMode)
                                        }
                                    },
                                    onLanguageChange = { newLang ->
                                        language = newLang
                                        lifecycleScope.launch {
                                            SettingsManager.setLanguage(this@MainActivity, newLang)
                                        }
                                    }
                                )
                            }
                            null -> {
                                // در حال بارگذاری
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
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