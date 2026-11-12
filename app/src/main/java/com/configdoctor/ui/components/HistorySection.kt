package com.configdoctor.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.data.HistoryEntity

@Composable
fun HistorySection(
    historyList: List<HistoryEntity>,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تاریخچه تست‌ها", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (historyList.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Rounded.Delete, contentDescription = "پاک کردن تاریخچه", tint = Color.Red)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            if (historyList.isEmpty()) {
                Text("هنوز تستی انجام نشده.", color = Color.Gray)
            } else {
                historyList.take(10).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = item.status, fontWeight = FontWeight.Bold)
                        Text(text = item.outputIp ?: "-", fontSize = 12.sp)
                        Text(text = "${item.realDelayMs ?: "-"} ms", fontSize = 12.sp)
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}