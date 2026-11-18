package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SlowMotionVideo
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import com.example.domain.Exercise
import com.example.domain.ListeningVariation
import com.example.domain.PronunciationActivity
import com.example.domain.SpeakingOutcomeType
import com.example.domain.TrainingMode
import com.example.domain.WordToken
import com.example.ui.ActiveSessionUiState
import com.example.ui.theme.LocalSemanticColors
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseScreen(
    session: ActiveSessionUiState,
    isSpeakingTts: Boolean,
    isListeningAsr: Boolean,
    isRecordingAudio: Boolean,
    isPlayingRecording: Boolean,
    hasRecordedSample: Boolean,
    asrSupported: Boolean,
    onPlayAudio: (slow: Boolean) -> Unit,
    onUpdateTextInput: (String) -> Unit,
    onSubmitDragOption: (String) -> Unit,
    onSelectOption: (String) -> Unit,
    onToggleAssemblyToken: (WordToken) -> Unit,
    onMoveAssemblyToken: (Int, Int) -> Unit,
    onResetAssembly: () -> Unit,
    onStartAsr: () -> Unit,
    onToggleRecordSample: (() -> Unit) -> Unit,
    onPlayRecordedSample: () -> Unit,
    onSubmitPronunciationOutcome: (Boolean, SpeakingOutcomeType) -> Unit,
    onCheckAnswer: () -> Unit,
    onNextExercise: () -> Unit,
    onFinishOrExit: () -> Unit
) {
    BackHandler { onFinishOrExit() }

    val context = LocalContext.current
    val semantics = LocalSemanticColors.current
    val exercise = session.currentExercise
    val feedback = session.feedback
    val listState = rememberLazyListState()
    var permissionDeniedNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(feedback) {
        if (feedback != null) {
            listState.animateScrollToItem(3)
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            permissionDeniedNote = "Доступ к микрофону отклонён. Устные задания остаются необязательными и не блокируют тренировку."
        } else {
            permissionDeniedNote = null
        }
    }

    fun ensureMicPermission(onGranted: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            permissionDeniedNote = null
            onGranted()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val answeredCount = session.outcomes.size
    val sessionAcc = if (answeredCount == 0) 100 else {
        ((session.outcomes.count { it.isCorrect } * 100.0) / answeredCount).toInt()
    }
    val progressFraction = if (session.plannedCount == Int.MAX_VALUE) {
        0f
    } else {
        ((session.currentIndex + if (feedback != null) 1 else 0).toFloat() / session.plannedCount.coerceAtLeast(1)).coerceIn(0f, 1f)
    }

    val isSpeechRecallLockedBeforeAttempt =
        exercise is Exercise.Pronunciation &&
            exercise.activity == PronunciationActivity.SPEECH_RECALL &&
            feedback == null

    Scaffold(
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (session.plannedCount == Int.MAX_VALUE) {
                                "Задание ${session.currentIndex + 1}"
                            } else {
                                "Задание ${session.currentIndex + 1} / ${session.plannedCount}"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.testTag("exercise_counter_text")
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Серия: ${session.sessionStreak}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "$sessionAcc%",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    if (session.plannedCount != Int.MAX_VALUE) {
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onFinishOrExit,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("exercise_exit_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (session.mode == TrainingMode.DIAGNOSTIC) "Завершить" else "Выйти")
                    }

                    FilledTonalButton(
                        onClick = { onPlayAudio(false) },
                        enabled = !isSpeechRecallLockedBeforeAttempt,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("exercise_audio_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Прослушать французское произношение"
                        )
                    }

                    if (feedback == null) {
                        if (exercise !is Exercise.Pronunciation) {
                            Button(
                                onClick = onCheckAnswer,
                                enabled = canSubmit(session),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("exercise_check_button")
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Проверить")
                            }
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    } else {
                        Button(
                            onClick = onNextExercise,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("exercise_next_button")
                        ) {
                            Text("Далее")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("exercise_lazy_column"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = session.mode.titleRu,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = exercise.promptRu,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    if (exercise.targetTimeMs != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Целевой темп: ${String.format("%.1f", exercise.targetTimeMs!! / 1000.0)} с",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }

            // Target Display Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val isAudioOnly = exercise is Exercise.Listening ||
                            (exercise is Exercise.Telephone && exercise.isDictationListening)

                        if (isAudioOnly && feedback == null) {
                            Icon(
                                Icons.Filled.GraphicEq,
                                contentDescription = "Аудиозадание",
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = { onPlayAudio(false) },
                                    modifier = Modifier.testTag("listening_play_button")
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isSpeakingTts) "Звучит..." else "Прослушать")
                                }
                                OutlinedButton(
                                    onClick = { onPlayAudio(true) },
                                    modifier = Modifier.testTag("listening_slow_button")
                                ) {
                                    Icon(Icons.Filled.SlowMotionVideo, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("0.7× Медленно")
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Повторов: ${session.audioReplays}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val displayString = if (isAudioOnly && feedback != null) {
                                if (exercise is Exercise.Telephone) exercise.pairs.joinToString(" ")
                                else exercise.targetNumber.toString()
                            } else {
                                exercise.displayTarget
                            }
                            Text(
                                text = displayString,
                                style = if (displayString.length > 10) {
                                    MaterialTheme.typography.headlineLarge
                                } else {
                                    MaterialTheme.typography.displayLarge
                                },
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.testTag("exercise_target_display")
                            )
                        }
                    }
                }
            }

            // Exercise-specific interaction UI
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (exercise) {
                        is Exercise.DragAndDrop -> {
                            DragAndDropSection(
                                exercise = exercise,
                                feedback = feedback,
                                selectedOption = session.selectedOption,
                                onDropOrTapOption = onSubmitDragOption
                            )
                        }

                        is Exercise.Writing -> {
                            OutlinedTextField(
                                value = session.textInput,
                                onValueChange = onUpdateTextInput,
                                label = { Text("Напиши число по-французски") },
                                placeholder = { Text("Например: quatre vingt onze") },
                                singleLine = true,
                                enabled = feedback == null,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onCheckAnswer() }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("writing_text_input")
                            )
                            Text(
                                text = "Диакритика (é, ô) необязательна: «zero» принимается как «zéro», пробелы и дефисы равнозначны.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        is Exercise.WordAssembly -> {
                            WordAssemblySection(
                                pool = exercise.shuffledPool,
                                assembled = session.assembledTokens,
                                enabled = feedback == null,
                                onToggleToken = onToggleAssemblyToken,
                                onMoveToken = onMoveAssemblyToken,
                                onReset = onResetAssembly
                            )
                        }

                        is Exercise.Listening -> {
                            when (exercise.variation) {
                                ListeningVariation.MULTIPLE_CHOICE_NUMBERS -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        exercise.numericChoices.chunked(2).forEach { row ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                row.forEach { num ->
                                                    val isSelected = session.selectedOption == num.toString()
                                                    OutlinedButton(
                                                        onClick = {
                                                            if (feedback == null) {
                                                                onSelectOption(num.toString())
                                                            }
                                                        },
                                                        colors = if (isSelected) {
                                                            ButtonDefaults.outlinedButtonColors(
                                                                containerColor = MaterialTheme.colorScheme.primaryContainer
                                                            )
                                                        } else ButtonDefaults.outlinedButtonColors(),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(56.dp)
                                                            .testTag("listening_choice_$num")
                                                    ) {
                                                        Text(
                                                            num.toString(),
                                                            style = MaterialTheme.typography.headlineMedium
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                ListeningVariation.TYPED_DIGITS -> {
                                    OutlinedTextField(
                                        value = session.textInput,
                                        onValueChange = { raw ->
                                            onUpdateTextInput(raw.filter { it.isDigit() }.take(4))
                                        },
                                        label = { Text("Введите услышанное число цифрами") },
                                        singleLine = true,
                                        enabled = feedback == null,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(onDone = { onCheckAnswer() }),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("listening_digits_input")
                                    )
                                }
                            }
                        }

                        is Exercise.Telephone -> {
                            if (exercise.isDictationListening) {
                                OutlinedTextField(
                                    value = session.textInput,
                                    onValueChange = { raw ->
                                        onUpdateTextInput(raw.filter { it.isDigit() || it == ' ' }.take(14))
                                    },
                                    label = { Text("Введите 10 цифр номера (сохраняя 0)") },
                                    placeholder = { Text("06 12 34 56 78") },
                                    singleLine = true,
                                    enabled = feedback == null,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Phone,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = { onCheckAnswer() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("phone_dictation_input")
                                )
                            } else {
                                if (exercise.wordOptionsPool.isNotEmpty() && session.textInput.isBlank()) {
                                    WordAssemblySection(
                                        pool = exercise.wordOptionsPool,
                                        assembled = session.assembledTokens,
                                        enabled = feedback == null,
                                        onToggleToken = onToggleAssemblyToken,
                                        onMoveToken = onMoveAssemblyToken,
                                        onReset = onResetAssembly
                                    )
                                    Text(
                                        text = "Или введите словами вручную:",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedTextField(
                                    value = session.textInput,
                                    onValueChange = onUpdateTextInput,
                                    label = { Text("Напишите телефон по-французски") },
                                    placeholder = { Text("zéro six, douze...") },
                                    enabled = feedback == null,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { onCheckAnswer() }),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("phone_words_input")
                                )
                            }
                        }

                        is Exercise.Pronunciation -> {
                            PronunciationSection(
                                exercise = exercise,
                                feedback = feedback,
                                isListeningAsr = isListeningAsr,
                                isRecordingAudio = isRecordingAudio,
                                isPlayingRecording = isPlayingRecording,
                                hasRecordedSample = hasRecordedSample,
                                asrSupported = asrSupported,
                                asrStatusMessage = session.asrStatusMessage,
                                asrTranscript = session.asrTranscript,
                                permissionDeniedNote = permissionDeniedNote,
                                onPlayReference = onPlayAudio,
                                onStartAsr = { ensureMicPermission(onStartAsr) },
                                onToggleRecord = {
                                    ensureMicPermission {
                                        onToggleRecordSample {
                                            permissionDeniedNote = "Необходим доступ к микрофону для записи."
                                        }
                                    }
                                },
                                onPlayRecordedSample = onPlayRecordedSample,
                                onSelfAssess = onSubmitPronunciationOutcome
                            )
                        }
                    }
                }
            }

            // Rich Feedback Section
            if (feedback != null) {
                item {
                    val containerColor = if (feedback.isCorrect) {
                        semantics.successContainer
                    } else {
                        semantics.errorContainer
                    }
                    val contentColor = if (feedback.isCorrect) {
                        semantics.success
                    } else {
                        semantics.error
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                            .testTag("exercise_feedback_card"),
                        colors = CardDefaults.cardColors(containerColor = containerColor)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (feedback.isCorrect) Icons.Filled.Check else Icons.Filled.Close,
                                        contentDescription = null,
                                        tint = contentColor
                                    )
                                    Text(
                                        text = if (feedback.isCorrect) "Верно!" else "Есть ошибка",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = contentColor
                                    )
                                }
                                Text(
                                    text = String.format("%.1f с", feedback.responseTimeMs / 1000.0),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = contentColor
                                )
                            }

                            Text(
                                text = "Правильный ответ: ${feedback.expectedFrench}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "МФА (IPA): ${feedback.ipa}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Структура: ${feedback.decompositionRu}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (feedback.metSpeedTarget != null) {
                                Text(
                                    text = if (feedback.metSpeedTarget) "✓ Уложились в целевой темп!" else "Ответ верный, но медленнее целевого темпа.",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (feedback.metSpeedTarget) semantics.success else MaterialTheme.colorScheme.secondary
                                )
                            }

                            feedback.mistakeAnalysis?.let { analysis ->
                                Surface(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = analysis.category.titleRu,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(analysis.explanationRu, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }

                            OutlinedButton(onClick = { onPlayAudio(false) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Прослушать ещё раз")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DragAndDropSection(
    exercise: Exercise.DragAndDrop,
    feedback: com.example.ui.ExerciseFeedback?,
    selectedOption: String?,
    onDropOrTapOption: (String) -> Unit
) {
    val semantics = LocalSemanticColors.current
    var dropZoneBounds by remember { mutableStateOf<Rect?>(null) }
    var isHoveringDropZone by remember { mutableStateOf(false) }

    val dropZoneBg by animateColorAsState(
        targetValue = when {
            feedback != null && feedback.isCorrect -> semantics.successContainer
            feedback != null && !feedback.isCorrect -> semantics.errorContainer
            isHoveringDropZone -> MaterialTheme.colorScheme.primaryContainer
            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        label = "dropZoneBg"
    )
    val dropZoneBorderColor by animateColorAsState(
        targetValue = when {
            feedback != null && feedback.isCorrect -> semantics.success
            feedback != null && !feedback.isCorrect -> semantics.error
            isHoveringDropZone -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.outline
        },
        label = "dropZoneBorder"
    )
    val pulseScale by animateFloatAsState(
        targetValue = when {
            feedback?.isCorrect == true -> 1.04f
            isHoveringDropZone -> 1.02f
            else -> 1f
        },
        animationSpec = spring(),
        label = "dropZoneScale"
    )

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!exercise.contrastExplanationRu.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = "Контрастная пара: выберите точное чтение числа",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Drop Zone
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(82.dp)
                .scale(pulseScale)
                .onGloballyPositioned { coords ->
                    dropZoneBounds = coords.boundsInWindow()
                }
                .testTag("drag_drop_zone"),
            color = dropZoneBg,
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(2.dp, dropZoneBorderColor)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = when {
                        feedback != null -> feedback.userAnswerDisplay
                        isHoveringDropZone -> "Отпустите карточку здесь"
                        selectedOption != null -> selectedOption
                        else -> "Перетащите карточку сюда (или нажмите ниже)"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // 4 Draggable & Tappable Cards
        exercise.options.forEachIndexed { idx, option ->
            var dragOffset by remember(exercise.id, option) { mutableStateOf(Offset.Zero) }
            var isDragging by remember(exercise.id, option) { mutableStateOf(false) }
            var cardOriginInWindow by remember { mutableStateOf(Offset.Zero) }

            val scale by animateFloatAsState(
                targetValue = if (isDragging) 1.06f else 1f,
                animationSpec = spring(),
                label = "cardScale"
            )

            val cardBg = when {
                feedback != null && option == exercise.expectedFrench -> semantics.successContainer
                feedback != null && option == selectedOption && !feedback.isCorrect -> semantics.errorContainer
                else -> MaterialTheme.colorScheme.surface
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (isDragging) 20f else 1f)
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                    .scale(scale)
                    .onGloballyPositioned { coords ->
                        cardOriginInWindow = coords.boundsInWindow().center
                    }
                    .then(
                        if (feedback == null) {
                            Modifier
                                .pointerInput(exercise.id, option) {
                                    detectDragGestures(
                                        onDragStart = { isDragging = true },
                                        onDragEnd = {
                                            val droppedCenter = cardOriginInWindow + dragOffset
                                            val inside = dropZoneBounds?.contains(droppedCenter) == true
                                            isDragging = false
                                            isHoveringDropZone = false
                                            dragOffset = Offset.Zero
                                            if (inside) {
                                                onDropOrTapOption(option)
                                            }
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                            isHoveringDropZone = false
                                            dragOffset = Offset.Zero
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount
                                            val currentCenter = cardOriginInWindow + dragOffset
                                            isHoveringDropZone = dropZoneBounds?.contains(currentCenter) == true
                                        }
                                    )
                                }
                                .clickable { onDropOrTapOption(option) }
                        } else Modifier
                    )
                    .testTag("drag_option_$idx"),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 10.dp else 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordAssemblySection(
    pool: List<WordToken>,
    assembled: List<WordToken>,
    enabled: Boolean,
    onToggleToken: (WordToken) -> Unit,
    onMoveToken: (Int, Int) -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Собранный ответ:", style = MaterialTheme.typography.labelLarge)
            TextButton(
                onClick = onReset,
                enabled = enabled && assembled.isNotEmpty(),
                modifier = Modifier.testTag("assembly_reset_button")
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Сбросить")
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("assembly_answer_tray"),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
            shape = MaterialTheme.shapes.medium,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {
            if (assembled.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нажимайте на слова ниже в нужном порядке",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                FlowRow(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    assembled.forEachIndexed { idx, token ->
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                if (enabled && idx > 0) {
                                    IconButton(
                                        onClick = { onMoveToken(idx, -1) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Сдвинуть влево"
                                        )
                                    }
                                }
                                Text(
                                    text = token.text,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .clickable(enabled = enabled) { onToggleToken(token) }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                                if (enabled && idx < assembled.lastIndex) {
                                    IconButton(
                                        onClick = { onMoveToken(idx, 1) },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Сдвинуть вправо"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text("Доступные карточки:", style = MaterialTheme.typography.labelLarge)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            pool.forEach { token ->
                val isUsed = assembled.any { it.id == token.id }
                OutlinedButton(
                    onClick = { onToggleToken(token) },
                    enabled = enabled && !isUsed,
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("assembly_pool_${token.text}")
                ) {
                    Text(token.text, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun PronunciationSection(
    exercise: Exercise.Pronunciation,
    feedback: com.example.ui.ExerciseFeedback?,
    isListeningAsr: Boolean,
    isRecordingAudio: Boolean,
    isPlayingRecording: Boolean,
    hasRecordedSample: Boolean,
    asrSupported: Boolean,
    asrStatusMessage: String?,
    asrTranscript: String?,
    permissionDeniedNote: String?,
    onPlayReference: (Boolean) -> Unit,
    onStartAsr: () -> Unit,
    onToggleRecord: () -> Unit,
    onPlayRecordedSample: () -> Unit,
    onSelfAssess: (Boolean, SpeakingOutcomeType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (exercise.activity == PronunciationActivity.SHADOWING) {
            Text(
                text = "Эталон: ${exercise.expectedFrench} (${exercise.ipa})",
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(onClick = { onPlayReference(false) }) {
                    Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Обычная (1.0×)")
                }
                OutlinedButton(onClick = { onPlayReference(true) }) {
                    Icon(Icons.Filled.SlowMotionVideo, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Медленно (0.7×)")
                }
            }
        } else {
            Text(
                text = "Произнесите число по-французски по памяти (озвучка откроется после попытки).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Recording & ASR controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (exercise.activity == PronunciationActivity.SPEECH_RECALL && asrSupported) {
                Button(
                    onClick = onStartAsr,
                    enabled = feedback == null && !isListeningAsr,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pronunciation_asr_button")
                ) {
                    Icon(Icons.Filled.Mic, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (isListeningAsr) "Слушаю..." else "Распознать речь")
                }
            }

            OutlinedButton(
                onClick = onToggleRecord,
                enabled = feedback == null,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("pronunciation_record_button")
            ) {
                Icon(
                    if (isRecordingAudio) Icons.Filled.Stop else Icons.Filled.Mic,
                    contentDescription = null
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isRecordingAudio) "Стоп запись" else "Записать себя")
            }
        }

        if (hasRecordedSample) {
            FilledTonalButton(
                onClick = onPlayRecordedSample,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(if (isPlayingRecording) "Воспроизведение..." else "Прослушать свою запись и сравнить")
            }
        }

        if (!asrStatusMessage.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = asrStatusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        if (!permissionDeniedNote.isNullOrBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = permissionDeniedNote,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Text(
            text = "Распознавание речи (ASR) может использовать системный сервис устройства и фиксирует только распознанный текст — это не оценка фонетики или акцента.",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (feedback == null) {
            val outcomeType = if (asrTranscript != null) {
                SpeakingOutcomeType.ASR_TRANSCRIPTION
            } else if (hasRecordedSample) {
                SpeakingOutcomeType.MANUAL_RECORDING
            } else {
                SpeakingOutcomeType.SELF_REPORTED
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { onSelfAssess(true, outcomeType) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pronunciation_success_button")
                ) {
                    Text("Получилось")
                }
                OutlinedButton(
                    onClick = { onSelfAssess(false, outcomeType) },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("pronunciation_retry_button")
                ) {
                    Text("Нужно ещё")
                }
            }
        }
    }
}

private fun canSubmit(session: ActiveSessionUiState): Boolean {
    return when (val ex = session.currentExercise) {
        is Exercise.DragAndDrop -> session.selectedOption != null
        is Exercise.Writing -> session.textInput.isNotBlank()
        is Exercise.WordAssembly -> session.assembledTokens.isNotEmpty()
        is Exercise.Listening -> when (ex.variation) {
            ListeningVariation.MULTIPLE_CHOICE_NUMBERS -> session.selectedOption != null
            ListeningVariation.TYPED_DIGITS -> session.textInput.isNotBlank()
        }
        is Exercise.Telephone -> if (ex.isDictationListening) {
            session.textInput.any { it.isDigit() }
        } else {
            session.assembledTokens.isNotEmpty() || session.textInput.isNotBlank()
        }
        is Exercise.Pronunciation -> false
    }
}
