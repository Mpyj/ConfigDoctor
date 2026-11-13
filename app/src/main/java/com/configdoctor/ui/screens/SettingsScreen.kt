package com.configdoctor.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.R
import com.configdoctor.data.SettingsManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    themeMode: String,
    language: String,
    onThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var healMode by remember { mutableStateOf("DEEP") }
    var timeout by remember { mutableStateOf(10000) }
    var customIps by remember { mutableStateOf("") }
    var lastUpdate by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        healMode = SettingsManager.getDefaultHealMode(context).first()
        timeout = SettingsManager.getTestTimeout(context).first()
        customIps = SettingsManager.getCloudflareIps(context).first() ?: ""
        lastUpdate = SettingsManager.getLastIpUpdate(context).first()
    }

    val subTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ─── بخش ظاهری ───
            SettingsSection(title = stringResource(R.string.theme_settings)) {
                SettingsItem(
                    icon = Icons.Rounded.Palette,
                    title = "Theme / تم",
                    subtitle = themeMode,
                    onClick = {
                        val next = when (themeMode) {
                            "DARK" -> "LIGHT"
                            "LIGHT" -> "AUTO"
                            else -> "DARK"
                        }
                        onThemeChange(next)
                    }
                )
                SettingsItem(
                    icon = Icons.Rounded.Language,
                    title = stringResource(R.string.language_settings),
                    subtitle = language,
                    onClick = {
                        onLanguageChange(if (language == "FA") "EN" else "FA")
                    }
                )
            }

            // ─── بخش ترمیم ───
            SettingsSection(title = "Heal / ترمیم") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Build, contentDescription = null, tint = Color(0xFF6C63FF))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.default_heal_mode), fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = healMode == "QUICK", onClick = {
                        healMode = "QUICK"
                        scope.launch { SettingsManager.setDefaultHealMode(context, "QUICK") }
                    })
                    Text(stringResource(R.string.quick))
                    Spacer(Modifier.width(16.dp))
                    RadioButton(selected = healMode == "DEEP", onClick = {
                        healMode = "DEEP"
                        scope.launch { SettingsManager.setDefaultHealMode(context, "DEEP") }
                    })
                    Text(stringResource(R.string.deep_accurate))
                }
                
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.test_timeout), fontWeight = FontWeight.Bold)
                Slider(
                    value = timeout.toFloat(),
                    onValueChange = {
                        timeout = it.toInt()
                        scope.launch { SettingsManager.setTestTimeout(context, timeout) }
                    },
                    valueRange = 3000f..30000f,
                    steps = 26
                )
                Text("$timeout ms", fontSize = 14.sp, color = subTextColor)
            }

            // ─── بخش IPها (دستی) ───
            SettingsSection(title = stringResource(R.string.cf_ips)) {
                Text(stringResource(R.string.cf_ips_desc), fontSize = 12.sp, color = subTextColor)
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = customIps,
                    onValueChange = { customIps = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    placeholder = { Text("104.16.1.1\n104.17.2.2\n...", color = subTextColor) },
                    textStyle = LocalTextStyle.current.copy(fontSize = 12.sp)
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        scope.launch {
                            SettingsManager.setCloudflareIps(context, customIps)
                            lastUpdate = System.currentTimeMillis()
                            Toast.makeText(context, "✅ ذخیره شد / Saved", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("ذخیره لیست / Save List")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            SettingsManager.setCloudflareIps(context, "")
                            customIps = ""
                            lastUpdate = 0L
                            Toast.makeText(context, "✅ پیش‌فرض / Default", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.reset_default_ips))
                }
            }

            // ─── بخش شبکه‌های اجتماعی ───
            SettingsSection(title = stringResource(R.string.social_media)) {
                SocialItem(Icons.Rounded.Send, "Telegram", "@MpyjTelegram", "https://t.me/MpyjTelegram")
                SocialItem(Icons.Rounded.AlternateEmail, "X (Twitter)", "@Mpyj_X", "https://x.com/Mpyj_X")
                SocialItem(Icons.Rounded.Code, "GitHub", "github.com/Mpyj", "https://github.com/Mpyj")
            }

            // ─── بخش درباره ───
            SettingsSection(title = stringResource(R.string.about)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFF6C63FF))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Config Doctor", fontWeight = FontWeight.Bold)
                        Text("v0.1.0", fontSize = 12.sp, color = subTextColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF6C63FF))
        Spacer(Modifier.height(8.dp))
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color(0xFF6C63FF))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(Icons.Rounded.ChevronLeft, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
private fun SocialItem(icon: ImageVector, title: String, subtitle: String, url: String) {
    val context = LocalContext.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable {
            try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) } catch (e: Exception) {}
        }.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF6C63FF))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 12.sp, color = Color.Gray)
        }
        Icon(Icons.Rounded.OpenInNew, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
    }
}