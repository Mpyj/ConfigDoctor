package com.configdoctor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.data.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var healMode by remember { mutableStateOf("DEEP") }
    var timeout by remember { mutableStateOf(10000) }

    LaunchedEffect(Unit) {
        healMode = SettingsManager.getDefaultHealMode(context).first()
        timeout = SettingsManager.getTestTimeout(context).first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("حالت پیش‌فرض ترمیم", fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = healMode == "QUICK",
                    onClick = {
                        healMode = "QUICK"
                        scope.launch { SettingsManager.setDefaultHealMode(context, "QUICK") }
                    }
                )
                Text("سریع")
                Spacer(Modifier.width(16.dp))
                RadioButton(
                    selected = healMode == "DEEP",
                    onClick = {
                        healMode = "DEEP"
                        scope.launch { SettingsManager.setDefaultHealMode(context, "DEEP") }
                    }
                )
                Text("کامل (دقیق‌تر)")
            }

            Spacer(Modifier.height(24.dp))
            Text("زمان انتظار تست (میلی‌ثانیه)", fontWeight = FontWeight.Bold)
            Slider(
                value = timeout.toFloat(),
                onValueChange = {
                    timeout = it.toInt()
                    scope.launch { SettingsManager.setTestTimeout(context, timeout) }
                },
                valueRange = 3000f..30000f,
                steps = 26
            )
            Text("$timeout ms", fontSize = 14.sp)
        }
    }
}