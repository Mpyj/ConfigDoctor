package com.configdoctor.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun BatchScreen(
    activity: MainActivity,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val historyDao = db.historyDao()

    var linksText by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var isHealing by remember { mutableStateOf(false) }
    var isFetchingSub by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Pair<String, DiagnosisResult?>>>(emptyList()) }
    var showHealModeDialog by remember { mutableStateOf(false) }

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray

    // تابع دریافت ساب (اصلاح‌شده)
    val fetchSubscription: (String) -> Unit = { url ->
        isFetchingSub = true
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                try {
                    URL(url).readText()
                } catch (e: Exception) {
                    null
                }
            }
            if (content != null) {
                // محتوای ساب معمولاً Base64 هست، باید دیکد بشه
                val decoded = try {
                    String(android.util.Base64.decode(content, android.util.Base64.DEFAULT))
                } catch (e: Exception) {
                    content // اگه Base64 نبود، خودش رو استفاده کن
                }
                
                // فیلتر کردن خطوطی که واقعاً یه کانفیگ هستن
                val configLines = decoded.lines().filter { line ->
                    line.startsWith("vless://") || line.startsWith("vmess://") || line.startsWith("trojan://")
                }
                
                if (configLines.isNotEmpty()) {
                    linksText = configLines.joinToString("\n")
                    Toast.makeText(context, "${configLines.size} کانفیگ از ساب دریافت شد", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "هیچ کانفیگی توی ساب پیدا نشد", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "خطا در دریافت ساب", Toast.LENGTH_SHORT).show()
            }
            isFetchingSub = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(8.dp))

        // ورودی لینک ساب
        var subUrl by remember { mutableStateOf("") }
        OutlinedTextField(
            value = subUrl,
            onValueChange = { subUrl = it },
            label = { Text("لینک ساب (https://...)", color = subTextColor) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6C63FF),
                unfocusedBorderColor = subTextColor.copy(alpha = 0.3f),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            trailingIcon = {
                IconButton(onClick = { if (subUrl.isNotBlank()) fetchSubscription(subUrl) }) {
                    Icon(Icons.Rounded.Download, contentDescription = "دریافت ساب", tint = Color(0xFF6C63FF))
                }
            }
        )
        
        Spacer(Modifier.height(8.dp))

        if (isFetchingSub) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color(0xFF6C63FF))
        }

        Spacer(Modifier.height(12.dp))
        Text("یا لینک‌ها را دستی وارد کنید (هر خط یک لینک):", fontSize = 12.sp, color = subTextColor)

        OutlinedTextField(
            value = linksText,
            onValueChange = { linksText = it },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            minLines = 5,
            enabled = !isTesting && !isHealing && !isFetchingSub,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF6C63FF),
                unfocusedBorderColor = subTextColor.copy(alpha = 0.3f),
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            )
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                isTesting = true
                val links = linksText.lines().filter { it.isNotBlank() }
                scope.launch {
                    results = Diagnostician.diagnoseBatch(activity, links) { progress ->
                        progressText = progress
                    }
                    isTesting = false
                    progressText = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isTesting && !isHealing && linksText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(progressText)
            } else {
                Text("شروع تست گروهی", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(16.dp))

        if (results.isNotEmpty()) {
            val healthyCount = results.count { it.second?.overallStatus == OverallStatus.HEALTHY }
            val deadCount = results.count { it.second?.overallStatus == OverallStatus.IP_DEAD || it.second?.overallStatus == OverallStatus.UNKNOWN }

            Text("خلاصه نتایج: سالم $healthyCount | خراب $deadCount | کل ${results.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
            Spacer(Modifier.height(8.dp))

            results.forEach { (link, result) ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.White)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(link, fontSize = 12.sp, maxLines = 1, color = textColor)
                        if (result != null) {
                            Text("وضعیت: ${result.overallStatus}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                            Text("Real Delay: ${result.realDelayMs ?: "-"} ms", fontSize = 12.sp, color = subTextColor)
                        } else {
                            Text("خطا در پارس یا تشخیص", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { showHealModeDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !isTesting && !isHealing
            ) {
                Text("ترمیم گروهی کانفیگ‌های خراب", fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val allLinks = results.mapNotNull { it.second?.config }
                        .map { ConfigParserFactory.buildLink(it) }
                        .joinToString("\n")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("batch_links", allLinks))
                    Toast.makeText(context, "همه لینک‌ها کپی شدند", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C63FF))
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = Color(0xFF6C63FF))
                Spacer(Modifier.width(8.dp))
                Text("کپی همه لینک‌ها", color = Color(0xFF6C63FF))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val allLinks = results.mapNotNull { it.second?.config }
                        .map { ConfigParserFactory.buildLink(it) }
                        .joinToString("\n")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, allLinks)
                    }
                    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری"))
                },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C63FF))
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, tint = Color(0xFF6C63FF))
                Spacer(Modifier.width(8.dp))
                Text("اشتراک‌گذاری همه لینک‌ها", color = Color(0xFF6C63FF))
            }
        }
    }

    if (showHealModeDialog) {
        AlertDialog(
            onDismissRequest = { showHealModeDialog = false },
            title = { Text("حالت ترمیم گروهی", color = textColor) },
            text = { Text("حالت کامل دقیق‌تره ولی طولانی‌تره.", color = textColor) },
            confirmButton = {
                TextButton(onClick = {
                    showHealModeDialog = false
                    performHealGroup(activity, results, Diagnostician.HealMode.DEEP, scope, historyDao) { newResults -> results = newResults; isHealing = false }
                }) { Text("کامل", color = Color(0xFF6C63FF)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHealModeDialog = false
                    performHealGroup(activity, results, Diagnostician.HealMode.QUICK, scope, historyDao) { newResults -> results = newResults; isHealing = false }
                }) { Text("سریع", color = Color(0xFF6C63FF)) }
            }
        )
    }
}

private fun performHealGroup(
    activity: MainActivity,
    results: List<Pair<String, DiagnosisResult?>>,
    mode: Diagnostician.HealMode,
    scope: kotlinx.coroutines.CoroutineScope,
    historyDao: com.configdoctor.data.HistoryDao,
    onComplete: (List<Pair<String, DiagnosisResult?>>) -> Unit
) {
    val configsToHeal = results.mapNotNull { it.second?.config }
    scope.launch {
        val healedPairs = Diagnostician.healBatch(activity, configsToHeal, mode) { }
        val newResults = results.mapIndexed { index, pair ->
            if (index < healedPairs.size && healedPairs[index].second != null) {
                pair.first to pair.second?.copy(config = healedPairs[index].second!!)
            } else {
                pair
            }
        }
        for ((link, res) in newResults) {
            if (res != null) {
                historyDao.insert(HistoryEntity(link = link, timestamp = System.currentTimeMillis(), status = res.overallStatus.name, outputIp = res.outputIp, outputCountry = res.outputCountry, realDelayMs = res.realDelayMs))
            }
        }
        onComplete(newResults)
    }
}