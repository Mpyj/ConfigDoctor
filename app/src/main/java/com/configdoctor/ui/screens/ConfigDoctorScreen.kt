// توی همون فایل MainScreen.kt یا یه فایل جدید SingleConfigTab.kt
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
    var healingProgress by remember { mutableStateOf("") }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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

        OutlinedButton(
            onClick = { showHistory = !showHistory },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, subTextColor.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Rounded.History, contentDescription = null, tint = textColor)
            Spacer(Modifier.width(8.dp))
            Text(if (showHistory) "بستن تاریخچه" else "نمایش تاریخچه", color = textColor)
        }

        AnimatedVisibility(visible = showHistory) {
            HistorySection(
                historyList = historyList,
                onClear = { scope.launch { historyDao.clearAll() } },
                isDarkTheme = isDarkTheme
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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

        // ... (بقیه کدهای نمایش نتیجه و ترمیم دقیقاً مثل ConfigDoctorScreen قبلی) ...
        // برای جلوگیری از طولانی شدن، این بخش رو از کد قبلی کپی کن (از AnimatedVisibility دیاگنوز تا آخر)
    }
}