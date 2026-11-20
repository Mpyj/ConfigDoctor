package com.configdoctor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.diagnose.DiagnosisResult
import com.configdoctor.diagnose.ServerType
import com.configdoctor.diagnose.formatCountry

@Composable
fun DetailGrid(res: DiagnosisResult, isDarkTheme: Boolean) {
    val cardBg = if (isDarkTheme) Color(0xFF16213E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.DarkGray
    val subTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray
    val dividerColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Gray.copy(alpha = 0.15f)

    var showMtuDialog by remember { mutableStateOf(false) }
    var showDnsDialog by remember { mutableStateOf(false) }

    val pl = res.packetLoss
    val plText = if (pl != null) "${"%.0f".format(pl.lossPercent)}% (${pl.received}/${pl.sent})" else "-"
    val plColor = when {
        pl == null -> subTextColor
        pl.lossPercent <= 5f -> Color(0xFF4CAF50)
        pl.lossPercent <= 20f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
    val tlsText = when (res.tlsOk) {
        true -> "موفق"
        false -> "ناموفق"
        null -> "انجام نشد"
    }
    val tlsColor = when (res.tlsOk) {
        true -> Color(0xFF4CAF50)
        false -> Color(0xFFF44336)
        null -> subTextColor
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Assessment, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("جزئیات فنی", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = textColor)
            }
            HorizontalDivider(color = dividerColor)

            DetailRow("نوع سرور", serverTypeLabel(res.serverType), Icons.Rounded.Dns, Color(0xFF2196F3), textColor, subTextColor)
            DetailRow("IP سرور", res.resolvedIp ?: "-", Icons.Rounded.Public, Color(0xFF9C27B0), textColor, subTextColor)
            DetailRow("TCP RTT", "${res.tcpLatencyMs ?: "-"} ms", Icons.Rounded.NetworkCheck, Color(0xFFFF9800), textColor, subTextColor)
            DetailRow("Real Delay", "${res.realDelayMs ?: "-"} ms", Icons.Rounded.Speed, Color(0xFFE91E63), textColor, subTextColor)
            DetailRow("TLS", tlsText, Icons.Rounded.Security, tlsColor, textColor, subTextColor)
            DetailRow("IP خروجی", res.outputIp ?: "-", Icons.Rounded.ExitToApp, Color(0xFF795548), textColor, subTextColor)
            DetailRow("کشور خروجی", formatCountry(res.outputCountry), Icons.Rounded.Flag, Color(0xFF3F51B5), textColor, subTextColor)
            
            // MTU (برای همه سرورها)
            if (res.mtu != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showMtuDialog = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.SettingsEthernet, contentDescription = null, tint = Color(0xFF009688), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("MTU بهینه", fontSize = 13.sp, color = subTextColor, modifier = Modifier.weight(1f))
                    Text("${res.mtu}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF009688))
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Rounded.Info, contentDescription = "راهنما", tint = Color(0xFF6C63FF), modifier = Modifier.size(16.dp))
                }
            }

            // DNS (برای همه سرورها - چون به کانفیگ ربطی نداره)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDnsDialog = true }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Dns, contentDescription = null, tint = Color(0xFF8BC34A), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text("تنظیم DNS", fontSize = 13.sp, color = subTextColor, modifier = Modifier.weight(1f))
                Text("پیشنهادی", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8BC34A))
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Rounded.Info, contentDescription = "راهنما", tint = Color(0xFF6C63FF), modifier = Modifier.size(16.dp))
            }

            if (pl != null) {
                HorizontalDivider(color = dividerColor, modifier = Modifier.padding(vertical = 4.dp))
                DetailRow("پکت‌لاس", plText, Icons.Rounded.NetworkPing, plColor, textColor, subTextColor, valueColor = plColor)
                DetailRow("Jitter", "${pl.jitterMs ?: "-"} ms", Icons.Rounded.Timeline, Color(0xFF673AB7), textColor, subTextColor)
                DetailRow("میانگین پینگ", "${pl.avgLatencyMs ?: "-"} ms", Icons.Rounded.Speed, Color(0xFF00BCD4), textColor, subTextColor)
            }
        }
    }

    // دیالوگ MTU
    if (showMtuDialog && res.mtu != null) {
        AlertDialog(
            onDismissRequest = { showMtuDialog = false },
            title = { Text("تنظیم MTU") },
            text = { 
                Text("عدد MTU بهینه برای این کانفیگ ${res.mtu} است.\n\n" +
                     "برای اعمال این تنظیم، به تنظیمات کلاینت خود (مثل v2rayNG یا NekoBox) بروید و مقدار MTU را روی ${res.mtu} تنظیم کنید.\n\n" +
                     "این کار باعث افزایش پایداری و سرعت اتصال می‌شود.")
            },
            confirmButton = {
                TextButton(onClick = { showMtuDialog = false }) { Text("متوجه شدم") }
            }
        )
    }

    // دیالوگ DNS
    if (showDnsDialog) {
        AlertDialog(
            onDismissRequest = { showDnsDialog = false },
            title = { Text("تنظیم DNS پیشنهادی") },
            text = { 
                Text("برای بهبود سرعت و امنیت، پیشنهاد می‌شود از DNSهای معتبر و سریع مثل شکن (Shecan) یا رادار (Radar) استفاده کنید.\n\n" +
                     "چطور تنظیم کنم؟\n" +
                     "۱. در تنظیمات گوشی به بخش Network & Internet بروید.\n" +
                     "۲. Private DNS را انتخاب کنید.\n" +
                     "۳. آدرس DNS مدنظر را وارد کنید (مثلاً برای شکن: free.shecan.ir)\n\n" +
                     "توجه: برای اعمال این تنظیم، نیازی به تغییر کانفیگ نیست و روی کل گوشی اعمال می‌شود.")
            },
            confirmButton = {
                TextButton(onClick = { showDnsDialog = false }) { Text("متوجه شدم") }
            }
        )
    }
}

@Composable
private fun DetailRow(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    textColor: Color,
    subTextColor: Color,
    valueColor: Color = textColor
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(title, fontSize = 13.sp, color = subTextColor, modifier = Modifier.weight(1f))
        SelectionContainer {
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor, maxLines = 1)
        }
    }
}

fun serverTypeLabel(type: ServerType): String = when (type) {
    ServerType.CLOUDFLARE -> "کلودفلر"
    ServerType.DATACENTER -> "دیتاسنتر / VPS"
    ServerType.DIRECT -> "سرور مستقیم"
    ServerType.UNKNOWN -> "نامشخص"
}