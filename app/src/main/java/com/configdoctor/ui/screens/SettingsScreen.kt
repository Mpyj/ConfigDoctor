package com.configdoctor.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.data.SettingsManager
import com.configdoctor.diagnose.CloudflareCleanIps
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
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

            Spacer(Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("IPهای کلادفلر", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                "لیست IPهای تمیز برای ترمیم کانفیگ. هر چند هفته یک بار آپدیت کن.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            if (lastUpdate > 0) {
                val dateStr = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(lastUpdate))
                Text("آخرین آپدیت: $dateStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
            }

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
                        var lastError: String? = null
                        for (url in sources) {
                            val result = CloudflareCleanIps.fetchFromSource(url)
                            if (result.isSuccess) {
                                val ips = result.getOrNull() ?: emptyList()
                                if (ips.isNotEmpty()) {
                                    SettingsManager.setCloudflareIps(context, ips.joinToString("\n"))
                                    lastUpdate = System.currentTimeMillis()
                                    success = true
                                    Toast.makeText(context, "✅ ${ips.size} IP آپدیت شد", Toast.LENGTH_SHORT).show()
                                    break
                                }
                            } else {
                                lastError = result.exceptionOrNull()?.message
                            }
                        }
                        if (!success) {
                            Toast.makeText(context, "❌ آپدیت ناموفق: ${lastError ?: "اتصال را چک کن"}", Toast.LENGTH_LONG).show()
                        }
                        isUpdatingIps = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isUpdatingIps
            ) {
                if (isUpdatingIps) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("در حال آپدیت...")
                } else {
                    Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("آپدیت لیست IPها")
                }
            }

            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    scope.launch {
                        SettingsManager.setCloudflareIps(context, "")
                        lastUpdate = 0L
                        Toast.makeText(context, "لیست به حالت پیش‌فرض برگشت", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("بازگشت به لیست پیش‌فرض")
            }
        }
    }
}