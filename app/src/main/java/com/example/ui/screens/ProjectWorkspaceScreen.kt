package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ProjectEntity
import com.example.data.local.ScriptVersionEntity
import com.example.ui.components.AiLoadingDialog
import com.example.ui.components.StatusBadge
import com.example.ui.components.StudioTopBar
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.BlueAccent
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
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    viewModel: MainViewModel,
    projectId: Long,
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val activeProject by viewModel.activeProject.collectAsState()
    val versions by viewModel.activeProjectVersions.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val aiStatusMessage by viewModel.aiStatusMessage.collectAsState()
    val saveStatus by viewModel.saveStatus.collectAsState()

    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("Overview", "Transcript", "Script Editor", "AI Studio Tools", "History & Drafts")

    // State for Save Version Dialog
    var showSaveVersionDialog by remember { mutableStateOf(false) }
    var newVersionName by remember { mutableStateOf("") }
    var newVersionNote by remember { mutableStateOf("") }

    // State for Version Preview Dialog
    var previewVersion by remember { mutableStateOf<ScriptVersionEntity?>(null) }

    if (isAiLoading) {
        AiLoadingDialog(message = aiStatusMessage)
    }

    if (activeProject == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = RedPrimary)
        }
        return
    }

    val project = activeProject!!

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Bar
        StudioTopBar(
            title = project.title,
            subtitle = "${project.videoType} • ${project.targetDuration} • ${project.language}",
            showBackButton = true,
            onBackClick = onBack,
            statusBadge = project.status,
            actions = {
                // Quick Teleprompter Launch
                IconButton(
                    onClick = { viewModel.navigateTo(Screen.Teleprompter(projectId)) },
                    modifier = Modifier.testTag("btn_launch_teleprompter")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayCircle,
                        contentDescription = "Teleprompter",
                        tint = AmberAccent
                    )
                }

                // Share / Export
                IconButton(
                    onClick = { viewModel.shareScript(context, project) },
                    modifier = Modifier.testTag("btn_share_script")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export",
                        tint = TextPrimary
                    )
                }
            }
        )

        // Workspace Tab Row
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkCard,
            contentColor = TextPrimary,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                if (selectedTab < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = RedPrimary
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == index) RedPrimary else TextSecondary
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                0 -> OverviewTab(viewModel, project, onSelectTab = { selectedTab = it })
                1 -> TranscriptTab(viewModel, project, onOpenScriptEditor = { selectedTab = 2 })
                2 -> ScriptEditorTab(viewModel, project, saveStatus)
                3 -> AiStudioToolsTab(viewModel, project)
                4 -> HistoryVersionsTab(
                    versions = versions,
                    onSaveNew = { showSaveVersionDialog = true },
                    onPreview = { previewVersion = it },
                    onRestore = { version -> viewModel.restoreVersion(project.id, version) },
                    onDelete = { vId -> viewModel.deleteVersion(vId) }
                )
            }
        }
    }

    // Save Version Dialog
    if (showSaveVersionDialog) {
        AlertDialog(
            onDismissRequest = { showSaveVersionDialog = false },
            title = { Text("Save Script Version", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Create a restore point for this draft iteration:", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = newVersionName,
                        onValueChange = { newVersionName = it },
                        placeholder = { Text("e.g. Draft 2 - Hook Refined", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newVersionNote,
                        onValueChange = { newVersionNote = it },
                        placeholder = { Text("Optional note or summary of changes...", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = newVersionName.ifBlank { "Version ${versions.size + 1}" }
                        viewModel.saveNewVersion(projectId, name, newVersionNote)
                        showSaveVersionDialog = false
                        newVersionName = ""
                        newVersionNote = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Save Version")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveVersionDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }

    // Preview Version Dialog
    if (previewVersion != null) {
        val version = previewVersion!!
        AlertDialog(
            onDismissRequest = { previewVersion = null },
            title = { Text("Preview: ${version.versionName}", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Words: ${version.wordCount} • Created: ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(version.createdAt))}", color = TextMuted, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn {
                            item {
                                Text(
                                    text = version.scriptContent.ifEmpty { "(Empty script in this version)" },
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreVersion(projectId, version)
                        previewVersion = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Restore This Version")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewVersion = null }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }
}

// -------------------------------------------------------------
// TAB 1: OVERVIEW TAB
// -------------------------------------------------------------
@Composable
fun OverviewTab(
    viewModel: MainViewModel,
    project: ProjectEntity,
    onSelectTab: (Int) -> Unit
) {
    val durationMin = kotlin.math.max(1, project.wordCount / 150)
    var isEditingTitle by remember { mutableStateOf(false) }
    var titleText by remember(project.title) { mutableStateOf(project.title) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status & Title Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        StatusBadge(status = project.status)

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val newStatus = if (project.status == "Final") "Draft" else "Final"
                                    viewModel.setProjectStatus(project.id, newStatus)
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (project.status == "Final") AmberAccent.copy(alpha = 0.2f) else GreenSuccess.copy(alpha = 0.2f),
                                    contentColor = if (project.status == "Final") AmberAccent else GreenSuccess
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(
                                    text = if (project.status == "Final") "Revert to Draft" else "Mark as Final ✓",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isEditingTitle) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = titleText,
                                onValueChange = { titleText = it },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = DarkSurfaceVariant,
                                    unfocusedContainerColor = DarkSurfaceVariant,
                                    focusedBorderColor = RedPrimary,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                viewModel.updateActiveProject(project.copy(title = titleText.ifBlank { "Untitled" }))
                                isEditingTitle = false
                            }) {
                                Icon(Icons.Default.Check, contentDescription = "Save Title", tint = GreenSuccess)
                            }
                        }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = project.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { isEditingTitle = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit Title", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }

        // Live Metadata Counters
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    title = "WORDS",
                    value = "${project.wordCount}",
                    color = BlueAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "EST. DURATION",
                    value = "≈ $durationMin min",
                    color = AmberAccent,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "TARGET",
                    value = project.targetDuration,
                    color = PurpleAccent,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Strategy Specs Card
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
                        text = "VIDEO STRATEGY & PARAMETERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    SpecRow(label = "Video Type", value = project.videoType)
                    SpecRow(label = "Language", value = project.language)
                    SpecRow(label = "Creator Tone", value = project.tone)
                    SpecRow(label = "Target Audience", value = project.targetAudience.ifEmpty { "General YouTube" })
                    SpecRow(label = "Tags", value = project.tags)
                }
            }
        }

        // Quick Action Matrix
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "QUICK ACTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "🎙️ Record Voice",
                        subtitle = "Speak new thoughts",
                        onClick = { viewModel.navigateTo(Screen.SpeechRecord(project.id)) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "📝 Script Editor",
                        subtitle = "Format & refine script",
                        onClick = { onSelectTab(2) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionButton(
                        title = "✨ AI Studio Tools",
                        subtitle = "18+ rewrite & hook tools",
                        onClick = { onSelectTab(3) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionButton(
                        title = "🎬 Teleprompter",
                        subtitle = "Full-screen filming mode",
                        onClick = { viewModel.navigateTo(Screen.Teleprompter(project.id)) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = modifier.border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 15.sp, fontWeight = FontWeight.Black, color = color)
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextSecondary, fontSize = 13.sp)
        Text(text = value, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

@Composable
fun QuickActionButton(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        modifier = modifier
            .height(82.dp)
            .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, fontSize = 11.sp, color = TextSecondary)
        }
    }
}

// -------------------------------------------------------------
// TAB 2: TRANSCRIPT TAB
// -------------------------------------------------------------
@Composable
fun TranscriptTab(
    viewModel: MainViewModel,
    project: ProjectEntity,
    onOpenScriptEditor: () -> Unit = {}
) {
    var rawText by remember(project.rawTranscript) { mutableStateOf(project.rawTranscript) }
    var cleanText by remember(project.cleanTranscript) { mutableStateOf(project.cleanTranscript) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // AI Conversion Banner
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PurpleAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TRANSCRIPT → YOUTUBE SCRIPT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = PurpleAccent,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Clean up verbal fillers and convert spoken points into an organized, high-retention script.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                if (rawText.isBlank()) {
                                    viewModel.showMessage("Please record or write a transcript first.")
                                } else {
                                    viewModel.cleanTranscriptAi(
                                        projectId = project.id,
                                        rawTranscript = rawText,
                                        language = project.language,
                                        autoOpenEditor = true
                                    )
                                    onOpenScriptEditor()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PurpleAccent,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("✨ Clean & Edit in Script Editor", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.convertTranscriptToScriptAi(project.id)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🚀 Convert to Script", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Cleaned Transcript Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CLEAN TRANSCRIPT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = GreenSuccess,
                            letterSpacing = 1.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Words: ${cleanText.split(Regex("\\s+")).filter { it.isNotBlank() }.size}",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            if (cleanText.isNotBlank()) {
                                Button(
                                    onClick = onOpenScriptEditor,
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Text("Open Editor", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = cleanText,
                        onValueChange = {
                            cleanText = it
                            viewModel.updateActiveProject(project.copy(cleanTranscript = it))
                        },
                        placeholder = { Text("Cleaned transcript will appear here after AI cleanup...", color = TextMuted) },
                        minLines = 5,
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
                }
            }
        }

        // Raw Transcript Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RAW SPOKEN TRANSCRIPT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent,
                            letterSpacing = 1.sp
                        )
                        Button(
                            onClick = {
                                viewModel.navigateTo(Screen.SpeechRecord(project.id))
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = RedPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Record", modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Record More", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = rawText,
                        onValueChange = {
                            rawText = it
                            viewModel.updateActiveProject(project.copy(rawTranscript = it))
                        },
                        placeholder = { Text("Spoken words from voice recordings or pasted thoughts...", color = TextMuted) },
                        minLines = 5,
                        maxLines = 10,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = AmberAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: SCRIPT EDITOR TAB
// -------------------------------------------------------------
@Composable
fun ScriptEditorTab(
    viewModel: MainViewModel,
    project: ProjectEntity,
    saveStatus: String
) {
    val clipboardManager = LocalClipboardManager.current
    var tfv by remember(project.currentScript) {
        mutableStateOf(TextFieldValue(project.currentScript, TextRange(project.currentScript.length)))
    }

    val wordCount = remember(tfv.text) {
        if (tfv.text.isBlank()) 0 else tfv.text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
    }
    val charCount = tfv.text.length
    val durationMin = kotlin.math.max(1, wordCount / 150)
    val durationSecRem = (wordCount % 150) * 60 / 150
    val estDurationFormatted = String.format("%dm %02ds", durationMin, durationSecRem)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Editor Status & Formatting Toolbar
        Card(
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                // Top Row: Counters & Auto-Save status
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$wordCount words",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueAccent
                        )
                        Text(text = "•", color = TextMuted)
                        Text(
                            text = "$charCount chars",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(text = "•", color = TextMuted)
                        Text(
                            text = "≈ $estDurationFormatted",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberAccent
                        )
                    }

                    Text(
                        text = saveStatus,
                        fontSize = 12.sp,
                        color = if (saveStatus == "Saved ✓") GreenSuccess else AmberAccent,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Rich Mobile Formatting Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Undo
                    IconButton(
                        onClick = { viewModel.undoScript() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Undo, contentDescription = "Undo", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    // Redo
                    IconButton(
                        onClick = { viewModel.redoScript() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Redo, contentDescription = "Redo", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Bold Insert
                    EditorToolPill(label = "B", onClick = {
                        val sel = tfv.selection
                        val text = tfv.text
                        val newText = text.substring(0, sel.start) + "**" + text.substring(sel.start, sel.end) + "**" + text.substring(sel.end)
                        tfv = TextFieldValue(newText, TextRange(sel.start + 2, sel.end + 2))
                        viewModel.updateScriptContent(newText)
                    })

                    // Heading Insert
                    EditorToolPill(label = "H2", onClick = {
                        val sel = tfv.selection
                        val text = tfv.text
                        val newText = text.substring(0, sel.start) + "\n## " + text.substring(sel.start)
                        tfv = TextFieldValue(newText, TextRange(sel.start + 4))
                        viewModel.updateScriptContent(newText)
                    })

                    // Hook Tag
                    EditorToolPill(label = "[HOOK]", onClick = {
                        val sel = tfv.selection
                        val text = tfv.text
                        val newText = text.substring(0, sel.start) + "\n[HOOK (0-10s)]\n" + text.substring(sel.start)
                        tfv = TextFieldValue(newText, TextRange(sel.start + 16))
                        viewModel.updateScriptContent(newText)
                    })

                    // Intro Tag
                    EditorToolPill(label = "[INTRO]", onClick = {
                        val sel = tfv.selection
                        val text = tfv.text
                        val newText = text.substring(0, sel.start) + "\n[INTRO]\n" + text.substring(sel.start)
                        tfv = TextFieldValue(newText, TextRange(sel.start + 8))
                        viewModel.updateScriptContent(newText)
                    })

                    // CTA Tag
                    EditorToolPill(label = "[CTA]", onClick = {
                        val sel = tfv.selection
                        val text = tfv.text
                        val newText = text.substring(0, sel.start) + "\n[CALL TO ACTION]\n" + text.substring(sel.start)
                        tfv = TextFieldValue(newText, TextRange(sel.start + 17))
                        viewModel.updateScriptContent(newText)
                    })

                    // Bullet
                    EditorToolPill(label = "• Bullet", onClick = {
                        val sel = tfv.selection
                        val text = tfv.text
                        val newText = text.substring(0, sel.start) + "\n- " + text.substring(sel.start)
                        tfv = TextFieldValue(newText, TextRange(sel.start + 3))
                        viewModel.updateScriptContent(newText)
                    })

                    // Copy All
                    EditorToolPill(label = "Copy All", onClick = {
                        clipboardManager.setText(AnnotatedString(tfv.text))
                        viewModel.showMessage("Script copied to clipboard!")
                    })
                }
            }
        }

        // Script Text Area
        OutlinedTextField(
            value = tfv,
            onValueChange = {
                tfv = it
                viewModel.updateScriptContent(it.text)
            },
            placeholder = {
                Text(
                    "Start writing your YouTube script here or use AI to generate...\n\nExample structure:\n[HOOK (0-10s)]\nStop the scroll with a curiosity gap!\n\n[INTRO]\nExplain why this video is essential.\n\n## Section 1: The Secret Formula\nExplain step 1 in punchy spoken sentences.\n\n[CALL TO ACTION]\nSubscribe for weekly AI creator breakdowns!",
                    color = TextMuted,
                    fontSize = 14.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkBackground,
                unfocusedContainerColor = DarkBackground,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            textStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp,
                lineHeight = 24.sp,
                color = TextPrimary
            ),
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("script_editor_textarea")
        )
    }
}

@Composable
fun EditorToolPill(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

// -------------------------------------------------------------
// TAB 4: AI STUDIO TOOLS TAB
// -------------------------------------------------------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiStudioToolsTab(
    viewModel: MainViewModel,
    project: ProjectEntity
) {
    val clipboardManager = LocalClipboardManager.current
    var activeSubTool by remember { mutableStateOf("Rewrites") } // "Rewrites", "Hooks", "Titles", "Description", "Thumbnails", "Scenes", "Analyze"

    val subTools = listOf(
        "Rewrites" to "✨ AI Rewriter",
        "FunZone" to "🎭 Creator Fun Zone",
        "Hooks" to "🎣 8 Hooks",
        "Titles" to "🏷️ 10 Titles",
        "Description" to "📝 Description",
        "Thumbnails" to "🖼️ Thumbnails",
        "Scenes" to "🎥 Scene B-Roll",
        "Analyze" to "📊 Script Analysis"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tool switcher bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkCard)
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subTools.forEach { (key, label) ->
                val isSelected = activeSubTool == key
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) RedPrimary else DarkSurfaceVariant)
                        .clickable { activeSubTool = key }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) Color.White else TextSecondary
                    )
                }
            }
        }

        // Sub-tool content container
        Box(modifier = Modifier.weight(1f)) {
            when (activeSubTool) {
                "Rewrites" -> RewritesSubTool(viewModel, project)
                "FunZone" -> FunZoneSubTool(viewModel, project, clipboardManager)
                "Hooks" -> HooksSubTool(viewModel, project, clipboardManager)
                "Titles" -> TitlesSubTool(viewModel, project, clipboardManager)
                "Description" -> DescriptionSubTool(viewModel, project, clipboardManager)
                "Thumbnails" -> ThumbnailsSubTool(viewModel, project, clipboardManager)
                "Scenes" -> ScenesSubTool(viewModel, project, clipboardManager)
                "Analyze" -> AnalysisSubTool(viewModel, project)
            }
        }
    }
}

// 1. Rewrites Sub-Tool
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RewritesSubTool(viewModel: MainViewModel, project: ProjectEntity) {
    val aiTools = listOf(
        "Improve Script", "Improve Hook", "Make More Engaging",
        "Make Shorter", "Expand Script", "Make More Natural",
        "Make Voice-over Friendly", "Convert to Hinglish", "Convert to Hindi",
        "Convert to English", "Make More Casual", "Make More Educational",
        "Make Documentary Style", "Make High-Energy", "Remove Repetition",
        "Improve Transitions", "Add CTA"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "1-TAP AI SCRIPT REFINEMENTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Select any optimization to rewrite the current script. Your previous version will be automatically backed up in Version History.",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                aiTools.forEach { tool ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        modifier = Modifier
                            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.applyScriptToolAi(project.id, tool)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "AI", tint = RedPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = tool, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
                        }
                    }
                }
            }
        }
    }
}

// 2. Hooks Sub-Tool (8 Types)
@Composable
fun HooksSubTool(
    viewModel: MainViewModel,
    project: ProjectEntity,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val hooksList = remember(project.hooksJson) {
        val list = mutableListOf<Triple<String, String, String>>()
        try {
            val arr = JSONArray(project.hooksJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Triple(
                        obj.optString("type", "Hook ${i + 1}"),
                        obj.optString("text", ""),
                        obj.optString("explanation", "")
                    )
                )
            }
        } catch (e: Exception) {}
        list
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "8 HIGH-RETENTION HOOKS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Hook variations to maximize first 10-second watch time",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { viewModel.generateHooksAi(project.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (hooksList.isEmpty()) "Generate Hooks" else "Regenerate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (hooksList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No hooks generated yet", color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.generateHooksAi(project.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("🎣 Generate 8 YouTube Hooks")
                        }
                    }
                }
            }
        } else {
            items(hooksList) { (type, text, explanation) ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AmberAccent.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = type.uppercase(),
                                    color = AmberAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(text))
                                        viewModel.showMessage("Hook copied to clipboard!")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        val newScript = "[HOOK ($type)]\n$text\n\n" + project.currentScript
                                        viewModel.updateActiveProject(project.copy(currentScript = newScript))
                                        viewModel.showMessage("Hook inserted at top of script!")
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Bookmark, contentDescription = "Insert", tint = RedPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "\"$text\"",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        )

                        if (explanation.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 $explanation",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

// 3. Titles Sub-Tool (10 Titles in 6 Categories)
@Composable
fun TitlesSubTool(
    viewModel: MainViewModel,
    project: ProjectEntity,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val titlesList = remember(project.titlesJson) {
        val list = mutableListOf<Triple<String, String, String>>()
        try {
            val arr = JSONArray(project.titlesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    Triple(
                        obj.optString("category", "General"),
                        obj.optString("title", ""),
                        obj.optString("scoreHint", "")
                    )
                )
            }
        } catch (e: Exception) {}
        list
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "10 CLICK-WORTHY TITLES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "SEO, Curiosity, High CTR & Punchy variations",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { viewModel.generateTitlesAi(project.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (titlesList.isEmpty()) "Generate Titles" else "Regenerate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (titlesList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No title ideas generated yet", color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.generateTitlesAi(project.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("🏷️ Generate 10 YouTube Titles")
                        }
                    }
                }
            }
        } else {
            items(titlesList) { (category, title, scoreHint) ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(DarkSurfaceVariant)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = category.uppercase(),
                                        color = BlueAccent,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (scoreHint.isNotBlank()) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = scoreHint, fontSize = 11.sp, color = GreenSuccess)
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            )
                        }

                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(title))
                                viewModel.showMessage("Title copied!")
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// 4. Description & Chapters Sub-Tool
@Composable
fun DescriptionSubTool(
    viewModel: MainViewModel,
    project: ProjectEntity,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val descObj = remember(project.descriptionJson) {
        try {
            JSONObject(project.descriptionJson)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    val shortDesc = descObj.optString("shortDescription", "")
    val seoDesc = descObj.optString("seoDescription", "")
    val detailedDesc = descObj.optString("detailedDescription", "")
    val chaptersArr = descObj.optJSONArray("chapters")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SEO DESCRIPTION & CHAPTERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Algorithm-optimized metadata package",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { viewModel.generateDescriptionAndChaptersAi(project.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (detailedDesc.isEmpty()) "Generate" else "Regenerate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (detailedDesc.isEmpty() && seoDesc.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No description generated yet", color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.generateDescriptionAndChaptersAi(project.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("📝 Generate Description & Chapters")
                        }
                    }
                }
            }
        } else {
            // Short Description
            if (shortDesc.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("SHORT DESCRIPTION (2-LINER)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(shortDesc, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }

            // SEO Description
            if (seoDesc.isNotBlank()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("SEO KEYWORD DESCRIPTION", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(seoDesc, color = TextPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }

            // Chapters
            if (chaptersArr != null && chaptersArr.length() > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("TIMESTAMPS & CHAPTERS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                            Spacer(modifier = Modifier.height(8.dp))
                            for (i in 0 until chaptersArr.length()) {
                                val cObj = chaptersArr.getJSONObject(i)
                                val ts = cObj.optString("timestamp", "00:00")
                                val title = cObj.optString("title", "")
                                Text(
                                    text = "$ts - $title",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 5. Thumbnails Sub-Tool
@Composable
fun ThumbnailsSubTool(
    viewModel: MainViewModel,
    project: ProjectEntity,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val conceptsList = remember(project.thumbnailsJson) {
        val list = mutableListOf<Map<String, String>>()
        try {
            val arr = JSONArray(project.thumbnailsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    mapOf(
                        "mainText" to obj.optString("mainText", "CONCEPT ${i + 1}"),
                        "visualIdea" to obj.optString("visualIdea", ""),
                        "subjectPlacement" to obj.optString("subjectPlacement", ""),
                        "emotion" to obj.optString("emotion", ""),
                        "composition" to obj.optString("composition", "")
                    )
                )
            }
        } catch (e: Exception) {}
        list
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "5 THUMBNAIL CONCEPTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "High-CTR visual ideas, text overlays & compositions",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { viewModel.generateThumbnailConceptsAi(project.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (conceptsList.isEmpty()) "Generate" else "Regenerate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (conceptsList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No thumbnail concepts generated yet", color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.generateThumbnailConceptsAi(project.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("🖼️ Generate 5 Thumbnail Ideas")
                        }
                    }
                }
            }
        } else {
            items(conceptsList) { item ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(RedPrimary)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = item["mainText"] ?: "OVERLAY TEXT",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Text(
                                text = "Emotion: ${item["emotion"]}",
                                fontSize = 11.sp,
                                color = AmberAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "🎨 Visual Scene:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text(text = item["visualIdea"] ?: "", fontSize = 13.sp, color = TextPrimary)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(text = "👤 Subject & Placement:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text(text = item["subjectPlacement"] ?: "", fontSize = 13.sp, color = TextSecondary)

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(text = "📐 Colors & Composition:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Text(text = item["composition"] ?: "", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}

// 6. Scenes & Visual Suggestions Sub-Tool
@Composable
fun ScenesSubTool(
    viewModel: MainViewModel,
    project: ProjectEntity,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val scenesList = remember(project.scenesJson) {
        val list = mutableListOf<Map<String, String>>()
        try {
            val arr = JSONArray(project.scenesJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    mapOf(
                        "scriptSection" to obj.optString("scriptSection", "Section ${i + 1}"),
                        "sceneIdea" to obj.optString("sceneIdea", ""),
                        "bRollIdea" to obj.optString("bRollIdea", ""),
                        "screenRecordingIdea" to obj.optString("screenRecordingIdea", ""),
                        "animationIdea" to obj.optString("animationIdea", ""),
                        "aiImagePrompt" to obj.optString("aiImagePrompt", "")
                    )
                )
            }
        } catch (e: Exception) {}
        list
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SCENE & B-ROLL SUGGESTIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Visual cues, camera framing & motion graphics",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { viewModel.generateSceneSuggestionsAi(project.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (scenesList.isEmpty()) "Generate" else "Regenerate", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (scenesList.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No visual scene suggestions generated yet", color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.generateSceneSuggestionsAi(project.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("🎥 Break Down Script Into Scenes")
                        }
                    }
                }
            }
        } else {
            items(scenesList) { scene ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🎬 ${scene["scriptSection"]}",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(text = "📷 Primary Framing: ${scene["sceneIdea"]}", fontSize = 12.sp, color = BlueAccent)
                        if (!scene["bRollIdea"].isNullOrBlank()) {
                            Text(text = "🎞️ B-Roll: ${scene["bRollIdea"]}", fontSize = 12.sp, color = AmberAccent)
                        }
                        if (!scene["screenRecordingIdea"].isNullOrBlank()) {
                            Text(text = "💻 Screen Record: ${scene["screenRecordingIdea"]}", fontSize = 12.sp, color = GreenSuccess)
                        }
                        if (!scene["animationIdea"].isNullOrBlank()) {
                            Text(text = "✨ Animation/Text: ${scene["animationIdea"]}", fontSize = 12.sp, color = PurpleAccent)
                        }
                        if (!scene["aiImagePrompt"].isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "🤖 AI Image Prompt: \"${scene["aiImagePrompt"]}\"", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

// 7. Script Analyzer Sub-Tool (8 Criteria Scores)
@Composable
fun AnalysisSubTool(viewModel: MainViewModel, project: ProjectEntity) {
    val analysisObj = remember(project.analysisJson) {
        try {
            JSONObject(project.analysisJson)
        } catch (e: Exception) {
            JSONObject()
        }
    }

    val overallScore = analysisObj.optInt("overallScore", 0)
    val hookScore = analysisObj.optInt("hookScore", 0)
    val clarityScore = analysisObj.optInt("clarityScore", 0)
    val structureScore = analysisObj.optInt("structureScore", 0)
    val engagementScore = analysisObj.optInt("engagementScore", 0)
    val pacingScore = analysisObj.optInt("pacingScore", 0)
    val valueScore = analysisObj.optInt("valueScore", 0)
    val repetitionScore = analysisObj.optInt("repetitionScore", 0)
    val ctaScore = analysisObj.optInt("ctaScore", 0)
    val summary = analysisObj.optString("summary", "")

    val strengthsArr = analysisObj.optJSONArray("strengths")
    val improvementsArr = analysisObj.optJSONArray("improvements")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "AI SCRIPT AUDIT & RETENTION ANALYSIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "8-dimension evaluation of viral watch time potential",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { viewModel.analyzeScriptAi(project.id) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(if (overallScore == 0) "Analyze" else "Re-Analyze", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (overallScore == 0) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No script analysis generated yet", color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.analyzeScriptAi(project.id) },
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("📊 Run AI Script Audit")
                        }
                    }
                }
            }
        } else {
            // Overall Score Card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (overallScore >= 80) GreenSuccess else AmberAccent, RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "COMPOSITE SCRIPT RATING", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (overallScore >= 85) "🌟 Excellent Retention Potential" else if (overallScore >= 70) "👍 Solid Script with Minor Fixes" else "⚠️ Needs Hook & Pacing Polish",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextPrimary)
                            )
                            if (summary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = summary, fontSize = 12.sp, color = TextSecondary)
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(if (overallScore >= 80) GreenSuccess.copy(alpha = 0.2f) else AmberAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$overallScore",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = if (overallScore >= 80) GreenSuccess else AmberAccent
                            )
                        }
                    }
                }
            }

            // 8 Criteria Progress Bars
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.fillMaxWidth().border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = "8-POINT CRITERIA BREAKDOWN", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        CriterionBar(label = "1. Opening Hook Power", score = hookScore)
                        CriterionBar(label = "2. Clarity & Simplicity", score = clarityScore)
                        CriterionBar(label = "3. Section Structure", score = structureScore)
                        CriterionBar(label = "4. Audience Engagement", score = engagementScore)
                        CriterionBar(label = "5. Pacing & Flow", score = pacingScore)
                        CriterionBar(label = "6. Value Density", score = valueScore)
                        CriterionBar(label = "7. Non-Repetition", score = repetitionScore)
                        CriterionBar(label = "8. CTA Effectiveness", score = ctaScore)
                    }
                }
            }

            // Strengths & Fixes
            if (strengthsArr != null && strengthsArr.length() > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "✅ KEY STRENGTHS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                            Spacer(modifier = Modifier.height(6.dp))
                            for (i in 0 until strengthsArr.length()) {
                                Text(
                                    text = "• ${strengthsArr.optString(i)}",
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (improvementsArr != null && improvementsArr.length() > 0) {
                item {
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkCard),
                        modifier = Modifier.fillMaxWidth().border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(text = "⚡ RECOMMENDED FIXES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmberAccent)
                            Spacer(modifier = Modifier.height(6.dp))
                            for (i in 0 until improvementsArr.length()) {
                                Text(
                                    text = "• ${improvementsArr.optString(i)}",
                                    fontSize = 13.sp,
                                    color = TextPrimary,
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CriterionBar(label: String, score: Int) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
            Text(
                text = "$score/100",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (score >= 80) GreenSuccess else if (score >= 65) AmberAccent else RedPrimary
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { (score / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = if (score >= 80) GreenSuccess else if (score >= 65) AmberAccent else RedPrimary,
            trackColor = DarkSurfaceVariant
        )
    }
}

// -------------------------------------------------------------
// TAB 5: HISTORY & VERSIONS TAB
// -------------------------------------------------------------
@Composable
fun HistoryVersionsTab(
    versions: List<ScriptVersionEntity>,
    onSaveNew: () -> Unit,
    onPreview: (ScriptVersionEntity) -> Unit,
    onRestore: (ScriptVersionEntity) -> Unit,
    onDelete: (Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "VERSION HISTORY & RESTORE POINTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${versions.size} saved iterations in project history",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = onSaveNew,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text("+ Save Version", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (versions.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No saved versions yet", color = TextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onSaveNew,
                            colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                        ) {
                            Text("Create First Version Snapshot")
                        }
                    }
                }
            }
        } else {
            items(versions, key = { it.id }) { version ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkCard),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = version.versionName,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Words: ${version.wordCount} • ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(version.createdAt))}",
                                fontSize = 11.sp,
                                color = TextMuted
                            )

                            if (version.note.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "📝 ${version.note}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = { onPreview(version) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Visibility, contentDescription = "Preview", tint = TextSecondary, modifier = Modifier.size(18.dp))
                            }

                            Button(
                                onClick = { onRestore(version) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DarkSurfaceVariant,
                                    contentColor = AmberAccent
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Restore", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// 1b. Creator Fun Zone (Meme, Comedy & Viral Tools)
// -------------------------------------------------------------
data class FunCreatorTool(
    val id: String,
    val name: String,
    val iconEmoji: String,
    val tag: String,
    val tagColor: Color,
    val description: String,
    val exampleSnippet: String
)

@Composable
fun FunZoneSubTool(
    viewModel: MainViewModel,
    project: ProjectEntity,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val funnyResult by viewModel.funnyToolResult.collectAsState()
    var showReplaceConfirmation by remember { mutableStateOf(false) }

    val funTools = listOf(
        FunCreatorTool(
            id = "Meme Mode",
            name = "Meme Mode",
            iconEmoji = "🔥",
            tag = "VIRAL POP CULTURE",
            tagColor = RedPrimary,
            description = "Infuses relatable modern internet memes, punchlines, and pop culture references.",
            exampleSnippet = "e.g. \"API ne bola: bhai aaj nahi. 💀\""
        ),
        FunCreatorTool(
            id = "Bhai Moment",
            name = "Bhai Moment",
            iconEmoji = "🤝",
            tag = "DESI CREATOR HUMOR",
            tagColor = AmberAccent,
            description = "Relatable everyday struggle, brotherly banter, and Hinglish punchlines.",
            exampleSnippet = "e.g. \"Sun bhai, code likhna aasan tha... deploy karna nahi.\""
        ),
        FunCreatorTool(
            id = "Roast My Script",
            name = "Roast My Script",
            iconEmoji = "🌶️",
            tag = "SAVAGE COMEDY ROAST",
            tagColor = PurpleAccent,
            description = "Lighthearted, hilarious roast of your clichés, tropes, and cringe transitions with funny fixes.",
            exampleSnippet = "Roast breakdown: Brutal critique + Cringe Radar + Fixes."
        ),
        FunCreatorTool(
            id = "Comedy Boost",
            name = "Comedy Boost",
            iconEmoji = "😂",
            tag = "STAND-UP TIMING",
            tagColor = BlueAccent,
            description = "Injects stand-up comedy pacing, unexpected funny analogies, callbacks, and one-liners.",
            exampleSnippet = "Elevates punchlines without derailing your educational content."
        ),
        FunCreatorTool(
            id = "Brainrot Mode",
            name = "Brainrot Mode",
            iconEmoji = "🧠",
            tag = "GEN-Z / SHORTS SLANG",
            tagColor = GreenSuccess,
            description = "Infuses playful viral slang (cooking, no cap, rizz, sigma lore, gigachad) for viral shorts.",
            exampleSnippet = "e.g. \"Bro really thought he could fix the bug in production. 😭\""
        ),
        FunCreatorTool(
            id = "Reaction Generator",
            name = "Reaction & SFX Generator",
            iconEmoji = "🎬",
            tag = "MEME SOUNDS & CUES",
            tagColor = AmberAccent,
            description = "Generates funny face reactions, zoom-in cues, and sound effect tags ([Vine Boom], [Awkward Pause]).",
            exampleSnippet = "Includes cues like [Record Scratch], [Emotional Damage SFX]."
        ),
        FunCreatorTool(
            id = "Expectation vs Reality",
            name = "Expectation vs Reality",
            iconEmoji = "🎭",
            tag = "COMEDY SEGMENT",
            tagColor = PurpleAccent,
            description = "Creates a hilarious side-by-side comedy segment comparing tutorial hype vs real-life pain.",
            exampleSnippet = "Generates ✨ Expectation vs 💀 Reality on-camera script."
        ),
        FunCreatorTool(
            id = "Shorts Punchline",
            name = "Shorts Punchlines",
            iconEmoji = "⚡",
            tag = "MIC-DROP CLOSERS",
            tagColor = RedPrimary,
            description = "5 ultra-punchy, high-retention comedic closing lines tailored for YouTube Shorts.",
            exampleSnippet = "Quick punchy lines to maximize replay loops."
        ),
        FunCreatorTool(
            id = "Deadpan Mode",
            name = "Deadpan Sarcasm",
            iconEmoji = "😐",
            tag = "DRY MONOTONE HUMOR",
            tagColor = BlueAccent,
            description = "Rewrites with brutally dry, emotionless sarcastic delivery.",
            exampleSnippet = "Delivers absurd truths with complete straight-faced composure."
        ),
        FunCreatorTool(
            id = "Savage But Friendly",
            name = "Savage But Friendly",
            iconEmoji = "😈",
            tag = "PLAYFUL TEASING",
            tagColor = AmberAccent,
            description = "Delightfully savage commentary and playful banter that keeps the audience hooked and smiling.",
            exampleSnippet = "Playful viewer roasts with warm creator energy."
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, PurpleAccent.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🎭 CREATOR FUN ZONE & MEME TOOLS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PurpleAccent,
                            letterSpacing = 1.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Inject stand-up comedy, viral pop-culture memes, funny sound-effect cues, and witty roasts into your YouTube scripts.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        items(funTools) { tool ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, DarkBorder, RoundedCornerShape(14.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text(text = tool.iconEmoji, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = tool.name,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                )
                                Text(
                                    text = tool.tag,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = tool.tagColor
                                )
                            }
                        }

                        Button(
                            onClick = {
                                viewModel.applyFunCreatorToolAi(project.id, tool.id)
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceVariant,
                                contentColor = RedPrimary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("Generate ✨", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = tool.description, fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = tool.exampleSnippet, fontSize = 11.sp, color = TextMuted, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                }
            }
        }
    }

    // Output Result Dialog / Preview Card
    if (funnyResult != null) {
        val (toolName, content) = funnyResult!!

        AlertDialog(
            onDismissRequest = { viewModel.clearFunnyToolResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🎭 $toolName Output", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                ) {
                    OutlinedTextField(
                        value = content,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = PurpleAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(content))
                            viewModel.showMessage("Copied to clipboard!")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant, contentColor = TextPrimary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Copy", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.applyFunnyResultToScript(project.id, toolName, content, replaceAll = false)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Append", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            showReplaceConfirmation = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedPrimary, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Replace", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearFunnyToolResult() }) {
                    Text("Close", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }

    // Confirmation for full replacement
    if (showReplaceConfirmation && funnyResult != null) {
        val (toolName, content) = funnyResult!!
        AlertDialog(
            onDismissRequest = { showReplaceConfirmation = false },
            title = { Text("Replace Entire Script?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Your current script will be replaced with the $toolName version. ScriptForge AI will automatically save a backup of your previous version in the History & Drafts tab.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.applyFunnyResultToScript(project.id, toolName, content, replaceAll = true)
                        showReplaceConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedPrimary)
                ) {
                    Text("Yes, Replace & Backup")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceConfirmation = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkCard
        )
    }
}
