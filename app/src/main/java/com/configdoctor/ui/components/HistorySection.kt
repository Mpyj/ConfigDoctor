package com.configdoctor.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.data.HistoryEntity

@Composable
fun HistorySection(
    historyList: List<HistoryEntity>,
    onClear: () -> Unit,
    isDarkTheme: Boolean
) {
    val context = LocalContext.current
    val cardColor = if (isDarkTheme) Color(0xFF16213E) else Color.White
    val textColor = if (isDarkTheme) Color.White else Color.Black

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("تاریخچه تست‌ها", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                if (historyList.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Rounded.Delete, contentDescription = "پاک کردن", tint = Color.Red)
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = item.status, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = textColor)
                            Text(text = item.outputIp ?: "-", fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(text = "${item.realDelayMs ?: "-"} ms", fontSize = 12.sp, color = Color.Gray)
                        
                        // دکمه کپی برای کانفیگ‌های سالم یا ترمیم‌شده
                        if (item.status == "HEALTHY" || item.status == "HEALED") {
                            IconButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("history_link", item.link))
                                Toast.makeText(context, "لینک کپی شد", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "کپی", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                }
            }
        }
    }
}