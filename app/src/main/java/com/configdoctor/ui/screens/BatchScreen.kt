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

@Composable
fun BatchScreen(
    activity: MainActivity,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val db = remember { AppDatabase.getDatabase(context) }
    val historyDao = db.historyDao()

    var linksText by remember { mutableStateOf("") }
    var isTesting by remember { mutableStateOf(false) }
    var isHealing by remember { mutableStateOf(false) }
    var progressText by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Pair<String, DiagnosisResult?>>>(emptyList()) }
    var showHealModeDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("تست گروهی", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = null)
            }
        }
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = linksText,
            onValueChange = { linksText = it },
            label = { Text("هر خط یک لینک") },
            modifier = Modifier.fillMaxWidth().height(150.dp),
            minLines = 5,
            enabled = !isTesting && !isHealing
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
            modifier = Modifier.fillMaxWidth(),
            enabled = !isTesting && !isHealing && linksText.isNotBlank()
        ) {
            if (isTesting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(progressText)
            } else {
                Text("شروع تست گروهی")
            }
        }

        Spacer(Modifier.height(16.dp))

        if (results.isNotEmpty()) {
            // خلاصه‌ی نتایج
            val healthyCount = results.count { it.second?.overallStatus == OverallStatus.HEALTHY }
            val deadCount = results.count { it.second?.overallStatus == OverallStatus.IP_DEAD || it.second?.overallStatus == OverallStatus.UNKNOWN }

            Text("خلاصه نتایج", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("سالم: $healthyCount | خراب: $deadCount | کل: ${results.size}", fontSize = 14.sp)
            Spacer(Modifier.height(8.dp))

            results.forEach { (link, result) ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(link, fontSize = 12.sp, maxLines = 1)
                        if (result != null) {
                            Text("وضعیت: ${result.overallStatus}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Real Delay: ${result.realDelayMs ?: "-"} ms", fontSize = 12.sp)
                        } else {
                            Text("خطا در پارس یا تشخیص", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // دکمه ترمیم گروهی (با انتخاب حالت)
            Button(
                onClick = { showHealModeDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !isTesting && !isHealing
            ) {
                Text("ترمیم گروهی کانفیگ‌های خراب")
            }

            Spacer(Modifier.height(8.dp))

            // دکمه کپی همه
            OutlinedButton(
                onClick = {
                    val allLinks = results.mapNotNull { it.second?.config }
                        .map { ConfigParserFactory.buildLink(it) }
                        .joinToString("\n")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("batch_links", allLinks))
                    Toast.makeText(context, "همه لینک‌های سالم کپی شدند", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("کپی همه لینک‌ها")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // دکمه اشتراک‌گذاری
            OutlinedButton(
                onClick = {
                    val allLinks = results.mapNotNull { it.second?.config }
                        .map { ConfigParserFactory.buildLink(it) }
                        .joinToString("\n")
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, allLinks)
                    }
                    context.startActivity(Intent.createChooser(intent, "اشتراک‌گذاری لینک‌ها"))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("اشتراک‌گذاری همه لینک‌ها")
            }
        }
    }

    // دیالوگ انتخاب حالت ترمیم گروهی
    if (showHealModeDialog) {
        AlertDialog(
            onDismissRequest = { showHealModeDialog = false },
            title = { Text("حالت ترمیم گروهی") },
            text = { Text("حالت سریع سریع‌تره ولی حالت کامل دقیق‌تره.") },
            confirmButton = {
                TextButton(onClick = {
                    showHealModeDialog = false
                    performHealGroup(activity, results, Diagnostician.HealMode.DEEP, scope, historyDao) { newResults -> results = newResults; isHealing = false }
                }) { Text("کامل") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHealModeDialog = false
                    performHealGroup(activity, results, Diagnostician.HealMode.QUICK, scope, historyDao) { newResults -> results = newResults; isHealing = false }
                }) { Text("سریع") }
            }
        )
    }
}

// تابع کمکی برای انجام ترمیم گروهی
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
        // ذخیره در تاریخچه
        for ((link, res) in newResults) {
            if (res != null) {
                historyDao.insert(
                    HistoryEntity(
                        link = link,
                        timestamp = System.currentTimeMillis(),
                        status = res.overallStatus.name,
                        outputIp = res.outputIp,
                        outputCountry = res.outputCountry,
                        realDelayMs = res.realDelayMs
                    )
                )
            }
        }
        onComplete(newResults)
    }
}