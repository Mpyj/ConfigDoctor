package com.configdoctor

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.diagnose.Diagnostician
import com.configdoctor.parser.VlessParser
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                MaterialTheme {
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
    var resultText by remember { mutableStateOf("نتیجه اینجا نمایش داده می‌شود...") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = activity.applicationContext

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دستیار هوشمند کانفیگ", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = link,
                onValueChange = { link = it },
                label = { Text("لینک VLESS را پیست کنید") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (link.isBlank()) {
                        resultText = "لطفاً یه لینک وارد کن"
                        return@Button
                    }
                    isLoading = true
                    resultText = "در حال تشخیص...\n(ممکنه چند ثانیه طول بکشه)"
                    scope.launch {
                        val parsed = VlessParser.parse(link)
                        val text = parsed.fold(
                            onSuccess = { config ->
                                try {
                                    val diag = Diagnostician.diagnose(activity, config)
                                    diag.summary
                                } catch (e: Exception) {
                                    "خطا در تشخیص: ${e.message}"
                                }
                            },
                            onFailure = { "خطا در پارس لینک: ${it.message}" }
                        )
                        resultText = text
                        isLoading = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isLoading) "لطفاً صبر کنید..." else "شروع تشخیص")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // کارت نتیجه
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 گزارش وضعیت",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (resultText.length > 10 && !isLoading) {
                            TextButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("result", resultText))
                            }) {
                                Text("کپی")
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = resultText,
                        fontSize = 15.sp,
                        lineHeight = 24.sp
                    )
                }
            }
        }
    }
}