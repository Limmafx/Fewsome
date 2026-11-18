package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.TrainingMode
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DiagnosticReportScreen
import com.example.ui.screens.ExerciseScreen
import com.example.ui.screens.GuidedRulesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.PhoneticsScreen
import com.example.ui.screens.PracticeHubScreen
import com.example.ui.screens.SessionSummaryScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FrenchNumbersTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val prefs by viewModel.preferences.collectAsStateWithLifecycle()

            FrenchNumbersTheme(themePreference = prefs.themePreference) {
                FrenchNumbersApp(viewModel = viewModel)
            }
        }
    }
}

private data class MainTabItem(
    val labelRu: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun FrenchNumbersApp(viewModel: MainViewModel) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val latestDiagnostic by viewModel.latestDiagnostic.collectAsStateWithLifecycle()

    val selectedMode by viewModel.selectedMode.collectAsStateWithLifecycle()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsStateWithLifecycle()
    val selectedLength by viewModel.selectedLength.collectAsStateWithLifecycle()
    val selectedDailyDuration by viewModel.selectedDailyDuration.collectAsStateWithLifecycle()
    val selectedPhoneSubMode by viewModel.selectedTelephoneSubMode.collectAsStateWithLifecycle()
    val mobilePhonesOnly by viewModel.mobilePhonesOnly.collectAsStateWithLifecycle()
    val selectedListeningVariation by viewModel.selectedListeningVariation.collectAsStateWithLifecycle()
    val selectedPronunciationActivity by viewModel.selectedPronunciationActivity.collectAsStateWithLifecycle()

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTabIndex.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val sessionSummary by viewModel.sessionSummary.collectAsStateWithLifecycle()
    val diagnosticReport by viewModel.diagnosticReport.collectAsStateWithLifecycle()

    val ttsReady by viewModel.listeningTrainer.isReady.collectAsStateWithLifecycle()
    val ttsSpeaking by viewModel.listeningTrainer.isSpeaking.collectAsStateWithLifecycle()
    val ttsVoices by viewModel.listeningTrainer.voiceNames.collectAsStateWithLifecycle()
    val ttsStatus by viewModel.listeningTrainer.statusMessageRu.collectAsStateWithLifecycle()

    val isListeningAsr by viewModel.pronunciationTrainer.isListeningAsr.collectAsStateWithLifecycle()
    val isRecordingAudio by viewModel.pronunciationTrainer.isRecordingAudio.collectAsStateWithLifecycle()
    val isPlayingRecording by viewModel.pronunciationTrainer.isPlayingRecording.collectAsStateWithLifecycle()
    val hasRecordedSample by viewModel.pronunciationTrainer.hasRecordedSample.collectAsStateWithLifecycle()

    when (currentScreen) {
        AppScreen.EXERCISE -> {
            val session = activeSession
            if (session != null) {
                ExerciseScreen(
                    session = session,
                    isSpeakingTts = ttsSpeaking,
                    isListeningAsr = isListeningAsr,
                    isRecordingAudio = isRecordingAudio,
                    isPlayingRecording = isPlayingRecording,
                    hasRecordedSample = hasRecordedSample,
                    asrSupported = viewModel.pronunciationTrainer.isSpeechRecognitionSupported(),
                    onPlayAudio = { slow -> viewModel.playCurrentExerciseAudio(slow) },
                    onUpdateTextInput = viewModel::updateTextInput,
                    onSubmitDragOption = viewModel::submitDragAndDropOption,
                    onSelectOption = viewModel::selectDragOrChoiceOption,
                    onToggleAssemblyToken = viewModel::toggleAssemblyToken,
                    onMoveAssemblyToken = viewModel::moveAssemblyToken,
                    onResetAssembly = viewModel::resetAssembly,
                    onStartAsr = viewModel::startSpeechRecallAsr,
                    onToggleRecordSample = viewModel::startOrStopShadowRecording,
                    onPlayRecordedSample = { viewModel.pronunciationTrainer.playRecordedSample() },
                    onSubmitPronunciationOutcome = viewModel::submitPronunciationSelfAssessment,
                    onCheckAnswer = viewModel::submitCurrentAnswer,
                    onNextExercise = viewModel::nextExerciseOrFinish,
                    onFinishOrExit = {
                        if (session.outcomes.isNotEmpty() || session.mode == TrainingMode.DIAGNOSTIC) {
                            viewModel.finishCurrentSession()
                        } else {
                            viewModel.returnToMainTabs()
                        }
                    }
                )
            }
        }

        AppScreen.SESSION_SUMMARY -> {
            val summary = sessionSummary
            if (summary != null) {
                SessionSummaryScreen(
                    summary = summary,
                    onPlayAudio = { text -> viewModel.speakText(text) },
                    onRetryMistakes = { nums ->
                        viewModel.startTrainingSession(
                            mode = TrainingMode.MISTAKES_REVIEW,
                            customQueueNumbers = nums
                        )
                    },
                    onStartNewSession = { viewModel.startTrainingSession() },
                    onExitToHome = viewModel::returnToMainTabs
                )
            }
        }

        AppScreen.DIAGNOSTIC_REPORT -> {
            val report = diagnosticReport
            if (report != null) {
                DiagnosticReportScreen(
                    report = report,
                    onApplyAndTrain = {
                        viewModel.selectDifficulty(report.recommendedDifficulty)
                        viewModel.startTrainingSession(
                            mode = TrainingMode.DAILY_TRAINING,
                            difficulty = report.recommendedDifficulty
                        )
                    },
                    onExitToHome = viewModel::returnToMainTabs
                )
            }
        }

        AppScreen.GUIDED_RULES -> {
            GuidedRulesScreen(
                onPlayAudio = { text -> viewModel.speakText(text) },
                onStartStage2Practice = {
                    viewModel.selectMode(TrainingMode.DRAG_AND_DROP)
                    viewModel.startTrainingSession(mode = TrainingMode.DRAG_AND_DROP)
                },
                onBack = viewModel::returnToMainTabs
            )
        }

        AppScreen.PHONETICS -> {
            PhoneticsScreen(
                onPlayAudio = { text -> viewModel.speakText(text) },
                onBack = viewModel::returnToMainTabs
            )
        }

        AppScreen.MAIN_TABS -> {
            val tabs = listOf(
                MainTabItem("Тренировка", Icons.Filled.PlayCircle, Icons.Outlined.PlayCircle),
                MainTabItem("Практика", Icons.Filled.FitnessCenter, Icons.Outlined.FitnessCenter),
                MainTabItem("Статистика", Icons.Filled.BarChart, Icons.Outlined.BarChart),
                MainTabItem("Настройки", Icons.Filled.Settings, Icons.Outlined.Settings)
            )

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    bottomBar = {
                        if (!useNavRail) {
                            NavigationBar {
                                tabs.forEachIndexed { idx, tab ->
                                    NavigationBarItem(
                                        selected = selectedTab == idx,
                                        onClick = { viewModel.selectTab(idx) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selectedTab == idx) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.labelRu
                                            )
                                        },
                                        label = { Text(tab.labelRu) }
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavRail) {
                            NavigationRail {
                                tabs.forEachIndexed { idx, tab ->
                                    NavigationRailItem(
                                        selected = selectedTab == idx,
                                        onClick = { viewModel.selectTab(idx) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selectedTab == idx) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.labelRu
                                            )
                                        },
                                        label = { Text(tab.labelRu) }
                                    )
                                }
                            }
                        }

                        when (selectedTab) {
                            0 -> HomeScreen(
                                preferences = prefs,
                                metrics = metrics,
                                latestDiagnostic = latestDiagnostic,
                                selectedMode = selectedMode,
                                selectedDifficulty = selectedDifficulty,
                                selectedLength = selectedLength,
                                selectedDailyDuration = selectedDailyDuration,
                                selectedPhoneSubMode = selectedPhoneSubMode,
                                mobilePhonesOnly = mobilePhonesOnly,
                                selectedListeningVariation = selectedListeningVariation,
                                selectedPronunciationActivity = selectedPronunciationActivity,
                                onSelectMode = viewModel::selectMode,
                                onSelectDifficulty = viewModel::selectDifficulty,
                                onSelectLength = viewModel::selectLength,
                                onSelectDailyDuration = viewModel::selectDailyDuration,
                                onSelectPhoneSubMode = viewModel::selectTelephoneSubMode,
                                onToggleMobileOnly = viewModel::setMobilePhonesOnly,
                                onSelectListeningVariation = viewModel::selectListeningVariation,
                                onSelectPronunciationActivity = viewModel::selectPronunciationActivity,
                                onCustomRangeChanged = viewModel::setCustomRange,
                                onStartTraining = { viewModel.startTrainingSession() },
                                onStartDaily = { viewModel.startTrainingSession(mode = TrainingMode.DAILY_TRAINING) },
                                onStartDiagnostic = { viewModel.startTrainingSession(mode = TrainingMode.DIAGNOSTIC) },
                                onDismissDiagnostic = viewModel::dismissDiagnosticBanner,
                                onOpenMistakesReview = { viewModel.startTrainingSession(mode = TrainingMode.MISTAKES_REVIEW) },
                                onOpenStatistics = { viewModel.selectTab(2) },
                                modifier = Modifier.weight(1f)
                            )

                            1 -> PracticeHubScreen(
                                metrics = metrics,
                                onLaunchMode = { mode -> viewModel.startTrainingSession(mode = mode) },
                                onOpenPhonetics = viewModel::openPhonetics,
                                onOpenGuidedRules = viewModel::openGuidedRules,
                                modifier = Modifier.weight(1f)
                            )

                            2 -> DashboardScreen(
                                metrics = metrics,
                                preferences = prefs,
                                latestDiagnostic = latestDiagnostic,
                                onStartConfusionDrill = { viewModel.startTrainingSession(mode = TrainingMode.CONFUSION_DRILL) },
                                onStartAssessment = { viewModel.startTrainingSession(mode = TrainingMode.ASSESSMENT) },
                                modifier = Modifier.weight(1f)
                            )

                            else -> SettingsScreen(
                                preferences = prefs,
                                ttsReady = ttsReady,
                                ttsVoicesCount = ttsVoices.size,
                                ttsStatusMessage = ttsStatus,
                                onSelectTheme = viewModel::setTheme,
                                onToggleVoiceExercises = viewModel::setVoiceExercisesEnabled,
                                onToggleVaryVoices = viewModel::setVaryVoices,
                                onUpdateSpeedSettings = viewModel::setSpeedSettings,
                                onResetProgress = viewModel::resetAllProgress,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}
