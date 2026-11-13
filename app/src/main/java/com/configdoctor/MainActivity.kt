package com.configdoctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.lifecycleScope
import com.configdoctor.data.SettingsManager
import com.configdoctor.ui.screens.MainScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var themeLoaded by remember { mutableStateOf(false) }

            // خوندن تم از DataStore در اولین اجرا
            LaunchedEffect(Unit) {
                SettingsManager.getTheme(this@MainActivity).collect { saved ->
                    if (!themeLoaded) {
                        isDarkTheme = saved
                        themeLoaded = true
                    }
                }
            }

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

            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(colorScheme = colorScheme) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        MainScreen(
                            activity = this@MainActivity,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = {
                                isDarkTheme = !isDarkTheme
                                lifecycleScope.launch {
                                    SettingsManager.setTheme(this@MainActivity, isDarkTheme)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}