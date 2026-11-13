package com.configdoctor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.diagnose.DiagnosisResult
import com.configdoctor.diagnose.OverallStatus
import com.configdoctor.diagnose.formatCountry

@Composable
fun CompactResultCard(
    result: DiagnosisResult,
    isDarkTheme: Boolean,
    onCopy: () -> Unit
) {
    val (statusColor, statusIcon, statusText) = when (result.overallStatus) {
        OverallStatus.HEALTHY -> Triple(Color(0xFF4CAF50), Icons.Rounded.CheckCircle, "سالم")
        OverallStatus.SNI_FILTERED -> Triple(Color(0xFFFFC107), Icons.Rounded.Warning, "فیلتر SNI")
        OverallStatus.IP_DEAD -> Triple(Color(0xFFF44336), Icons.Rounded.Error, "IP سوخته")
        else -> Triple(Color(0xFF9E9E9E), Icons.Rounded.Info, "نامشخص")
    }

    val cardBg = if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black
    val subTextColor = if (isDarkTheme) Color.White.copy(alpha = 0.6f) else Color.Gray

    val pl = result.packetLoss
    val plText = if (pl != null) "${"%.0f".format(pl.lossPercent)}%" else "-"
    val plColor = when {
        pl == null -> textColor
        pl.lossPercent <= 5f -> Color(0xFF4CAF50)
        pl.lossPercent <= 20f -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // ردیف اول: وضعیت و دکمه کپی
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(statusIcon, contentDescription = null, tint = statusColor, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(statusText, color = statusColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی", tint = subTextColor, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // ردیف دوم: ۲ ستون
            Row(modifier = Modifier.fillMaxWidth()) {
                MiniInfo("Real Delay", "${result.realDelayMs ?: "-"} ms", textColor, subTextColor, Modifier.weight(1f))
                MiniInfo("پکت‌لاس", plText, plColor, subTextColor, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            // ردیف سوم: ۲ ستون
            Row(modifier = Modifier.fillMaxWidth()) {
                MiniInfo("IP خروجی", result.outputIp ?: "-", textColor, subTextColor, Modifier.weight(1f))
                MiniInfo("کشور", formatCountry(result.outputCountry), textColor, subTextColor, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MiniInfo(title: String, value: String, valueColor: Color, titleColor: Color, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(title, fontSize = 10.sp, color = titleColor)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = valueColor, maxLines = 1)
    }
}