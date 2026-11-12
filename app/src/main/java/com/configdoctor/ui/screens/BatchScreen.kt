package com.configdoctor.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import com.configdoctor.ui.components.DetailGrid
import com.configdoctor.ui.components.StatusCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

@Composable
fun BatchTab(
    activity: MainActivity,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val historyDao = db.historyDao()

    var subUrl by remember { mutableStateOf("") }
    var linksText by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var isHealing by remember { mutableStateOf(false) }
    var isFetchingSub by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Pair<String, DiagnosisResult?>>>(emptyList()) }
    var showHealModeDialog by remember { mutableStateOf(false) }

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray

    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )

    val fetchSubscription: () -> Unit = {
        isFetchingSub = true
        scope.launch {
            val content = withContext(Dispatchers.IO) {
                try { URL(subUrl).readText() } catch (e: Exception) { null }
            }
            if (content != null) {
                val decoded = try { String(android.util.Base64.decode(content, android.util.Base64.DEFAULT)) } catch (e: Exception) { content }
                val configLines = decoded.lines().filter { it.startsWith("vless://") || it.startsWith("vmess://") || it.startsWith("trojan://") }
                if (configLines.isNotEmpty()) {
                    linksText = configLines.joinToString("\n")
                    Toast.makeText(context, "${configLines.size} کانفیگ دریافت شد", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "هیچ کانفیگی پیدا نشد", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "خطا در دریافت ساب", Toast.LENGTH_SHORT).show()
            }
            isFetchingSub = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = subUrl,
            onValueChange = { subUrl = it },
            label = { Text("لینک ساب (https://...)", color = subTextColor) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6C63FF), unfocusedBorderColor = subTextColor.copy(alpha = 0.3f), focusedTextColor = textColor, unfocusedTextColor = textColor),
            trailingIcon = {
                IconButton(onClick = { if (subUrl.isNotBlank()) fetchSubscription() }) {
                    Icon(Icons.Rounded.Download, contentDescription = "دریافت", tint = Color(0xFF6C63FF))
                }
            }
        )
        
        Spacer(Modifier.height(12.dp))
        Text("یا لینک‌ها را دستی وارد کنید:", fontSize = 12.sp, color = subTextColor)

        OutlinedTextField(
            value = linksText,
            onValueChange = { linksText = it },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            enabled = !isTesting && !isHealing && !isFetchingSub,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6C63FF), unfocusedBorderColor = subTextColor.copy(alpha = 0.3f), focusedTextColor = textColor, unfocusedTextColor = textColor)
        )

        Spacer(Modifier.height(16.dp))

        // نوار وضعیت (Progress)
        AnimatedVisibility(visible = isTesting || isHealing || isFetchingSub) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF16213E) else Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(20.dp).rotate(rotation), tint = Color(0xFF6C63FF))
                    Spacer(Modifier.width(12.dp))
                    Text(text = if (isFetchingSub) "دریافت ساب..." else progressText, color = textColor, fontSize = 14.sp)
                }
            }
        }

        Button(
            onClick = {
                if (subUrl.isNotBlank() && linksText.isBlank()) {
                    isFetchingSub = true
                    scope.launch {
                        val content = withContext(Dispatchers.IO) { try { URL(subUrl).readText() } catch (e: Exception) { null } }
                        if (content != null) {
                            val decoded = try { String(android.util.Base64.decode(content, android.util.Base64.DEFAULT)) } catch (e: Exception) { content }
                            val configLines = decoded.lines().filter { it.startsWith("vless://") || it.startsWith("vmess://") || it.startsWith("trojan://") }
                            if (configLines.isNotEmpty()) {
                                linksText = configLines.joinToString("\n")
                                isTesting = true
                                results = Diagnostician.diagnoseBatch(activity, configLines) { p -> progressText = p }
                                isTesting = false
                                progressText = ""
                            }
                        }
                        isFetchingSub = false
                    }
                } else if (linksText.isNotBlank()) {
                    isTesting = true
                    val links = linksText.lines().filter { it.isNotBlank() }
                    scope.launch {
                        results = Diagnostician.diagnoseBatch(activity, links) { p -> progressText = p }
                        isTesting = false
                        progressText = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isTesting && !isHealing && !isFetchingSub && (linksText.isNotBlank() || subUrl.isNotBlank()),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
        ) {
            Text("شروع تست گروهی", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        if (results.isNotEmpty()) {
            val healthyCount = results.count { it.second?.overallStatus == OverallStatus.HEALTHY }
            val deadCount = results.count { it.second?.overallStatus == OverallStatus.IP_DEAD || it.second?.overallStatus == OverallStatus.UNKNOWN }

            Text("خلاصه: سالم $healthyCount | خراب $deadCount | کل ${results.size}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
            Spacer(Modifier.height(8.dp))

            // لیست نتایج با کارت‌های تفصیلی
            results.forEach { (link, result) ->
                if (result != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            StatusCard(status = result.overallStatus, summary = result.summary)
                            Spacer(modifier = Modifier.height(8.dp))
                            DetailGrid(result)
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.2f))
                    ) {
                        Text("خطا در پارس: $link", color = Color.Red, modifier = Modifier.padding(8.dp))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // دکمه ترمیم گروهی (اصلاح‌شده)
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
                    val allLinks = results.mapNotNull { it.second?.config }.map { ConfigParserFactory.buildLink(it) }.joinToString("\n")
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
                    executeHealBatch(activity, results, Diagnostician.HealMode.DEEP, historyDao,
                        onStart = { isHealing = true },
                        onProgress = { p -> progressText = p },
                        onComplete = { newResults -> results = newResults; isHealing = false; progressText = "" }
                    )
                }) { Text("کامل", color = Color(0xFF6C63FF)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHealModeDialog = false
                    executeHealBatch(activity, results, Diagnostician.HealMode.QUICK, historyDao,
                        onStart = { isHealing = true },
                        onProgress = { p -> progressText = p },
                        onComplete = { newResults -> results = newResults; isHealing = false; progressText = "" }
                    )
                }) { Text("سریع", color = Color(0xFF6C63FF)) }
            }
        )
    }
}

// تابع کمکی برای اجرای ترمیم گروهی
private fun executeHealBatch(
    activity: MainActivity,
    currentResults: List<Pair<String, DiagnosisResult?>>,
    mode: Diagnostician.HealMode,
    historyDao: com.configdoctor.data.HistoryDao,
    onStart: () -> Unit,
    onProgress: (String) -> Unit,
    onComplete: (List<Pair<String, DiagnosisResult?>>) -> Unit
) {
    onStart()
    val configsToHeal = currentResults.mapNotNull { it.second?.config }
    val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
    
    scope.launch {
        val healedPairs = Diagnostician.healBatch(activity, configsToHeal, mode) { progress ->
            onProgress(progress)
        }
        
        var configIndex = 0
        val newResults = currentResults.map { pair ->
            if (pair.second != null) {
                val healed = healedPairs[configIndex].second
                configIndex++
                if (healed != null) {
                    // کانفیگ ترمیم شد، دوباره تشخیص بده تا اطلاعات جدید (Real Delay و...) آپدیت شه
                    val newDiag = Diagnostician.diagnose(activity, healed)
                    historyDao.insert(HistoryEntity(link = ConfigParserFactory.buildLink(healed), timestamp = System.currentTimeMillis(), status = "HEALED", outputIp = newDiag.outputIp, outputCountry = newDiag.outputCountry, realDelayMs = newDiag.realDelayMs))
                    pair.first to newDiag
                } else {
                    pair // ترمیم نشد، همون قبلی بمونه
                }
            } else {
                pair
            }
        }
        onComplete(newResults)
    }
}