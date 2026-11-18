package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.DiagnosticEngine
import com.example.domain.DiagnosticReport
import com.example.domain.PhoneticGuide
import com.example.domain.SessionSummary
import com.example.ui.theme.LocalSemanticColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionSummaryScreen(
    summary: SessionSummary,
    onPlayAudio: (String) -> Unit,
    onRetryMistakes: (List<Int>) -> Unit,
    onStartNewSession: () -> Unit,
    onExitToHome: () -> Unit
) {
    BackHandler { onExitToHome() }
    val semantics = LocalSemanticColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Итоги тренировки") },
                navigationIcon = {
                    IconButton(onClick = onExitToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "На главную")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = semantics.success)
                            Text(summary.mode.titleRu, style = MaterialTheme.typography.titleLarge)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SummaryStatBox("Верно", "${summary.correctCount}", Modifier.weight(1f))
                            SummaryStatBox("Ошибки", "${summary.incorrectCount}", Modifier.weight(1f))
                            SummaryStatBox("Точность", "${summary.accuracyPercent}%", Modifier.weight(1f))
                            SummaryStatBox(
                                "Ср. время",
                                String.format("%.1f с", summary.averageResponseMs / 1000.0),
                                Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            if (summary.difficultItems.isNotEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                    ) {
                        Text("Числа, вызвавшие трудности", style = MaterialTheme.typography.titleMedium)
                    }
                }
                items(summary.difficultItems) { outcome ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 640.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${outcome.exercise.displayTarget} → ${outcome.exercise.expectedFrench}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Ваш ответ: ${outcome.userAnswer}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Text(
                                    text = outcome.exercise.decompositionRu,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { onPlayAudio(outcome.exercise.audioFrenchText) }) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Прослушать")
                            }
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (summary.difficultItems.isNotEmpty()) {
                        Button(
                            onClick = {
                                onRetryMistakes(summary.difficultItems.map { it.exercise.targetNumber }.distinct())
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("summary_retry_mistakes_button")
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Повторить ошибки")
                        }
                    }
                    OutlinedButton(
                        onClick = onStartNewSession,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("summary_new_session_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Начать новую тренировку")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticReportScreen(
    report: DiagnosticReport,
    onApplyAndTrain: () -> Unit,
    onExitToHome: () -> Unit
) {
    BackHandler { onExitToHome() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Результаты диагностики") },
                navigationIcon = {
                    IconButton(onClick = onExitToHome) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "На главную")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Пройдено заданий: ${report.completedCount}", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SummaryStatBox(
                                "Узнавание",
                                report.recognitionAccuracy?.let { "$it%" } ?: "—",
                                Modifier.weight(1f)
                            )
                            SummaryStatBox(
                                "Письмо",
                                report.writingAccuracy?.let { "$it%" } ?: "—",
                                Modifier.weight(1f)
                            )
                            SummaryStatBox(
                                "Слух",
                                report.listeningAccuracy?.let { "$it%" } ?: "—",
                                Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "Среднее время ответа: ${String.format("%.1f с", report.averageLatencyMs / 1000.0)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (report.speakingAttemptedCount > 0) {
                            Text(
                                text = "Речевые задания (отдельно от основного балла): ${report.speakingAsrMatchedCount} из ${report.speakingAttemptedCount} распознано ASR.",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Персональный учебный план", style = MaterialTheme.typography.titleMedium)
                        Text(report.recommendedStage.titleRu, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                        Text(report.planSummaryRu, style = MaterialTheme.typography.bodyMedium)
                        if (report.weakCategories.isNotEmpty()) {
                            Text(
                                text = "Слабые диапазоны: ${report.weakCategories.joinToString(", ") { DiagnosticEngine.categoryLabelRu(it) }}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                        if (report.confusionPairs.isNotEmpty()) {
                            Text(
                                text = "Замеченные смешения: ${report.confusionPairs.joinToString(", ") { "${it.first}↔${it.second}" }}",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onApplyAndTrain,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Начать рекомендованную тренировку (${report.recommendedDifficulty.titleRu})")
                    }
                    OutlinedButton(
                        onClick = onExitToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("На главный экран")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GuidedRulesScreen(
    onPlayAudio: (String) -> Unit,
    onStartStage2Practice: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Этап 1 · Правила чисел") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(PhoneticGuide.guidedStage1Rules) { rule ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(rule.titleRu, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                Text(
                                    text = rule.rangeTag,
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        Text(rule.ruleExplanationRu, style = MaterialTheme.typography.bodyMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            rule.examples.forEach { (num, fr) ->
                                AssistChip(
                                    onClick = { onPlayAudio(fr) },
                                    label = { Text("$num = $fr") },
                                    leadingIcon = {
                                        Icon(
                                            Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Прослушать $fr",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onStartStage2Practice,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .height(50.dp)
                ) {
                    Text("Перейти к Этапу 2 · Карточки")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneticsScreen(
    onPlayAudio: (String) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Фонетика и МФА (fr-FR)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(PhoneticGuide.lessons) { lesson ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(lesson.titleRu, style = MaterialTheme.typography.titleMedium)
                        Text(
                            lesson.focusAreaRu,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(lesson.explanationRu, style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(4.dp))
                        lesson.examples.forEach { ex ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${ex.displayNumber} — ${ex.frenchText}  ${ex.ipa}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = ex.noteRu,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(onClick = { onPlayAudio(ex.frenchText) }) {
                                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Прослушать ${ex.frenchText}")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
