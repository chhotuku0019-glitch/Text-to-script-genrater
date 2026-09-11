package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.service.RecordingStateHolder
import com.example.service.RecordingStatus
import com.example.ui.components.AiLoadingDialog
import com.example.ui.components.AudioWaveVisualizer
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GreenSuccess
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

@Composable
fun SpeechRecordScreen(
    viewModel: MainViewModel,
    targetProjectId: Long?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val recordingState by viewModel.recordingState.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsState()

    val isRecording = recordingState.status == RecordingStatus.RECORDING
    val isPaused = recordingState.status == RecordingStatus.PAUSED
    val isProcessing = recordingState.status == RecordingStatus.PROCESSING

    var selectedLanguage by remember { mutableStateOf("Hinglish") }
    var transcriptTab by remember { mutableStateOf(0) } // 0: Raw, 1: Cleaned
    var editableRawTranscript by remember(recordingState.fullTranscript) {
        mutableStateOf(recordingState.fullTranscript)
    }
    var cleanTranscriptText by remember { mutableStateOf("") }
    var projectTitleInput by remember { mutableStateOf("") }

    // Audio Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] == true
        if (recordAudioGranted) {
            viewModel.startRecording(context, targetProjectId, selectedLanguage)
        } else {
            viewModel.showMessage("Microphone permission is required to record speech.")
        }
    }

    if (isAiLoading) {
        AiLoadingDialog(message = aiStatusMessage)
    }

    // Glowing halo animation when recording
    val infiniteTransition = rememberInfiniteTransition(label = "halo_anim")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo_scale"
    )

    val durationMin = recordingState.durationSeconds / 60
    val durationSec = recordingState.durationSeconds % 60
    val timerFormatted = String.format("%02d:%02d", durationMin, durationSec)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Bar
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Voice → Script Studio",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    )
                    Text(
                        text = "Speak naturally — AI cleans and structures your thoughts",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        }

        // Language & Mode Bar
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Speech Language:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Hinglish", "Hindi", "English").forEach { lang ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selectedLanguage == lang) RedPrimary else DarkSurfaceVariant)
                                    .clickable(enabled = !isRecording) { selectedLanguage = lang }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = lang,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedLanguage == lang) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedLanguage == lang) Color.White else TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Central Microphone Hero Studio
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Timer Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isRecording) RedPrimary.copy(alpha = 0.15f) else DarkSurfaceVariant)
                            .border(
                                1.dp,
                                if (isRecording) RedPrimary else DarkBorder,
                                RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecording) RedPrimary else if (isPaused) AmberAccent else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRecording) "LIVE • $timerFormatted" else if (isPaused) "PAUSED • $timerFormatted" else "READY • 00:00",
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) RedPrimary else if (isPaused) AmberAccent else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Big Glowing Microphone Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(130.dp)
                    ) {
                        if (isRecording) {
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(haloScale)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                RedPrimary.copy(alpha = 0.4f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) Brush.linearGradient(listOf(RedPrimary, Color(0xFFFF536E)))
                                    else if (isPaused) Brush.linearGradient(listOf(AmberAccent, Color(0xFFFFD166)))
                                    else Brush.linearGradient(listOf(DarkSurfaceVariant, DarkBorder))
                                )
                                .clickable {
                                    if (isRecording) {
                                        viewModel.pauseRecording(context)
                                    } else if (isPaused) {
                                        viewModel.resumeRecording(context)
                                    } else {
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            context, Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED

                                        if (hasPermission) {
                                            viewModel.startRecording(context, targetProjectId, selectedLanguage)
                                        } else {
                                            val permissionsToRequest = mutableListOf(Manifest.permission.RECORD_AUDIO)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                            permissionLauncher.launch(permissionsToRequest.toTypedArray())
                                        }
                                    }
                                }
                                .testTag("record_mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Pause else if (isPaused) Icons.Default.PlayArrow else Icons.Default.Mic,
                                contentDescription = "Mic",
                                tint = if (isRecording || isPaused) Color.White else RedPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio Waveform
                    AudioWaveVisualizer(
                        rmsNormalized = recordingState.currentRmsDb,
                        isRecording = isRecording
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isRecording) "Listening in background... Speak freely!" else if (isPaused) "Recording paused. Tap to resume." else "Tap microphone to start speaking your video idea.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )

                    // Control Buttons (Pause / Stop / Clear)
                    if (isRecording || isPaused || editableRawTranscript.isNotBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isRecording || isPaused) {
                                Button(
                                    onClick = { viewModel.stopRecording(context) },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RedPrimary,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Done Recording", fontWeight = FontWeight.Bold)
                                }
                            }

                            if (editableRawTranscript.isNotBlank()) {
                                Button(
                                    onClick = {
                                        RecordingStateHolder.reset()
                                        editableRawTranscript = ""
                                        cleanTranscriptText = ""
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkSurfaceVariant,
                                        contentColor = TextSecondary
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Live Real-Time Speech Stream (While Recording)
        if (recordingState.interimText.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AmberAccent.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "LIVE SPEECH STREAM",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberAccent,
                                letterSpacing = 1.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "... ${recordingState.interimText}",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Transcript Workspace & Comparison
        val fullText = editableRawTranscript.ifEmpty { recordingState.fullTranscript }
        if (fullText.isNotBlank()) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Tab Row for Raw vs Cleaned Transcript
                        TabRow(
                            selectedTabIndex = transcriptTab,
                            containerColor = DarkCard,
                            contentColor = TextPrimary,
                            indicator = { tabPositions ->
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[transcriptTab]),
                                    color = RedPrimary
                                )
                            }
                        ) {
                            Tab(
                                selected = transcriptTab == 0,
                                onClick = { transcriptTab = 0 },
                                text = {
                                    Text(
                                        text = "Raw Transcript",
                                        fontWeight = if (transcriptTab == 0) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                }
                            )
                            Tab(
                                selected = transcriptTab == 1,
                                onClick = { transcriptTab = 1 },
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Cleaned AI",
                                            fontWeight = if (transcriptTab == 1) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp
                                        )
                                        if (cleanTranscriptText.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(GreenSuccess)
                                            )
                                        }
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (transcriptTab == 0) {
                            // Raw Transcript Content
                            OutlinedTextField(
                                value = fullText,
                                onValueChange = {
                                    editableRawTranscript = it
                                    RecordingStateHolder.setFullTranscript(it)
                                },
                                minLines = 4,
                                maxLines = 10,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant,
                                    focusedBorderColor = RedPrimary,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("raw_transcript_field")
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Words: ${fullText.split(Regex("\\s+")).filter { it.isNotBlank() }.size}",
                                    color = TextMuted,
                                    fontSize = 12.sp
                                )

                                Button(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(fullText))
                                        viewModel.showMessage("Transcript copied to clipboard!")
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkSurfaceVariant,
                                        contentColor = TextPrimary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", fontSize = 12.sp)
                                }
                            }
                        } else {
                            // Cleaned Transcript Content
                            if (cleanTranscriptText.isBlank()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Clean up spoken filler words with AI",
                                        color = TextSecondary,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.cleanTranscriptAi(
                                                    projectId = targetProjectId ?: 0L,
                                                    rawTranscript = fullText,
                                                    language = selectedLanguage,
                                                    autoOpenEditor = false,
                                                    onSuccess = { cleaned, _ ->
                                                        cleanTranscriptText = cleaned
                                                    }
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = PurpleAccent,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = "Clean", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("✨ Clean Text", fontWeight = FontWeight.Bold)
                                        }

                                        Button(
                                            onClick = {
                                                viewModel.cleanTranscriptAi(
                                                    projectId = targetProjectId ?: 0L,
                                                    rawTranscript = fullText,
                                                    language = selectedLanguage,
                                                    autoOpenEditor = true,
                                                    onSuccess = { cleaned, _ ->
                                                        cleanTranscriptText = cleaned
                                                    }
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = RedPrimary,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(Icons.Default.AutoAwesome, contentDescription = "Clean & Open", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("✨ Clean & Edit in Script Editor", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                OutlinedTextField(
                                    value = cleanTranscriptText,
                                    onValueChange = { cleanTranscriptText = it },
                                    minLines = 4,
                                    maxLines = 10,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = DarkSurfaceVariant,
                                        unfocusedContainerColor = DarkSurfaceVariant,
                                        focusedBorderColor = GreenSuccess,
                                        unfocusedBorderColor = DarkBorder,
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        viewModel.cleanTranscriptAi(
                                            projectId = targetProjectId ?: 0L,
                                            rawTranscript = fullText,
                                            language = selectedLanguage,
                                            autoOpenEditor = true
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RedPrimary,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = "Open", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Open in Project Script Editor", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Actions: Convert to YouTube Script OR Save to Project
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // 1. Convert to YouTube Script Action
                            Button(
                                onClick = {
                                    val textToConvert = if (cleanTranscriptText.isNotBlank()) cleanTranscriptText else fullText
                                    viewModel.createNewProject(
                                        title = projectTitleInput.ifBlank { "Voice Idea: ${textToConvert.take(30).trim()}..." },
                                        videoType = "Tutorial",
                                        language = selectedLanguage,
                                        targetDuration = "8 min",
                                        tone = "Energetic",
                                        targetAudience = "",
                                        tags = "Voice, YouTube",
                                        ideaText = textToConvert,
                                        onCreated = { newProjId ->
                                            viewModel.saveRecordedTranscriptToProject(newProjId, textToConvert)
                                            viewModel.convertTranscriptToScriptAi(newProjId)
                                            viewModel.navigateTo(Screen.ProjectWorkspace(newProjId, initialTab = 2))
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RedPrimary,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(50.dp)
                                    .testTag("convert_speech_to_script_button")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = "Convert", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "🚀 Convert to YouTube Script",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            // 2. Save Transcript to Project Workspace
                            if (targetProjectId != null && targetProjectId > 0) {
                                Button(
                                    onClick = {
                                        viewModel.saveRecordedTranscriptToProject(targetProjectId, fullText)
                                        viewModel.navigateTo(Screen.ProjectWorkspace(targetProjectId, initialTab = 1))
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DarkSurfaceVariant,
                                        contentColor = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save to Active Project")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
