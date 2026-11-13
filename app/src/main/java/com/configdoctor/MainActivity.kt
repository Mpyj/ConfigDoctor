package com.configdoctor

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import com.configdoctor.data.SettingsManager
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
            var themeMode by remember { mutableStateOf("AUTO") }
            var language by remember { mutableStateOf("FA") }
            var showOnboarding by remember { mutableStateOf<Boolean?>(null) }

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

            val isDarkTheme = when (themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> {
                    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                    hour < 7 || hour >= 19
                }
            }

            val locale = if (language == "FA") Locale("fa", "IR") else Locale.ENGLISH
            val layoutDirection = if (language == "FA") LayoutDirection.Rtl else LayoutDirection.Ltr

            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            val localizedContext = context.createConfigurationContext(config)

            val colorScheme = if (isDarkTheme) {
                darkColorScheme(primary = Color(0xFF6C63FF), secondary = Color(0xFF00BFA5), background = Color(0xFF1A1A2E), surface = Color(0xFF16213E))
            } else {
                lightColorScheme(primary = Color(0xFF6C63FF), secondary = Color(0xFF00BFA5), background = Color(0xFFF8F9FA), surface = Color.White)
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MaterialTheme(colorScheme = colorScheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        when (showOnboarding) {
                            true -> {
                                OnboardingScreen(
                                    resources = localizedContext.resources,
                                    onFinish = {
                                        showOnboarding = false
                                        lifecycleScope.launch { SettingsManager.setOnboardingShown(this@MainActivity) }
                                    }
                                )
                            }
                            false -> {
                                // ست کردن لوکال برای stringResource
                                val configuration = LocalConfiguration.current
                                configuration.setLocale(locale)
                                
                                MainScreen(
                                    activity = this@MainActivity,
                                    isDarkTheme = isDarkTheme,
                                    themeMode = themeMode,
                                    language = language,
                                    onToggleTheme = { newMode ->
                                        themeMode = newMode
                                        lifecycleScope.launch { SettingsManager.setThemeMode(this@MainActivity, newMode) }
                                    },
                                    onLanguageChange = { newLang ->
                                        language = newLang
                                        lifecycleScope.launch { SettingsManager.setLanguage(this@MainActivity, newLang) }
                                    }
                                )
                            }
                            null -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                            }
                        }
                    }
                }
            }
        }
    }
}