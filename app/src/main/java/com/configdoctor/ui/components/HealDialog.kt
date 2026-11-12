package com.configdoctor.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun HealModeDialog(
    onDismiss: () -> Unit,
    onQuick: () -> Unit,
    onDeep: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب حالت ترمیم", fontWeight = FontWeight.Bold) },
        text = {
            Text("• حالت سریع: فقط ۲-۳ ثانیه.\n• حالت کامل: دقیق‌ترین (تا ۳۰ ثانیه).", fontSize = 13.sp)
        },
        confirmButton = {
            TextButton(onClick = onQuick) { Text("ترمیم سریع") }
        },
        dismissButton = {
            Button(onClick = onDeep) { Text("ترمیم کامل") }
        }
    )
}