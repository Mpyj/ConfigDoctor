package com.configdoctor.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.MainActivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    activity: MainActivity,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(onBack = { showSettings = false })
        return
    }

    val bgColors = if (isDarkTheme) {
        listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
    } else {
        listOf(Color(0xFFF8F9FA), Color(0xFFE0E0E0), Color(0xFFD1D1D1))
    }
    val textColor = if (isDarkTheme) Color.White else Color.Black

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = bgColors))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // --- هدر و دکمه‌های بالا ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.HealthAndSafety,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = Color(0xFF6C63FF)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("دستیار هوشمند", fontSize = 20.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, color = textColor)
                }
                Row {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = "تنظیمات", tint = textColor)
                    }
                    IconButton(onClick = onToggleTheme) {
                        Icon(
                            if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                            contentDescription = "تغییر تم",
                            tint = textColor
                        )
                    }
                }
            }

            // --- تب‌ها ---
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF6C63FF),
                divider = {}
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("تک کانفیگ", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("گروهی / ساب", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) }
                )
            }

            // --- محتوای تب ---
            Box(modifier = Modifier.weight(1f)) {
                if (selectedTabIndex == 0) {
                    SingleConfigTab(activity = activity, isDarkTheme = isDarkTheme)
                } else {
                    BatchTab(activity = activity, isDarkTheme = isDarkTheme)
                }
            }
        }
    }
}