package com.configdoctor.ui.screens

import android.content.res.Resources
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.configdoctor.R

@Composable
fun OnboardingScreen(
    resources: Resources,
    onFinish: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    val totalPages = 3

    val pages = listOf(
        Triple(
            Icons.Rounded.MedicalServices,
            R.string.onboarding_title_1,
            R.string.onboarding_desc_1
        ),
        Triple(
            Icons.Rounded.Troubleshoot,
            R.string.onboarding_title_2,
            R.string.onboarding_desc_2
        ),
        Triple(
            Icons.Rounded.Build,
            R.string.onboarding_title_3,
            R.string.onboarding_desc_3
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Skip button
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onFinish) {
                Text(resources.getString(R.string.onboarding_skip), color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        }

        // Content
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding"
        ) { currentPage ->
            val (icon, titleRes, descRes) = pages[currentPage]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .background(Color(0xFF6C63FF).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color(0xFF6C63FF)
                    )
                }
                Spacer(Modifier.height(32.dp))
                Text(
                    resources.getString(titleRes),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    resources.getString(descRes),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )
            }
        }

        // Dots + Button
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                repeat(totalPages) { i ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(if (i == page) 24.dp else 8.dp, 8.dp)
                            .background(
                                if (i == page) Color(0xFF6C63FF) else Color.Gray.copy(alpha = 0.3f),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    if (page < totalPages - 1) page++ else onFinish()
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6C63FF))
            ) {
                Text(
                    if (page < totalPages - 1) resources.getString(R.string.onboarding_next) else resources.getString(R.string.onboarding_start),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}