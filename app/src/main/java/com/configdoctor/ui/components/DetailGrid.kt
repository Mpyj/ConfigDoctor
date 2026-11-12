package com.configdoctor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun DetailGrid(res: DiagnosisResult) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailItemCard("نوع سرور", serverTypeLabel(res.serverType), Icons.Rounded.Dns, Color(0xFF2196F3), Modifier.weight(1f))
            DetailItemCard("IP سرور", res.resolvedIp ?: "-", Icons.Rounded.Public, Color(0xFF9C27B0), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailItemCard("TCP RTT", "${res.tcpLatencyMs ?: "-"} ms", Icons.Rounded.NetworkCheck, Color(0xFFFF9800), Modifier.weight(1f))
            DetailItemCard("Real Delay", "${res.realDelayMs ?: "-"} ms", Icons.Rounded.Speed, Color(0xFFE91E63), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val tlsText = when (res.tlsOk) {
                true -> "موفق"
                false -> "ناموفق"
                null -> "انجام نشد"
            }
            DetailItemCard("TLS", tlsText, Icons.Rounded.Security, Color(0xFF009688), Modifier.weight(1f))
            DetailItemCard("IP خروجی", res.outputIp ?: "-", Icons.Rounded.ExitToApp, Color(0xFF795548), Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DetailItemCard("کشور خروجی", formatCountry(res.outputCountry), Icons.Rounded.Flag, Color(0xFF3F51B5), Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun DetailItemCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(8.dp))
            SelectionContainer {
                Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray, maxLines = 1)
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