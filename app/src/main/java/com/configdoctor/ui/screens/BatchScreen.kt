package com.configdoctor.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Base64
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.MainActivity
import com.configdoctor.R
import com.configdoctor.data.AppDatabase
import com.configdoctor.data.HistoryEntity
import com.configdoctor.diagnose.DiagnosisResult
import com.configdoctor.diagnose.Diagnostician
import com.configdoctor.diagnose.OverallStatus
import com.configdoctor.parser.ConfigParserFactory
import com.configdoctor.ui.components.CompactResultCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

enum class BatchFilter { ALL, HEALTHY, DEAD }
enum class BatchSort { DEFAULT, DELAY, PACKET_LOSS }

@Composable
fun BatchTab(activity: MainActivity, isDarkTheme: Boolean) {
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
    var progressPercentage by remember { mutableStateOf(0f) }
    var results by remember { mutableStateOf<List<Pair<String, DiagnosisResult?>>>(emptyList()) }
    var healStatuses by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var showHealModeDialog by remember { mutableStateOf(false) }

    var filter by remember { mutableStateOf(BatchFilter.ALL) }
    var sort by remember { mutableStateOf(BatchSort.DEFAULT) }
    var searchQuery by remember { mutableStateOf("") }

    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray

    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(1500, easing = LinearEasing), repeatMode = RepeatMode.Restart),
        label = "rotation"
    )

    val filteredResults = remember(results, filter, sort, searchQuery) {
        var list = results
        list = when (filter) {
            BatchFilter.HEALTHY -> list.filter { it.second?.overallStatus == OverallStatus.HEALTHY }
            BatchFilter.DEAD -> list.filter { it.second != null && it.second?.overallStatus != OverallStatus.HEALTHY }
            BatchFilter.ALL -> list
        }
        if (searchQuery.isNotBlank()) list = list.filter { (link, _) -> link.contains(searchQuery, ignoreCase = true) }
        list = when (sort) {
            BatchSort.DELAY -> list.sortedBy { it.second?.realDelayMs ?: Long.MAX_VALUE }
            BatchSort.PACKET_LOSS -> list.sortedBy { it.second?.packetLoss?.lossPercent ?: Float.MAX_VALUE }
            BatchSort.DEFAULT -> list
        }
        list
    }

    val fetchSubscription: () -> Unit = {
        isFetchingSub = true
        scope.launch {
            val content = withContext(Dispatchers.IO) { try { URL(subUrl).readText() } catch (e: Exception) { null } }
            if (content != null) {
                val decoded = try { String(Base64.decode(content, Base64.DEFAULT)) } catch (e: Exception) { content }
                val configLines = decoded.lines().filter { it.startsWith("vless://") || it.startsWith("vmess://") || it.startsWith("trojan://") }
                if (configLines.isNotEmpty()) {
                    linksText = configLines.joinToString("\n")
                    Toast.makeText(context, "${configLines.size} configs", Toast.LENGTH_SHORT).show()
                } else Toast.makeText(context, "No configs", Toast.LENGTH_SHORT).show()
            } else Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
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
            label = { Text(stringResource(R.string.sub_link), color = subTextColor) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6C63FF), unfocusedBorderColor = subTextColor.copy(alpha = 0.3f), focusedTextColor = textColor, unfocusedTextColor = textColor),
            trailingIcon = {
                IconButton(onClick = { if (subUrl.isNotBlank()) fetchSubscription() }) {
                    Icon(Icons.Rounded.Download, contentDescription = "Get", tint = Color(0xFF6C63FF))
                }
            }
        )

        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.or_manual), fontSize = 12.sp, color = subTextColor)

        OutlinedTextField(
            value = linksText,
            onValueChange = { linksText = it },
            modifier = Modifier.fillMaxWidth().height(120.dp),
            enabled = !isTesting && !isHealing && !isFetchingSub,
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6C63FF), unfocusedBorderColor = subTextColor.copy(alpha = 0.3f), focusedTextColor = textColor, unfocusedTextColor = textColor)
        )

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(visible = isTesting || isHealing || isFetchingSub) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF16213E) else Color.White),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Autorenew, contentDescription = null, modifier = Modifier.size(20.dp).rotate(rotation), tint = Color(0xFF6C63FF))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text = if (isFetchingSub) "..." else progressText, color = textColor, fontSize = 14.sp)
                    }
                    if (isTesting || isHealing) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { progressPercentage }, modifier = Modifier.fillMaxWidth().height(6.dp), color = if (isHealing) Color(0xFF4CAF50) else Color(0xFF6C63FF), trackColor = Color.Gray.copy(alpha = 0.3f))
                    }
                }
            }
        }

        Button(
            onClick = {
                progressPercentage = 0f
                healStatuses = emptyMap()
                if (subUrl.isNotBlank() && linksText.isBlank()) {
                    isFetchingSub = true
                    scope.launch {
                        val content = withContext(Dispatchers.IO) { try { URL(subUrl).readText() } catch (e: Exception) { null } }
                        if (content != null) {
                            val decoded = try { String(Base64.decode(content, Base64.DEFAULT)) } catch (e: Exception) { content }
                            val configLines = decoded.lines().filter { it.startsWith("vless://") || it.startsWith("vmess://") || it.startsWith("trojan://") }
                            if (configLines.isNotEmpty()) {
                                linksText = configLines.joinToString("\n")
                                isTesting = true
                                results = Diagnostician.diagnoseBatch(activity, configLines) { p, percent -> progressText = p; progressPercentage = percent }
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
                        results = Diagnostician.diagnoseBatch(activity, links) { p, percent -> progressText = p; progressPercentage = percent }
                        isTesting = false
                        progressText = ""
                    }
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            enabled = !isTesting && !isHealing && !isFetchingSub && (linksText.isNotBlank() || subUrl.isNotBlank()),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
        ) {
            Text(stringResource(R.string.start_batch), fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        if (results.isNotEmpty()) {
            val healthyCount = results.count { it.second?.overallStatus == OverallStatus.HEALTHY }
            val deadCount = results.count { it.second?.overallStatus == OverallStatus.IP_DEAD || it.second?.overallStatus == OverallStatus.UNKNOWN }
            val snifilteredCount = results.count { it.second?.overallStatus == OverallStatus.SNI_FILTERED }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryChip(label = stringResource(R.string.healthy), count = healthyCount, color = Color(0xFF4CAF50), bg = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White, modifier = Modifier.weight(1f))
                SummaryChip(label = stringResource(R.string.dead), count = deadCount, color = Color(0xFFF44336), bg = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White, modifier = Modifier.weight(1f))
                SummaryChip(label = stringResource(R.string.sni), count = snifilteredCount, color = Color(0xFFFFC107), bg = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))

            ScrollableTabRow(
                selectedTabIndex = filter.ordinal,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF6C63FF),
                edgePadding = 0.dp,
                divider = {}
            ) {
                BatchFilter.entries.forEach { f ->
                    Tab(
                        selected = filter == f,
                        onClick = { filter = f },
                        text = {
                            Text(
                                when (f) {
                                    BatchFilter.ALL -> "${stringResource(R.string.all)} (${results.size})"
                                    BatchFilter.HEALTHY -> "${stringResource(R.string.healthy)} ($healthyCount)"
                                    BatchFilter.DEAD -> "${stringResource(R.string.dead)} (${deadCount + snifilteredCount})"
                                },
                                fontSize = 12.sp, fontWeight = FontWeight.Bold
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search), fontSize = 12.sp, color = subTextColor) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = subTextColor, modifier = Modifier.size(18.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6C63FF), unfocusedBorderColor = subTextColor.copy(alpha = 0.3f), focusedTextColor = textColor, unfocusedTextColor = textColor)
                )
                IconButton(onClick = {
                    sort = when (sort) {
                        BatchSort.DEFAULT -> BatchSort.DELAY
                        BatchSort.DELAY -> BatchSort.PACKET_LOSS
                        BatchSort.PACKET_LOSS -> BatchSort.DEFAULT
                    }
                }) {
                    Icon(
                        when (sort) {
                            BatchSort.DELAY -> Icons.Rounded.Speed
                            BatchSort.PACKET_LOSS -> Icons.Rounded.NetworkPing
                            else -> Icons.Rounded.Sort
                        },
                        contentDescription = "Sort",
                        tint = Color(0xFF6C63FF)
                    )
                }
            }

            Text(
                text = when (sort) {
                    BatchSort.DELAY -> stringResource(R.string.sort_delay)
                    BatchSort.PACKET_LOSS -> stringResource(R.string.sort_packet_loss)
                    BatchSort.DEFAULT -> stringResource(R.string.sort_default)
                },
                fontSize = 10.sp, color = subTextColor, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )

            Spacer(Modifier.height(8.dp))

            if (filteredResults.isEmpty()) {
                Text(stringResource(R.string.no_result), color = subTextColor, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
            } else {
                filteredResults.forEach { (link, result) ->
                    val statusText = healStatuses[link]
                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        if (result != null) {
                            CompactResultCard(result = result, isDarkTheme = isDarkTheme, onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("config_link", ConfigParserFactory.buildLink(result.config)))
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            })
                        } else {
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFD32F2F).copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Error, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Error: ${link.take(30)}...", color = Color.Red, fontSize = 12.sp)
                                }
                            }
                        }
                        if (statusText != null) {
                            Text(
                                text = statusText, fontSize = 11.sp,
                                color = if (statusText.startsWith("✅")) Color(0xFF4CAF50) else if (statusText.startsWith("❌")) Color(0xFFF44336) else Color(0xFFFFC107),
                                modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { healStatuses = emptyMap(); showHealModeDialog = true },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                enabled = !isTesting && !isHealing
            ) {
                Text(stringResource(R.string.heal_batch), fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val allLinks = filteredResults.mapNotNull { it.second?.config }.map { ConfigParserFactory.buildLink(it) }.joinToString("\n")
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("batch_links", allLinks))
                    Toast.makeText(context, "${filteredResults.size} links", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C63FF))
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = Color(0xFF6C63FF))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.copy_filtered, filteredResults.size), color = Color(0xFF6C63FF))
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = {
                    val allLinks = filteredResults.mapNotNull { it.second?.config }.map { ConfigParserFactory.buildLink(it) }.joinToString("\n")
                    val base64Sub = Base64.encodeToString(allLinks.toByteArray(), Base64.NO_WRAP)
                    val intent = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, base64Sub) }
                    context.startActivity(Intent.createChooser(intent, "Share"))
                },
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6C63FF))
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null, tint = Color(0xFF6C63FF))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.share_filtered), color = Color(0xFF6C63FF))
            }
        }
    }

    if (showHealModeDialog) {
        AlertDialog(
            onDismissRequest = { showHealModeDialog = false },
            title = { Text("Heal Batch", color = textColor) },
            text = { Text("Deep is more accurate but takes longer.", color = textColor) },
            confirmButton = {
                TextButton(onClick = {
                    showHealModeDialog = false
                    executeHealBatch(activity, results, Diagnostician.HealMode.DEEP, historyDao,
                        onStart = { isHealing = true; progressPercentage = 0f },
                        onProgress = { p, percent -> progressText = p; progressPercentage = percent },
                        onStatusUpdate = { link, status -> healStatuses = healStatuses + (link to status) },
                        onComplete = { newResults -> results = newResults; isHealing = false; progressText = ""; progressPercentage = 0f }
                    )
                }) { Text("Deep", color = Color(0xFF6C63FF)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showHealModeDialog = false
                    executeHealBatch(activity, results, Diagnostician.HealMode.QUICK, historyDao,
                        onStart = { isHealing = true; progressPercentage = 0f },
                        onProgress = { p, percent -> progressText = p; progressPercentage = percent },
                        onStatusUpdate = { link, status -> healStatuses = healStatuses + (link to status) },
                        onComplete = { newResults -> results = newResults; isHealing = false; progressText = ""; progressPercentage = 0f }
                    )
                }) { Text("Quick", color = Color(0xFF6C63FF)) }
            }
        )
    }
}

@Composable
private fun SummaryChip(label: String, count: Int, color: Color, bg: Color, modifier: Modifier = Modifier) {
    Card(colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$count", color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(label, color = color.copy(alpha = 0.8f), fontSize = 10.sp)
        }
    }
}

private fun executeHealBatch(
    activity: MainActivity, currentResults: List<Pair<String, DiagnosisResult?>>, mode: Diagnostician.HealMode,
    historyDao: com.configdoctor.data.HistoryDao, onStart: () -> Unit, onProgress: (String, Float) -> Unit,
    onStatusUpdate: (String, String) -> Unit, onComplete: (List<Pair<String, DiagnosisResult?>>) -> Unit
) {
    onStart()
    val configsToHeal = currentResults.mapNotNull { it.second?.config }
    val total = configsToHeal.size
    var current = 0

    val scope = kotlinx.coroutines.CoroutineScope(Dispatchers.Main)
    scope.launch {
        val healedPairs = Diagnostician.healBatch(activity, configsToHeal, mode) { progress, percent -> onProgress(progress, percent) }
        var configIndex = 0
        val newResults = currentResults.map { pair ->
            val originalLink = pair.first
            if (pair.second != null) {
                current++
                onStatusUpdate(originalLink, "⏳ Healing...")
                val percent = current.toFloat() / total.toFloat()
                onProgress("Healing ${current} of ${total}...", percent)
                val healed = healedPairs[configIndex].second
                configIndex++
                if (healed != null) {
                    val newDiag = Diagnostician.diagnose(activity, healed)
                    historyDao.insert(HistoryEntity(link = ConfigParserFactory.buildLink(healed), timestamp = System.currentTimeMillis(), status = "HEALED", outputIp = newDiag.outputIp, outputCountry = newDiag.outputCountry, realDelayMs = newDiag.realDelayMs, packetLossPercent = newDiag.packetLoss?.lossPercent, jitterMs = newDiag.packetLoss?.jitterMs, avgPingMs = newDiag.packetLoss?.avgLatencyMs))
                    onStatusUpdate(originalLink, "✅ Healed (${newDiag.realDelayMs ?: "-"}ms)")
                    originalLink to newDiag
                } else {
                    onStatusUpdate(originalLink, "❌ No healthy IP found")
                    pair
                }
            } else { pair }
        }
        onComplete(newResults)
    }
}