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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.MainActivity
import com.configdoctor.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    activity: MainActivity,
    isDarkTheme: Boolean,
    themeMode: String,
    language: String,
    onToggleTheme: (String) -> Unit,
    onLanguageChange: (String) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }

    if (showSettings) {
        SettingsScreen(
            isDarkTheme = isDarkTheme,
            themeMode = themeMode,
            language = language,
            onThemeChange = onToggleTheme,
            onLanguageChange = onLanguageChange,
            onBack = { showSettings = false }
        )
        return
    }

    val bgColors = if (isDarkTheme) listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)) else listOf(Color(0xFFF8F9FA), Color(0xFFE0E0E0), Color(0xFFD1D1D1))
    val textColor = if (isDarkTheme) Color.White else Color.Black

    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(colors = bgColors))) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, modifier = Modifier.size(36.dp), tint = Color(0xFF6C63FF))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.app_name), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Row {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings), tint = textColor)
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF6C63FF),
                divider = {}
            ) {
                Tab(selected = selectedTabIndex == 0, onClick = { selectedTabIndex = 0 }, text = { Text(stringResource(R.string.tab_single), fontWeight = FontWeight.Bold) })
                Tab(selected = selectedTabIndex == 1, onClick = { selectedTabIndex = 1 }, text = { Text(stringResource(R.string.tab_batch), fontWeight = FontWeight.Bold) })
            }

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