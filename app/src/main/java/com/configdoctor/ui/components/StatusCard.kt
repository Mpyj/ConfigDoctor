package com.configdoctor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.diagnose.DiagnosisResult
import com.configdoctor.diagnose.OverallStatus

@Composable
fun StatusCard(res: DiagnosisResult, isDarkTheme: Boolean) {
    val status = res.overallStatus
    val (color, icon, title) = when (status) {
        OverallStatus.HEALTHY -> Triple(Color(0xFF4CAF50), Icons.Rounded.CheckCircle, "کانفیگ سالم است")
        OverallStatus.SNI_FILTERED -> Triple(Color(0xFFFFC107), Icons.Rounded.Warning, "احتمال فیلتر SNI")
        OverallStatus.IP_DEAD -> Triple(Color(0xFFF44336), Icons.Rounded.Error, "سرور پاسخ نمی‌دهد")
        else -> Triple(Color(0xFF9E9E9E), Icons.Rounded.Info, "وضعیت نامشخص")
    }

    val cardBg = if (isDarkTheme) Color(0xFF16213E) else Color.White
    val textColor = if (isDarkTheme) Color(0xFFE0E0E0) else Color.DarkGray

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(colors = listOf(color.copy(alpha = 0.12f), Color.Transparent)))
        ) {
            Column {
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
                            Text(text = res.summary, fontSize = 13.sp, color = textColor, lineHeight = 20.sp)
                        }
                    }
                }

                // ─── هشدار و تنظیمات Fragment ───
                if (res.needsFragment) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFC107).copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Warning, contentDescription = null, tint = Color(0xFFFFC107))
                                Spacer(Modifier.width(8.dp))
                                Text("نیاز به Fragment", fontWeight = FontWeight.Bold, color = Color(0xFFFFC107), fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "احتمالاً DPI ترافیک را بلاک می‌کند. در تنظیمات کلاینت (v2rayNG) گزینه Fragment را فعال کنید.",
                                fontSize = 11.sp,
                                color = if (isDarkTheme) Color.White.copy(alpha = 0.8f) else Color.DarkGray
                            )
                            
                            // نمایش تنظیمات پیشنهادی
                            res.fragmentRecommendation?.let { rec ->
                                Spacer(Modifier.height(8.dp))
                                Surface(
                                    color = Color.Black.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("تنظیمات پیشنهادی:", fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
                                        Text("Packets: ${rec.packets}", fontSize = 11.sp, color = Color.White)
                                        Text("Length: ${rec.length}", fontSize = 11.sp, color = Color.White)
                                        Text("Interval: ${rec.interval}", fontSize = 11.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.horizontalGradient(listOf(color, color.copy(alpha = 0.2f))))
            )
        }
    }
}