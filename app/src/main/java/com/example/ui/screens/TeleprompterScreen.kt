package com.example.ui.screens

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Composable
fun TeleprompterScreen(
    viewModel: MainViewModel,
    projectId: Long,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activeProject by viewModel.activeProject.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    var isPlaying by remember { mutableStateOf(false) }
    var scrollSpeed by remember { mutableIntStateOf(viewModel.preferences.teleprompterSpeed) } // 1: Slow, 2: Med, 3: Fast, 4: Extra Fast
    var fontSizeSp by remember { mutableIntStateOf(viewModel.preferences.teleprompterFontSize) }
    var isMirrorMode by remember { mutableStateOf(false) }

    // Keep screen on while filming
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Auto-scroll loop
    LaunchedEffect(isPlaying, scrollSpeed) {
        if (isPlaying) {
            val stepPx = when (scrollSpeed) {
                1 -> 2
                2 -> 4
                3 -> 7
                else -> 11
            }
            while (isActive && isPlaying) {
                delay(30)
                if (scrollState.value < scrollState.maxValue) {
                    scrollState.scrollTo(scrollState.value + stepPx)
                } else {
                    isPlaying = false
                }
            }
        }
    }

    val scriptText = activeProject?.currentScript?.ifBlank {
        "No script written yet. Open the Script Editor to write or generate your YouTube script."
    } ?: "Loading script..."

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Scrolling Text Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .scale(scaleX = if (isMirrorMode) -1f else 1f, scaleY = 1f)
                .padding(horizontal = 24.dp, vertical = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spoken Eye-Line Indicator Guide
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(AmberAccent.copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = scriptText,
                color = Color.White,
                fontSize = fontSizeSp.sp,
                lineHeight = (fontSizeSp * 1.5).sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPlaying = !isPlaying }
            )

            Spacer(modifier = Modifier.height(300.dp)) // Extra scroll pad at bottom
        }

        // Top Control Overlay
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(DarkCard.copy(alpha = 0.85f))
                    .testTag("teleprompter_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Font Size Decrement
                IconButton(
                    onClick = { if (fontSizeSp > 18) fontSizeSp -= 4 },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkCard.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.Default.TextDecrease, contentDescription = "Font -", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // Font Size Increment
                IconButton(
                    onClick = { if (fontSizeSp < 56) fontSizeSp += 4 },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkCard.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.Default.TextIncrease, contentDescription = "Font +", tint = Color.White, modifier = Modifier.size(18.dp))
                }

                // Mirror Mode Toggle
                IconButton(
                    onClick = { isMirrorMode = !isMirrorMode },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isMirrorMode) AmberAccent else DarkCard.copy(alpha = 0.85f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Flip,
                        contentDescription = "Mirror Mode",
                        tint = if (isMirrorMode) Color.Black else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Reset Scroll to Top
                IconButton(
                    onClick = {
                        isPlaying = false
                        scope.launch { scrollState.scrollTo(0) }
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkCard.copy(alpha = 0.85f))
                ) {
                    Icon(Icons.Default.Replay, contentDescription = "Reset Scroll", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Bottom Floating Play / Speed Controller Bar
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = DarkCard.copy(alpha = 0.92f),
            shadowElevation = 16.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(20.dp)
                .border(1.dp, DarkBorder, RoundedCornerShape(24.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Play / Pause Primary Button
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(RedPrimary)
                        .clickable { isPlaying = !isPlaying },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Speed Selectors: 1x, 2x, 3x, 4x
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        1 to "Slow",
                        2 to "Med",
                        3 to "Fast",
                        4 to "Max"
                    ).forEach { (spd, label) ->
                        val isSel = scrollSpeed == spd
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) AmberAccent else DarkSurfaceVariant)
                                .clickable { scrollSpeed = spd }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.Black else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
