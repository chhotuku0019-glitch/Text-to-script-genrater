package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.service.RecordingStatus
import com.example.ui.components.FloatingRecordBar
import com.example.ui.components.StudioBottomBar
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NewProjectScreen
import com.example.ui.screens.ProjectWorkspaceScreen
import com.example.ui.screens.ProjectsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpeechRecordScreen
import com.example.ui.screens.TeleprompterScreen
import com.example.ui.screens.TextToScriptScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.Screen

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ScriptForgeAppContent(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun ScriptForgeAppContent(viewModel: MainViewModel) {
    val context = LocalContext.current
    val currentScreen by viewModel.currentScreen.collectAsState()
    val recordingState by viewModel.recordingState.collectAsState()
    val userMessage by viewModel.userMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userMessage) {
        if (userMessage != null) {
            snackbarHostState.showSnackbar(userMessage!!)
            viewModel.clearUserMessage()
        }
    }

    BackHandler {
        val handled = viewModel.navigateBack()
        if (!handled) {
            // let system exit
        }
    }

    val showBottomBar = when (currentScreen) {
        is Screen.Home, is Screen.Projects, is Screen.TextToScript, is Screen.Settings -> true
        is Screen.SpeechRecord -> (currentScreen as Screen.SpeechRecord).projectId == null
        else -> false
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                StudioBottomBar(
                    currentScreen = currentScreen,
                    onNavigate = { viewModel.navigateTo(it) }
                )
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val screen = currentScreen) {
                is Screen.Home -> {
                    HomeScreen(viewModel = viewModel)
                }
                is Screen.Projects -> {
                    ProjectsScreen(viewModel = viewModel)
                }
                is Screen.NewProject -> {
                    NewProjectScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.TextToScript -> {
                    TextToScriptScreen(
                        viewModel = viewModel,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.SpeechRecord -> {
                    SpeechRecordScreen(
                        viewModel = viewModel,
                        targetProjectId = screen.projectId,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.ProjectWorkspace -> {
                    ProjectWorkspaceScreen(
                        viewModel = viewModel,
                        projectId = screen.projectId,
                        initialTab = screen.initialTab,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.Teleprompter -> {
                    TeleprompterScreen(
                        viewModel = viewModel,
                        projectId = screen.projectId,
                        onBack = { viewModel.navigateBack() }
                    )
                }
                is Screen.Settings -> {
                    SettingsScreen(viewModel = viewModel)
                }
            }

            // Floating Recording Bar when recording in background on other screens
            val isRecordingOrPaused = recordingState.status == RecordingStatus.RECORDING || recordingState.status == RecordingStatus.PAUSED
            val isNotOnRecordScreen = currentScreen !is Screen.SpeechRecord

            if (isRecordingOrPaused && isNotOnRecordScreen) {
                Box(
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    FloatingRecordBar(
                        isRecording = recordingState.status == RecordingStatus.RECORDING,
                        isPaused = recordingState.status == RecordingStatus.PAUSED,
                        durationSeconds = recordingState.durationSeconds,
                        onPauseResume = {
                            if (recordingState.status == RecordingStatus.RECORDING) {
                                viewModel.pauseRecording(context)
                            } else {
                                viewModel.resumeRecording(context)
                            }
                        },
                        onStop = {
                            viewModel.stopRecording(context)
                        },
                        onClick = {
                            viewModel.navigateTo(Screen.SpeechRecord(recordingState.projectId))
                        }
                    )
                }
            }
        }
    }
}
