package com.configdoctor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.diagnose.DiagnosisResult
import com.configdoctor.diagnose.Diagnostician
import com.configdoctor.diagnose.OverallStatus
import com.configdoctor.diagnose.ServerType
import com.configdoctor.parser.VlessParser
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        ConfigDoctorScreen(this@MainActivity)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDoctorScreen(activity: MainActivity) {
    var link by remember { mutableStateOf("") }
    var diagnosisResult by remember { mutableStateOf<DiagnosisResult?>(null) }
    var healedLink by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isHealing by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = activity.applicationContext

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دستیار هوشمند کانفیگ", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("لینک VLESS را پیست کنید") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (link.isBlank()) {
                        errorText = "لطفاً یه لینک وارد کن"
                        return@Button
                    }
                    isLoading = true
                    diagnosisResult = null
                    healedLink = null
                    errorText = null
                    scope.launch {
                        val parsed = VlessParser.parse(link)
                        parsed.fold(
                            onSuccess = { config ->
                                try {
                                    diagnosisResult = Diagnostician.diagnose(activity, config)
                                } catch (e: Exception) {
                                    errorText = "خطا در تشخیص: ${e.message}"
                                }
                            },
                            onFailure = { errorText = "خطا در پارس لینک: ${it.message}" }
                        )
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading && !isHealing
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isLoading) "در حال تشخیص..." else "شروع تشخیص")
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (errorText != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorText!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            if (diagnosisResult != null) {
                val res = diagnosisResult!!
                
                StatusCard(status = res.overallStatus, summary = res.summary)
                Spacer(modifier = Modifier.height(16.dp))
                DetailCard(res)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("result", res.summary))
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("📋 کپی گزارش")
                    }

                    if (res.overallStatus == OverallStatus.IP_DEAD) {
                        Button(
                            onClick = {
                                isHealing = true
                                scope.launch {
                                    val healedConfig = Diagnostician.healConfig(activity, res.config)
                                    if (healedConfig != null) {
                                        healedLink = VlessParser.buildLink(healedConfig)
                                    } else {
                                        errorText = "❌ متاسفانه IP تمیزی پیدا نشد."
                                    }
                                    isHealing = false
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            enabled = !isHealing
                        ) {
                            if (isHealing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                            } else {
                                Text("🔧 ترمیم خودکار")
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = healedLink != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("✅ کانفیگ با موفقیت ترمیم شد!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                            Spacer(Modifier.height(8.dp))
                            SelectionContainer {
                                Text(text = healedLink ?: "", fontSize = 14.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("healed_link", healedLink))
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📋 کپی لینک جدید")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusCard(status: OverallStatus, summary: String) {
    val (color, icon, title) = when (status) {
        OverallStatus.HEALTHY -> Triple(Color(0xFF4CAF50), Icons.Default.CheckCircle, "✅ کانفیگ سالم است")
        OverallStatus.SNI_FILTERED -> Triple(Color(0xFFFFC107), Icons.Default.Warning, "⚠️ احتمال فیلتر SNI")
        OverallStatus.IP_DEAD -> Triple(Color(0xFFF44336), Icons.Default.Warning, "❌ سرور پاسخ نمی‌دهد")
        else -> Triple(Color(0xFF9E9E9E), Icons.Default.Info, "ℹ️ وضعیت نامشخص")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
                Spacer(Modifier.height(4.dp))
                SelectionContainer {
                    Text(text = summary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun DetailCard(res: DiagnosisResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("📊 جزئیات تست‌ها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            DetailRow("نوع سرور", serverTypeLabel(res.serverType))
            DetailRow("IP سرور", res.resolvedIp ?: "-")
            DetailRow("TCP RTT", "${res.tcpLatencyMs ?: "-"} ms")
            DetailRow("Real Delay", "${res.realDelayMs ?: "-"} ms")

            val tlsText = when (res.tlsOk) {
                true -> "موفق"
                false -> "ناموفق (${res.tlsError ?: "خطا"})"
                null -> "انجام نشد"
            }
            DetailRow("TLS", tlsText)
            DetailRow("IP خروجی", res.outputIp ?: "-")
            DetailRow("کشور خروجی", formatCountry(res.outputCountry))
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        SelectionContainer {
            Text(text = value, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }
}

fun serverTypeLabel(type: ServerType): String = when (type) {
    ServerType.CLOUDFLARE -> "کلودفلر (Cloudflare)"
    ServerType.DATACENTER -> "دیتاسنتر / VPS"
    ServerType.DIRECT -> "سرور مستقیم"
    ServerType.UNKNOWN -> "نامشخص"
}

fun formatCountry(code: String?): String {
    if (code.isNullOrBlank()) return "-"
    return when (code.uppercase()) {
        "US" -> "آمریکا (US)"
        "DE" -> "آلمان (DE)"
        "NL" -> "هلند (NL)"
        "FR" -> "فرانسه (FR)"
        "GB" -> "انگلستان (GB)"
        "TR" -> "ترکیه (TR)"
        "AE" -> "امارات (AE)"
        "CA" -> "کانادا (CA)"
        "FI" -> "فنلاند (FI)"
        "SE" -> "سوئد (SE)"
        "RU" -> "روسیه (RU)"
        "IR" -> "ایران (IR)"
        else -> code.uppercase()
    }
}