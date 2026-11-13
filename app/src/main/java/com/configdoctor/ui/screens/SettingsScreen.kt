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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.data.AppLanguage
import com.configdoctor.data.SettingsManager
import com.configdoctor.data.ThemeMode
import com.configdoctor.diagnose.CloudflareCleanIps
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isDarkTheme: Boolean,
    themeMode: ThemeMode,
    language: AppLanguage,
    onThemeChange: (ThemeMode) -> Unit,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var healMode by remember { mutableStateOf("DEEP") }
    var timeout by remember { mutableStateOf(10000) }
    var isUpdatingIps by remember { mutableStateOf(false) }
    var lastUpdate by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        healMode = SettingsManager.getDefaultHealMode(context).first()
        timeout = SettingsManager.getTestTimeout(context).first()
        lastUpdate = SettingsManager.getLastIpUpdate(context).first()
    }

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (language == AppLanguage.FA) "تنظیمات" else "Settings", fontWeight = FontWeight.Bold) },
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
            // ─── بخش ظاهری و زبان ───
            SettingsSection(title = if (language == AppLanguage.FA) "ظاهر و زبان" else "Appearance & Language") {
                SettingsItem(
                    icon = Icons.Rounded.Palette,
                    title = if (language == AppLanguage.FA) "تم برنامه" else "Theme",
                    subtitle = when (themeMode) {
                        ThemeMode.DARK -> if (language == AppLanguage.FA) "تاریک" else "Dark"
                        ThemeMode.LIGHT -> if (language == AppLanguage.FA) "روشن" else "Light"
                        ThemeMode.AUTO -> if (language == AppLanguage.FA) "خودکار (بر اساس ساعت)" else "Auto (By Time)"
                    },
                    onClick = {
                        val next = when (themeMode) {
                            ThemeMode.DARK -> ThemeMode.LIGHT
                            ThemeMode.LIGHT -> ThemeMode.AUTO
                            ThemeMode.AUTO -> ThemeMode.DARK
                        }
                        onThemeChange(next)
                    }
                )
                
                SettingsItem(
                    icon = Icons.Rounded.Language,
                    title = if (language == AppLanguage.FA) "زبان" else "Language",
                    subtitle = if (language == AppLanguage.FA) "فارسی" else "English",
                    onClick = {
                        onLanguageChange(if (language == AppLanguage.FA) AppLanguage.EN else AppLanguage.FA)
                    }
                )
            }

            // ─── بخش ترمیم ───
            SettingsSection(title = if (language == AppLanguage.FA) "ترمیم" else "Healing") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Build, contentDescription = null, tint = Color(0xFF6C63FF))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(if (language == AppLanguage.FA) "حالت پیش‌فرض ترمیم" else "Default Heal Mode", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = healMode == "QUICK",
                                onClick = {
                                    healMode = "QUICK"
                                    scope.launch { SettingsManager.setDefaultHealMode(context, "QUICK") }
                                }
                            )
                            Text(if (language == AppLanguage.FA) "سریع" else "Quick")
                            Spacer(Modifier.width(16.dp))
                            RadioButton(
                                selected = healMode == "DEEP",
                                onClick = {
                                    healMode = "DEEP"
                                    scope.launch { SettingsManager.setDefaultHealMode(context, "DEEP") }
                                }
                            )
                            Text(if (language == AppLanguage.FA) "کامل" else "Deep")
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(if (language == AppLanguage.FA) "زمان انتظار تست (ms)" else "Test Timeout (ms)", fontWeight = FontWeight.Bold)
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

            // ─── بخش IPها ───
            SettingsSection(title = if (language == AppLanguage.FA) "IPهای کلادفلر" else "Cloudflare IPs") {
                Text(
                    if (language == AppLanguage.FA) 
                        "لیست IPهای تمیز برای ترمیم. هر چند هفته آپدیت کن."
                    else 
                        "Clean IP list for healing. Update every few weeks.",
                    fontSize = 12.sp,
                    color = subTextColor
                )
                if (lastUpdate > 0) {
                    val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(lastUpdate))
                    Text(
                        (if (language == AppLanguage.FA) "آخرین آپدیت: " else "Last update: ") + dateStr,
                        fontSize = 11.sp,
                        color = subTextColor
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        isUpdatingIps = true
                        scope.launch {
                            val sources = listOf(
                                "https://raw.githubusercontent.com/ircfspace/cf-ip-scanner/main/ip.txt",
                                "https://raw.githubusercontent.com/vfarid/cf-clean-ips/main/list.txt",
                                "https://raw.githubusercontent.com/Epodonios/v2ray-configs/main/Cloudflare.txt"
                            )
                            var success = false
                            for (url in sources) {
                                val result = CloudflareCleanIps.fetchFromSource(url)
                                if (result.isSuccess) {
                                    val ips = result.getOrNull() ?: emptyList()
                                    if (ips.isNotEmpty()) {
                                        SettingsManager.setCloudflareIps(context, ips.joinToString("\n"))
                                        lastUpdate = System.currentTimeMillis()
                                        success = true
                                        Toast.makeText(context, "✅ ${ips.size} IP", Toast.LENGTH_SHORT).show()
                                        break
                                    }
                                }
                            }
                            if (!success) {
                                Toast.makeText(context, "❌ Update failed", Toast.LENGTH_LONG).show()
                            }
                            isUpdatingIps = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdatingIps
                ) {
                    if (isUpdatingIps) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text(if (language == AppLanguage.FA) "در حال آپدیت..." else "Updating...")
                    } else {
                        Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (language == AppLanguage.FA) "آپدیت لیست IPها" else "Update IP List")
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            SettingsManager.setCloudflareIps(context, "")
                            lastUpdate = 0L
                            Toast.makeText(context, if (language == AppLanguage.FA) "بازگشت به پیش‌فرض" else "Reset to default", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (language == AppLanguage.FA) "بازگشت به لیست پیش‌فرض" else "Reset to Default List")
                }
            }

            // ─── بخش شبکه‌های اجتماعی ───
            SettingsSection(title = if (language == AppLanguage.FA) "ارتباط با ما" else "Connect with Us") {
                SocialItem(
                    icon = Icons.Rounded.Send,
                    title = "Telegram",
                    subtitle = "@MpyjTelegram",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/MpyjTelegram")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Telegram not installed", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                SocialItem(
                    icon = Icons.Rounded.AlternateEmail,
                    title = "X (Twitter)",
                    subtitle = "@Mpyj_X",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://x.com/Mpyj_X")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Browser not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                SocialItem(
                    icon = Icons.Rounded.Code,
                    title = "GitHub",
                    subtitle = "github.com/Mpyj",
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Mpyj")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Browser not found", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // ─── بخش درباره ───
            SettingsSection(title = if (language == AppLanguage.FA) "درباره" else "About") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, contentDescription = null, tint = Color(0xFF6C63FF))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Config Doctor", fontWeight = FontWeight.Bold)
                        Text(
                            (if (language == AppLanguage.FA) "نسخه " else "Version ") + "0.1.0",
                            fontSize = 12.sp,
                            color = subTextColor
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    if (language == AppLanguage.FA) 
                        "ابزار هوشمند تست و ترمیم کانفیگ‌های VLESS، VMess و Trojan."
                    else 
                        "Smart tool to test and heal VLESS, VMess, and Trojan configs.",
                    fontSize = 12.sp,
                    color = subTextColor
                )
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF6C63FF))
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
private fun SocialItem(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
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