package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ScriptForgeApp
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
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val prefs = viewModel.preferences
    val scope = rememberCoroutineScope()
    val allProjects by viewModel.allProjects.collectAsState()

    var apiKeyInput by remember { mutableStateOf(prefs.customApiKey) }
    var isApiKeyVisible by remember { mutableStateOf(false) }
    var selectedModel by remember { mutableStateOf(prefs.selectedModel) }
    var temperatureVal by remember { mutableFloatStateOf(prefs.temperature) }
    var speakingWpm by remember { mutableIntStateOf(prefs.speakingWpm) }
    var autoCleanEnabled by remember { mutableStateOf(prefs.autoCleanTranscript) }
    var autoConvertEnabled by remember { mutableStateOf(prefs.autoConvertScript) }
    var screenRecordingMode by remember { mutableStateOf(prefs.isScreenRecordingMode) }

    var testConnectionStatus by remember { mutableStateOf<String?>(null) }
    var isTestingConnection by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Column {
                Text(
                    text = "Studio Settings",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                )
                Text(
                    text = "Configure Gemini AI models, voice defaults, and teleprompter",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )
            }
        }

        // 1. Gemini AI Configuration Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = "AI", tint = RedPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GEMINI AI CONFIGURATION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "API Key (Stored locally or via Secrets Panel)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = {
                            apiKeyInput = it
                            prefs.customApiKey = it
                        },
                        placeholder = { Text("Leave blank to use default BuildConfig key", color = TextMuted, fontSize = 12.sp) },
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isApiKeyVisible = !isApiKeyVisible }) {
                                Icon(
                                    imageVector = if (isApiKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "Toggle key visibility",
                                    tint = TextSecondary
                                )
                            }
                        },
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_api_key_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Test Connection Button & Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                isTestingConnection = true
                                testConnectionStatus = null
                                scope.launch {
                                    val res = ScriptForgeApp.instance.geminiService.testConnection()
                                    isTestingConnection = false
                                    testConnectionStatus = if (res.isSuccess) "Connected Successfully! ✓" else "Failed: ${res.exceptionOrNull()?.localizedMessage}"
                                }
                            },
                            enabled = !isTestingConnection,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = TextPrimary
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            if (isTestingConnection) {
                                CircularProgressIndicator(color = RedPrimary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Test API Connection", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (testConnectionStatus != null) {
                            val isSuccess = testConnectionStatus?.contains("Successfully") == true
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = "Status",
                                    tint = if (isSuccess) GreenSuccess else RedPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isSuccess) "Connected" else "Error",
                                    fontSize = 12.sp,
                                    color = if (isSuccess) GreenSuccess else RedPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Model Selection
                    Text(
                        text = "Active Gemini Model",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "gemini-3.5-flash" to "Flash (Fast & Structured)",
                            "gemini-3.1-pro-preview" to "Pro (Deep Reasoning)"
                        ).forEach { (modelId, label) ->
                            val isSel = selectedModel == modelId
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) RedPrimary else DarkSurfaceVariant)
                                    .border(1.dp, if (isSel) RedPrimary else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedModel = modelId
                                        prefs.selectedModel = modelId
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) Color.White else TextSecondary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Temperature Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Creativity (Temperature)", fontSize = 12.sp, color = TextSecondary)
                        Text(text = String.format("%.1f", temperatureVal), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                    }

                    Slider(
                        value = temperatureVal,
                        onValueChange = {
                            temperatureVal = it
                            prefs.temperature = it
                        },
                        valueRange = 0.2f..1.0f,
                        steps = 8,
                        colors = SliderDefaults.colors(
                            thumbColor = RedPrimary,
                            activeTrackColor = RedPrimary,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )
                }
            }
        }

        // 2. Speaking & Recording Settings
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Speed, contentDescription = "Speaking Speed", tint = AmberAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VOICE & SCRIPT TIMING",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Average Creator Speaking Pace", fontSize = 12.sp, color = TextSecondary)
                        Text(text = "$speakingWpm WPM", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                    }

                    Slider(
                        value = speakingWpm.toFloat(),
                        onValueChange = {
                            speakingWpm = it.toInt()
                            prefs.speakingWpm = it.toInt()
                        },
                        valueRange = 110f..220f,
                        steps = 10,
                        colors = SliderDefaults.colors(
                            thumbColor = AmberAccent,
                            activeTrackColor = AmberAccent,
                            inactiveTrackColor = DarkSurfaceVariant
                        )
                    )

                    Text(
                        text = "Standard YouTube narration is ~140-160 WPM. Fast-paced Shorts are ~180+ WPM.",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto Clean Transcript Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Auto-Clean Spoken Transcript", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(text = "Automatically strip filler words after recording", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = autoCleanEnabled,
                            onCheckedChange = {
                                autoCleanEnabled = it
                                prefs.autoCleanTranscript = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = RedPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Screen Recording Mode Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Screen Recording Friendly Mode", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                            Text(text = "Suppresses non-essential notification popups while recording videos", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(
                            checked = screenRecordingMode,
                            onCheckedChange = {
                                screenRecordingMode = it
                                prefs.isScreenRecordingMode = it
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AmberAccent
                            )
                        )
                    }
                }
            }
        }

        // 3. Privacy, Storage & Security Info
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = "Security", tint = GreenSuccess, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PRIVACY & LOCAL STORAGE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "• All projects, transcripts, drafts, and version history are stored strictly on your device in a secure local Room database.\n• Voice recordings are processed continuously without mandatory account creation or external cloud lock-in.\n• Total projects in database: ${allProjects.size}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 4. Developer Profile Card (GitHub)
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Code, contentDescription = "Developer", tint = PurpleAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "DEVELOPER PROFILE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, PurpleAccent, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Priyanshu",
                                    tint = PurpleAccent,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column {
                                Text(
                                    text = "Priyanshu",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = "@priyanshu0019",
                                    fontSize = 12.sp,
                                    color = PurpleAccent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/priyanshu0019")).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    viewModel.showMessage("GitHub: https://github.com/priyanshu0019")
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = TextPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text("GitHub", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Open GitHub",
                                modifier = Modifier.size(14.dp),
                                tint = PurpleAccent
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Crafted with passion for YouTube creators, scriptwriters, and storytellers worldwide.",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Version Footer
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ScriptForge AI v1.0.0 (Production Build)",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Text(
                    text = "YouTube Creator Script Workspace",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
