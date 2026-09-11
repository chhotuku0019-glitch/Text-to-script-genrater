package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AiLoadingDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PurpleAccent
import com.example.ui.theme.RedPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextToScriptScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var ideaText by remember { mutableStateOf("") }
    var projectTitle by remember { mutableStateOf("") }
    var selectedVideoType by remember { mutableStateOf("Tutorial") }
    var selectedLanguage by remember { mutableStateOf("Hinglish") }
    var selectedDuration by remember { mutableStateOf("8 min") }
    var selectedTone by remember { mutableStateOf("Energetic") }
    var targetAudience by remember { mutableStateOf("") }

    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsState()

    val quickVideoTypes = listOf("Tutorial", "YouTube Short", "Long Video", "Explainer", "Storytelling", "Review")
    val quickLanguages = listOf("Hinglish", "Hindi", "English")
    val quickDurations = listOf("60 sec", "5 min", "8 min", "10 min", "15 min")
    val quickTones = listOf("Energetic", "Friendly", "Educational", "Storytelling", "Documentary")

    if (isAiLoading) {
        AiLoadingDialog(message = aiStatusMessage)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Text → YouTube Script",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = PurpleAccent,
                            modifier = Modifier.height(20.dp)
                        )
                    }
                    Text(
                        text = "Enter your idea or prompt to generate a full master script",
                        style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                    )
                }
            }
        }

        // Idea Prompt Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "WHAT DO YOU WANT TO MAKE A VIDEO ABOUT?",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = ideaText,
                        onValueChange = {
                            ideaText = it
                            if (projectTitle.isBlank() && it.isNotBlank()) {
                                projectTitle = it.take(40).trim()
                            }
                        },
                        placeholder = {
                            Text(
                                "Describe your video idea, key points, takeaways, or outline...\n\nExample: 'Make a video teaching beginners how to start freelancing with AI tools in 2026. Explain 3 high-paying niches, client acquisition strategies, and common mistakes.'",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        minLines = 6,
                        maxLines = 12,
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
                            .testTag("text_to_script_idea_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "PROJECT TITLE (OPTIONAL)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = projectTitle,
                        onValueChange = { projectTitle = it },
                        placeholder = { Text("e.g. Freelancing With AI Masterclass", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Script Configuration Selectors
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "VIDEO FORMAT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickVideoTypes.forEach { type ->
                            SelectableChip(
                                label = type,
                                isSelected = selectedVideoType == type,
                                onClick = { selectedVideoType = type }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "LANGUAGE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickLanguages.forEach { lang ->
                            SelectableChip(
                                label = lang,
                                isSelected = selectedLanguage == lang,
                                onClick = { selectedLanguage = lang }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "TARGET DURATION",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickDurations.forEach { dur ->
                            SelectableChip(
                                label = dur,
                                isSelected = selectedDuration == dur,
                                onClick = { selectedDuration = dur }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "TONE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickTones.forEach { t ->
                            SelectableChip(
                                label = t,
                                isSelected = selectedTone == t,
                                onClick = { selectedTone = t }
                            )
                        }
                    }
                }
            }
        }

        // Generate Script Button
        item {
            Button(
                onClick = {
                    if (ideaText.isBlank()) {
                        viewModel.showMessage("Please enter your video idea first.")
                        return@Button
                    }
                    viewModel.generateScriptFromTextAi(
                        title = projectTitle.ifBlank { "YouTube: $selectedVideoType" },
                        ideaText = ideaText,
                        videoType = selectedVideoType,
                        language = selectedLanguage,
                        targetDuration = selectedDuration,
                        tone = selectedTone,
                        targetAudience = targetAudience,
                        onSuccess = { newProjectId ->
                            viewModel.navigateTo(Screen.ProjectWorkspace(newProjectId, initialTab = 2))
                        }
                    )
                },
                enabled = !isAiLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedPrimary,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("generate_script_submit_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Generate",
                    modifier = Modifier.height(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "✨ Generate YouTube Script",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
