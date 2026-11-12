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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.diagnose.OverallStatus

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