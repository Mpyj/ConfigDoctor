package com.configdoctor.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.diagnose.SpeedResult
import com.configdoctor.diagnose.SpeedTester
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(onBack: () -> Unit) {
    var isTesting by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SpeedResult?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تست سرعت", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (result == null && !isTesting) {
                Icon(Icons.Rounded.Speed, contentDescription = null, modifier = Modifier.size(100.dp), tint = Color(0xFF6C63FF).copy(alpha = 0.3f))
                Spacer(Modifier.height(16.dp))
                Text("سرعت اینترنت خود را تست کنید", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("تست دانلود و آپلود ۱ مگابایتی", fontSize = 14.sp, color = Color.Gray)
            }

            Spacer(Modifier.height(32.dp))

            if (isTesting) {
                CircularProgressIndicator(color = Color(0xFF6C63FF))
                Spacer(Modifier.height(16.dp))
                Text("در حال تست...", fontSize = 16.sp)
            }

            if (result != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SpeedCard(
                        title = "دانلود",
                        speed = result?.downloadMbps,
                        icon = Icons.Rounded.Download,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    SpeedCard(
                        title = "آپلود",
                        speed = result?.uploadMbps,
                        icon = Icons.Rounded.Upload,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { result = null }) {
                    Text("تست مجدد")
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    isTesting = true
                    result = null
                    scope.launch {
                        result = SpeedTester.testSpeed()
                        isTesting = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isTesting,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                Text(if (isTesting) "در حال تست..." else "شروع تست", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SpeedCard(title: String, speed: Float?, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
            Spacer(Modifier.height(8.dp))
            Text(title, fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (speed != null) "%.2f Mbps".format(speed) else "-",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}