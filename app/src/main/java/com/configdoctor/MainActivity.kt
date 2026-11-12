package com.configdoctor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.diagnose.DiagnosisResult
import com.configdoctor.diagnose.Diagnostician
import com.configdoctor.diagnose.OverallStatus
import com.configdoctor.diagnose.ServerType
import com.configdoctor.parser.VlessParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme(
                    colorScheme = lightColorScheme(
                        primary = Color(0xFF6C63FF),
                        secondary = Color(0xFF00BFA5),
                        background = Color(0xFFF8F9FA),
                        surface = Color.White
                    )
                ) {
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
    var healingProgress by remember { mutableStateOf("") }
    var showHealDialog by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    val context = activity.applicationContext
    val scrollState = rememberScrollState()

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "دستیار هوشمند کانفیگ", 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            MaterialTheme.colorScheme.background
                        ),
                        startY = 0f,
                        endY = 800f
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = Color.White.copy(alpha = 0.95f))
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "لینک کانفیگ خود را وارد کنید",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = link,
                            onValueChange = { link = it },
                            placeholder = { Text("vless://...", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && !isHealing,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                ) {
                    if (isLoading) {
                        Icon(
                            Icons.Rounded.Autorenew,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotation)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        if (isLoading) "در حال بررسی..." else "شروع تشخیص",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                AnimatedVisibility(
                    visible = errorText != null,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = errorText ?: "",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = diagnosisResult == null && !isLoading && errorText == null,
                    enter = fadeIn()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.Troubleshoot,
                            contentDescription = null,
                            modifier = Modifier.size(100.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "آماده برای تشخیص",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "لینک کانفیگ خود را وارد کنید تا\nوضعیت سلامت آن را بررسی کنیم.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                AnimatedVisibility(
                    visible = diagnosisResult != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    Column {
                        val res = diagnosisResult!!
                        
                        StatusCard(status = res.overallStatus, summary = res.summary)
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        DetailGrid(res)
                        
                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("result", res.summary))
                                    Toast.makeText(context, "گزارش کپی شد", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("کپی گزارش", fontWeight = FontWeight.Bold)
                            }

                            if (res.overallStatus == OverallStatus.IP_DEAD || res.overallStatus == OverallStatus.UNKNOWN) {
                                Button(
                                    onClick = {
                                        showHealDialog = true
                                    },
                                    modifier = Modifier.weight(1f).height(50.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = !isHealing,
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                ) {
                                    if (isHealing) {
                                        Icon(
                                            Icons.Rounded.Autorenew,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp).rotate(rotation)
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("ترمیم خودکار", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // نمایش پیشرفت ترمیم
                        AnimatedVisibility(visible = isHealing && healingProgress.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "در حال ترمیم...",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = healingProgress,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(
                    visible = healedLink != null,
                    enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("کانفیگ با موفقیت ترمیم شد!", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32), fontSize = 18.sp)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                color = Color.White,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            ) {
                                SelectionContainer {
                                    Text(
                                        text = healedLink ?: "",
                                        fontSize = 13.sp,
                                        color = Color.Black,
                                        modifier = Modifier.padding(12.dp)
                                    )
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
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("کپی لینک جدید", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(30.dp))
            }
        }

        // دیالوگ انتخاب حالت ترمیم (اصلاح‌شده)
        if (showHealDialog && diagnosisResult != null) {
            AlertDialog(
                onDismissRequest = { showHealDialog = false },
                title = { Text("انتخاب حالت ترمیم", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("چطور کانفیگ رو ترمیم کنیم؟", fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "• حالت سریع: فقط ۲-۳ ثانیه طول می‌کشه (تست TCP).",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Text(
                            "• حالت کامل: دقیق‌ترین نتیجه رو می‌ده ولی ممکنه تا ۳۰ ثانیه طول بکشه (تست Real Delay).",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    // دکمه ترمیم سریع (اول اجرا میشه)
                    TextButton(
                        onClick = {
                            showHealDialog = false
                            isHealing = true
                            healingProgress = "شروع ترمیم (حالت سریع)..."
                            scope.launch {
                                val res = diagnosisResult!!
                                val healedConfig = Diagnostician.healConfig(
                                    activity, res.config, Diagnostician.HealMode.QUICK
                                ) { progress ->
                                    scope.launch(Dispatchers.Main) { healingProgress = progress }
                                }
                                if (healedConfig != null) {
                                    healedLink = VlessParser.buildLink(healedConfig)
                                } else {
                                    errorText = "❌ متاسفانه مسیر سالمی پیدا نشد."
                                }
                                isHealing = false
                                healingProgress = ""
                            }
                        }
                    ) {
                        Text("ترمیم سریع")
                    }
                },
                dismissButton = {
                    // دکمه ترمیم کامل (دوم اجرا میشه)
                    Button(
                        onClick = {
                            showHealDialog = false
                            isHealing = true
                            healingProgress = "شروع ترمیم (حالت کامل)..."
                            scope.launch {
                                val res = diagnosisResult!!
                                val healedConfig = Diagnostician.healConfig(
                                    activity, res.config, Diagnostician.HealMode.DEEP
                                ) { progress ->
                                    scope.launch(Dispatchers.Main) { healingProgress = progress }
                                }
                                if (healedConfig != null) {
                                    healedLink = VlessParser.buildLink(healedConfig)
                                } else {
                                    errorText = "❌ متاسفانه مسیر سالمی پیدا نشد."
                                }
                                isHealing = false
                                healingProgress = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("ترمیم کامل (بهترین)")
                    }
                }
            )
        }
    }
}

@Composable
fun StatusCard(status: OverallStatus, summary: String) {
    val (color, icon, title) = when (status) {
        OverallStatus.HEALTHY -> Triple(Color(0xFF4CAF50), Icons.Rounded.CheckCircle, "کانفیگ سالم است")
        OverallStatus.SNI_FILTERED -> Triple(Color(0xFFFFC107), Icons.Rounded.Warning, "احتمال فیلتر SNI")
        OverallStatus.IP_DEAD -> Triple(Color(0xFFF44336), Icons.Rounded.Error, "سرور پاسخ نمی‌دهد")
        else -> Triple(Color(0xFF9E9E9E), Icons.Rounded.Info, "وضعیت نامشخص")
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
                Spacer(modifier = Modifier.height(6.dp))
                SelectionContainer {
                    Text(text = summary, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 20.sp)
                }
            }
        }
    }
}

@Composable
fun DetailGrid(res: DiagnosisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailItemCard(
                title = "نوع سرور",
                value = serverTypeLabel(res.serverType),
                icon = Icons.Rounded.Dns,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            DetailItemCard(
                title = "IP سرور",
                value = res.resolvedIp ?: "-",
                icon = Icons.Rounded.Public,
                color = Color(0xFF9C27B0),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailItemCard(
                title = "TCP RTT",
                value = "${res.tcpLatencyMs ?: "-"} ms",
                icon = Icons.Rounded.NetworkCheck,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
            DetailItemCard(
                title = "Real Delay",
                value = "${res.realDelayMs ?: "-"} ms",
                icon = Icons.Rounded.Speed,
                color = Color(0xFFE91E63),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val tlsText = when (res.tlsOk) {
                true -> "موفق"
                false -> "ناموفق"
                null -> "انجام نشد"
            }
            DetailItemCard(
                title = "TLS",
                value = tlsText,
                icon = Icons.Rounded.Security,
                color = Color(0xFF009688),
                modifier = Modifier.weight(1f)
            )
            DetailItemCard(
                title = "IP خروجی",
                value = res.outputIp ?: "-",
                icon = Icons.Rounded.ExitToApp,
                color = Color(0xFF795548),
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailItemCard(
                title = "کشور خروجی",
                value = formatCountry(res.outputCountry), // اینجا پرچم اضافه میشه
                icon = Icons.Rounded.Flag,
                color = Color(0xFF3F51B5),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun DetailItemCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            SelectionContainer {
                Text(
                    text = value,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    maxLines = 1
                )
            }
        }
    }
}

fun serverTypeLabel(type: ServerType): String = when (type) {
    ServerType.CLOUDFLARE -> "کلودفلر"
    ServerType.DATACENTER -> "دیتاسنتر / VPS"
    ServerType.DIRECT -> "سرور مستقیم"
    ServerType.UNKNOWN -> "نامشخص"
}

/**
 * تبدیل کد کشور به نام فارسی به همراه پرچم
 * از روش تبدیل حروف انگلیسی به Regional Indicator Symbol استفاده می‌کنیم
 */
fun formatCountry(code: String?): String {
    if (code.isNullOrBlank()) return "-"
    
    val name = when (code.uppercase()) {
        "US" -> "آمریکا"
        "DE" -> "آلمان"
        "NL" -> "هلند"
        "FR" -> "فرانسه"
        "GB" -> "انگلستان"
        "TR" -> "ترکیه"
        "AE" -> "امارات"
        "CA" -> "کانادا"
        "FI" -> "فنلاند"
        "SE" -> "سوئد"
        "RU" -> "روسیه"
        "IR" -> "ایران"
        "SG" -> "سنگاپور"
        "JP" -> "ژاپن"
        "IN" -> "هند"
        "PL" -> "لهستان"
        "AT" -> "اتریش"
        "CH" -> "سوئیس"
        "IT" -> "ایتالیا"
        "ES" -> "اسپانیا"
        else -> code.uppercase()
    }
    
    // تولید پرچم از کد دو حرفی
    val flag = getFlagEmoji(code)
    
    return "$flag $name ($code)".trim()
}

/**
 * تبدیل کد دو حرفی کشور (مثل "US") به ایموجی پرچم (🇺🇸)
 */
fun getFlagEmoji(countryCode: String): String {
    if (countryCode.length != 2) return ""
    val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
    val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
}