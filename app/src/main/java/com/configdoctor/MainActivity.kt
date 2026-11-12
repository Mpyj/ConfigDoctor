package com.configdoctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.layout.fillMaxSize
import com.configdoctor.ui.screens.ConfigDoctorScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF6C63FF),
                        secondary = Color(0xFF00BFA5),
                        background = Color(0xFFF8F9FA),
                        surface = Color.White
                    )
                ) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        ConfigDoctorScreen(this@MainActivity)
                    }
                }
            }
        }
    }
}