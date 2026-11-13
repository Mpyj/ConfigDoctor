package com.configdoctor.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.MainActivity
import com.configdoctor.data.AppDatabase
import com.configdoctor.data.HistoryEntity
import com.configdoctor.diagnose.DiagnosisResult
import com.configdoctor.diagnose.Diagnostician
import com.configdoctor.diagnose.OverallStatus
import com.configdoctor.parser.ConfigParserFactory
import com.configdoctor.ui.components.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SingleConfigTab(activity: MainActivity, isDarkTheme: Boolean) {
    var link by remember { mutableStateOf("") }
    var diagnosisResult by remember { mutableStateOf<DiagnosisResult?>(null) }
    var healedLink by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isHealing by remember { mutableStateOf(false) }
    var healingProgressList by remember { mutableStateOf<List<String>>(emptyList()) }
    var showHealDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val context = activity.applicationContext
    val scrollState = rememberScrollState()

    val db = remember { AppDatabase.getDatabase(context) }
    val historyDao = db.historyDao()
    val historyList by historyDao.getAll().collectAsState(initial = emptyList())

    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray

    // تابع کمکی برای اضافه کردن به لیست progress (حداکثر ۵ تا)
    fun addProgress(msg: String) {
        scope.launch(Dispatchers.Main) {
            healingProgressList = (healingProgressList + msg).takeLast(5)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        // ورودی
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("لینک کانفیگ", fontSize = 14.sp, color = subTextColor, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    placeholder = { Text("vless://...", color = subTextColor) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6C63FF),
                        unfocusedBorderColor = subTextColor.copy(alpha = 0.3f),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        cursorColor = Color(0xFF6C63FF)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // دکمه تشخیص
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
                healingProgressList = emptyList()
                scope.launch {
                    val parsed = ConfigParserFactory.parse(link)
                    parsed.fold(
                        onSuccess = { config ->
                            try {
                                val diag = Diagnostician.diagnose(activity, config)
                                diagnosisResult = diag
                                historyDao.insert(HistoryEntity(link = link, timestamp = System.currentTimeMillis(), status = diag.overallStatus.name, outputIp = diag.outputIp, outputCountry = diag.outputCountry, realDelayMs = diag.realDelayMs))
                            } catch (e: Exception) {
                                errorText = "خطا در تشخیص: ${e.message}"
                            }
                        },
                        onFailure = { errorText = "خطا در پارس لینک: ${it.message}" }
                    )
                    isLoading = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            enabled = !isLoading && !isHealing && link.isNotBlank(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
        ) {
            if (isLoading) {
                Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(24.dp).rotate(rotation))
                Spacer(Modifier.width(8.dp))
            }
            Text(if (isLoading) "در حال بررسی..." else "شروع تشخیص", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        // دکمه تاریخچه — فقط اگه تاریخچه خالی نباشه
        if (historyList.isNotEmpty()) {
            OutlinedButton(
                onClick = { showHistory = !showHistory },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, subTextColor.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = textColor)
                Spacer(Modifier.width(8.dp))
                Text(if (showHistory) "بستن تاریخچه" else "نمایش تاریخچه (${historyList.size})", color = textColor)
            }
        }

        AnimatedVisibility(visible = showHistory) {
            HistorySection(
                historyList = historyList,
                onClear = { scope.launch { historyDao.clearAll() } },
                isDarkTheme = isDarkTheme
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // خطا
        AnimatedVisibility(visible = errorText != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Error, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(errorText ?: "", color = Color.White, fontSize = 14.sp)
                }
            }
        }

        // Empty State
        AnimatedVisibility(visible = diagnosisResult == null && !isLoading && errorText == null) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Rounded.Troubleshoot, contentDescription = null, modifier = Modifier.size(100.dp), tint = subTextColor.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(16.dp))
                Text("آماده برای تشخیص", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = subTextColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text("لینک کانفیگ خود را وارد کنید تا\nوضعیت سلامت آن را بررسی کنیم.", textAlign = TextAlign.Center, fontSize = 14.sp, color = subTextColor.copy(alpha = 0.7f))
            }
        }

        // نتیجه
        AnimatedVisibility(visible = diagnosisResult != null) {
            Column {
                val res = diagnosisResult!!
                StatusCard(status = res.overallStatus, summary = res.summary, isDarkTheme = isDarkTheme)
                Spacer(modifier = Modifier.height(16.dp))
                DetailGrid(res, isDarkTheme)
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("result", res.summary))
                            Toast.makeText(context, "گزارش کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6C63FF))
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF6C63FF))
                        Spacer(Modifier.width(4.dp))
                        Text("کپی", color = Color(0xFF6C63FF))
                    }

                    // دکمه ترمیم برای همه‌ی وضعیت‌های غیر HEALTHY
                    if (res.overallStatus != OverallStatus.HEALTHY) {
                        Button(
                            onClick = { showHealDialog = true },
                            modifier = Modifier.weight(1f).height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isHealing
                        ) {
                            if (isHealing) {
                                Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp).rotate(rotation))
                            } else {
                                Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("ترمیم")
                            }
                        }
                    }
                }

                // Progress به صورت لیست آخرین ۵ پیام
                AnimatedVisibility(visible = isHealing && healingProgressList.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF16213E) else Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color(0xFF6C63FF), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("در حال ترمیم...", fontWeight = FontWeight.Bold, color = textColor)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            healingProgressList.forEach { p ->
                                Text("• $p", fontSize = 11.sp, color = subTextColor, lineHeight = 16.sp)
                            }
                        }
                    }
                }
            }
        }

        // لینک ترمیم شده
        AnimatedVisibility(visible = healedLink != null) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ترمیم موفق!", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(color = Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        SelectionContainer {
                            Text(text = healedLink ?: "", fontSize = 12.sp, color = Color.White, modifier = Modifier.padding(8.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("healed_link", healedLink))
                            Toast.makeText(context, "لینک کپی شد", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                    ) {
                        Text("کپی لینک جدید", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    // دیالوگ ترمیم
    if (showHealDialog && diagnosisResult != null) {
        HealModeDialog(
            onDismiss = { showHealDialog = false },
            onQuick = {
                showHealDialog = false
                isHealing = true
                healingProgressList = emptyList()
                addProgress("شروع ترمیم (سریع)...")
                scope.launch {
                    val res = diagnosisResult!!
                    val healedConfig = Diagnostician.healConfig(activity, res.config, Diagnostician.HealMode.QUICK) { p -> addProgress(p) }
                    if (healedConfig != null) {
                        healedLink = ConfigParserFactory.buildLink(healedConfig)
                        historyDao.insert(HistoryEntity(link = healedLink!!, timestamp = System.currentTimeMillis(), status = "HEALED", outputIp = res.outputIp, outputCountry = res.outputCountry, realDelayMs = res.realDelayMs))
                    } else { errorText = "مسیر سالمی پیدا نشد." }
                    isHealing = false
                }
            },
            onDeep = {
                showHealDialog = false
                isHealing = true
                healingProgressList = emptyList()
                addProgress("شروع ترمیم (کامل)...")
                scope.launch {
                    val res = diagnosisResult!!
                    val healedConfig = Diagnostician.healConfig(activity, res.config, Diagnostician.HealMode.DEEP) { p -> addProgress(p) }
                    if (healedConfig != null) {
                        healedLink = ConfigParserFactory.buildLink(healedConfig)
                        historyDao.insert(HistoryEntity(link = healedLink!!, timestamp = System.currentTimeMillis(), status = "HEALED", outputIp = res.outputIp, outputCountry = res.outputCountry, realDelayMs = res.realDelayMs))
                    } else { errorText = "مسیر سالمی پیدا نشد." }
                    isHealing = false
                }
            }
        )
    }
}