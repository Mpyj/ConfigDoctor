package com.configdoctor.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigDoctorScreen(
    activity: MainActivity,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit
) {
    var link by remember { mutableStateOf("") }
    var diagnosisResult by remember { mutableStateOf<DiagnosisResult?>(null) }
    var healedLink by remember { mutableStateOf<String?>(null) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isHealing by remember { mutableStateOf(false) }
    var healingProgress by remember { mutableStateOf("") }
    var showHealDialog by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }
    var showBatchScreen by remember { mutableStateOf(false) }

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

    if (showBatchScreen) {
        BatchScreen(activity = activity, onBack = { showBatchScreen = false })
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkTheme) {
                        listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460))
                    } else {
                        listOf(Color(0xFFF8F9FA), Color(0xFFE0E0E0), Color(0xFFD1D1D1))
                    }
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.HealthAndSafety,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF6C63FF)
                )
                IconButton(onClick = onToggleTheme) {
                    Icon(
                        if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = "تغییر تم",
                        tint = if (isDarkTheme) Color.White else Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "دستیار هوشمند کانفیگ",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkTheme) Color.White else Color.Black
            )
            Text(
                "تشخیص و ترمیم خودکار کانفیگ‌های VLESS",
                fontSize = 14.sp,
                color = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray
            )

            Spacer(modifier = Modifier.height(32.dp))

            // کارت ورودی
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.White
                ),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "لینک کانفیگ",
                        fontSize = 14.sp,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = link,
                        onValueChange = { link = it },
                        placeholder = { Text("vless://...", color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF6C63FF),
                            unfocusedBorderColor = if (isDarkTheme) Color.White.copy(alpha = 0.2f) else Color.LightGray,
                            focusedTextColor = if (isDarkTheme) Color.White else Color.Black,
                            unfocusedTextColor = if (isDarkTheme) Color.White else Color.Black,
                            cursorColor = Color(0xFF6C63FF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                    scope.launch {
                        val parsed = ConfigParserFactory.parse(link)
                        parsed.fold(
                            onSuccess = { config ->
                                try {
                                    val diag = Diagnostician.diagnose(activity, config)
                                    diagnosisResult = diag
                                    historyDao.insert(
                                        HistoryEntity(
                                            link = link,
                                            timestamp = System.currentTimeMillis(),
                                            status = diag.overallStatus.name,
                                            outputIp = diag.outputIp,
                                            outputCountry = diag.outputCountry,
                                            realDelayMs = diag.realDelayMs
                                        )
                                    )
                                } catch (e: Exception) {
                                    errorText = "خطا در تشخیص: ${e.message}"
                                }
                            },
                            onFailure = { errorText = "خطا در پارس لینک: ${it.message}" }
                        )
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = !isLoading && !isHealing && link.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6C63FF),
                    disabledContainerColor = Color.Gray.copy(alpha = 0.5f)
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                if (isLoading) {
                    Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(26.dp).rotate(rotation))
                    Spacer(Modifier.width(12.dp))
                } else {
                    Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isLoading) "در حال بررسی..." else "شروع تشخیص", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            // دکمه‌های تاریخچه و تست گروهی
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { showHistory = !showHistory },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray)
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = if (isDarkTheme) Color.White else Color.Black)
                    Spacer(Modifier.width(4.dp))
                    Text(if (showHistory) "بستن" else "تاریخچه", color = if (isDarkTheme) Color.White else Color.Black)
                }

                OutlinedButton(
                    onClick = { showBatchScreen = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Gray)
                ) {
                    Icon(Icons.Rounded.List, contentDescription = null, tint = if (isDarkTheme) Color.White else Color.Black)
                    Spacer(Modifier.width(4.dp))
                    Text("تست گروهی", color = if (isDarkTheme) Color.White else Color.Black)
                }
            }

            AnimatedVisibility(visible = showHistory) {
                HistorySection(historyList)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // خطا
            AnimatedVisibility(visible = errorText != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    Icon(
                        Icons.Rounded.Troubleshoot,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = if (isDarkTheme) Color.White.copy(alpha = 0.2f) else Color.Gray.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "آماده برای تشخیص",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.5f) else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "لینک کانفیگ خود را وارد کنید تا\nوضعیت سلامت آن را بررسی کنیم.",
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = if (isDarkTheme) Color.White.copy(alpha = 0.4f) else Color.Gray
                    )
                }
            }

            // نتیجه
            AnimatedVisibility(visible = diagnosisResult != null) {
                Column {
                    val res = diagnosisResult!!
                    StatusCard(status = res.overallStatus, summary = res.summary)
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailGrid(res)
                    Spacer(modifier = Modifier.height(20.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("result", res.summary))
                                Toast.makeText(context, "گزارش کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6C63FF))
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF6C63FF))
                            Spacer(Modifier.width(6.dp))
                            Text("کپی گزارش", fontWeight = FontWeight.Bold, color = Color(0xFF6C63FF))
                        }

                        if (res.overallStatus == OverallStatus.IP_DEAD || res.overallStatus == OverallStatus.UNKNOWN) {
                            Button(
                                onClick = { showHealDialog = true },
                                modifier = Modifier.weight(1f).height(50.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isHealing
                            ) {
                                if (isHealing) {
                                    Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(18.dp).rotate(rotation))
                                } else {
                                    Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("ترمیم خودکار")
                                }
                            }
                        }
                    }

                    AnimatedVisibility(visible = isHealing && healingProgress.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF16213E) else Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF6C63FF), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("در حال ترمیم...", fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color.White else Color.Black)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(healingProgress, fontSize = 13.sp, color = if (isDarkTheme) Color.White.copy(alpha = 0.7f) else Color.Gray)
                            }
                        }
                    }
                }
            }

            // لینک ترمیم‌شده
            AnimatedVisibility(visible = healedLink != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("کانفیگ با موفقیت ترمیم شد!", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            SelectionContainer {
                                Text(text = healedLink ?: "", fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(12.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("healed_link", healedLink))
                                Toast.makeText(context, "لینک جدید کپی شد", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF1B5E20))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("کپی لینک جدید", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }

        if (showHealDialog && diagnosisResult != null) {
            HealModeDialog(
                onDismiss = { showHealDialog = false },
                onQuick = {
                    showHealDialog = false
                    isHealing = true
                    healingProgress = "شروع ترمیم (حالت سریع)..."
                    scope.launch {
                        val res = diagnosisResult!!
                        val healedConfig = Diagnostician.healConfig(activity, res.config, Diagnostician.HealMode.QUICK) { progress ->
                            scope.launch(Dispatchers.Main) { healingProgress = progress }
                        }
                        if (healedConfig != null) {
                            healedLink = ConfigParserFactory.buildLink(healedConfig)
                        } else {
                            errorText = "❌ متاسفانه مسیر سالمی پیدا نشد."
                        }
                        isHealing = false
                        healingProgress = ""
                    }
                },
                onDeep = {
                    showHealDialog = false
                    isHealing = true
                    healingProgress = "شروع ترمیم (حالت کامل)..."
                    scope.launch {
                        val res = diagnosisResult!!
                        val healedConfig = Diagnostician.healConfig(activity, res.config, Diagnostician.HealMode.DEEP) { progress ->
                            scope.launch(Dispatchers.Main) { healingProgress = progress }
                        }
                        if (healedConfig != null) {
                            healedLink = ConfigParserFactory.buildLink(healedConfig)
                        } else {
                            errorText = "❌ متاسفانه مسیر سالمی پیدا نشد."
                        }
                        isHealing = false
                        healingProgress = ""
                    }
                }
            )
        }
    }
}