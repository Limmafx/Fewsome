package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DiagnosticSnapshotEntity
import com.example.data.local.UserPreferences
import com.example.domain.DailyDuration
import com.example.domain.DashboardMetrics
import com.example.domain.DifficultyLevel
import com.example.domain.ListeningVariation
import com.example.domain.PedagogicalStage
import com.example.domain.PronunciationActivity
import com.example.domain.SessionLength
import com.example.domain.TelephoneSubMode
import com.example.domain.TrainingMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    preferences: UserPreferences,
    metrics: DashboardMetrics,
    latestDiagnostic: DiagnosticSnapshotEntity?,
    selectedMode: TrainingMode,
    selectedDifficulty: DifficultyLevel,
    selectedLength: SessionLength,
    selectedDailyDuration: DailyDuration,
    selectedPhoneSubMode: TelephoneSubMode,
    mobilePhonesOnly: Boolean,
    selectedListeningVariation: ListeningVariation,
    selectedPronunciationActivity: PronunciationActivity,
    onSelectMode: (TrainingMode) -> Unit,
    onSelectDifficulty: (DifficultyLevel) -> Unit,
    onSelectLength: (SessionLength) -> Unit,
    onSelectDailyDuration: (DailyDuration) -> Unit,
    onSelectPhoneSubMode: (TelephoneSubMode) -> Unit,
    onToggleMobileOnly: (Boolean) -> Unit,
    onSelectListeningVariation: (ListeningVariation) -> Unit,
    onSelectPronunciationActivity: (PronunciationActivity) -> Unit,
    onCustomRangeChanged: (Int, Int) -> Unit,
    onStartTraining: () -> Unit,
    onStartDaily: () -> Unit,
    onStartDiagnostic: () -> Unit,
    onDismissDiagnostic: () -> Unit,
    onOpenMistakesReview: () -> Unit,
    onOpenStatistics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryModes = listOf(
        TrainingMode.DRAG_AND_DROP,
        TrainingMode.WRITING,
        TrainingMode.ACTIVE_RECALL,
        TrainingMode.WORD_ASSEMBLY,
        TrainingMode.LISTENING,
        TrainingMode.PRONUNCIATION,
        TrainingMode.TELEPHONE,
        TrainingMode.CONTEXTUAL,
        TrainingMode.SPEED_FLUENCY,
        TrainingMode.GUIDED_RULES
    )

    var customMinText by remember(preferences.customMin) { mutableStateOf(preferences.customMin.toString()) }
    var customMaxText by remember(preferences.customMax) { mutableStateOf(preferences.customMax.toString()) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("home_lazy_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Editorial Card
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_paris_numbers_hero),
                        contentDescription = "Иллюстрация French Numbers Trainer",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.LocalFireDepartment,
                                        contentDescription = null,
                                        tint = Color(0xFFF4C95D),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Серия: ${preferences.currentStreak} · Этап ${preferences.currentStageNumber}/6",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "French Numbers Trainer",
                            style = MaterialTheme.typography.headlineLarge,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Числа, телефоны (fr-FR), слух и активное воспроизведение",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
                }
            }
        }

        // Optional Diagnostic Prompt
        if (!preferences.diagnosticPromptDismissed && latestDiagnostic == null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
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
                                    Icons.Filled.Assessment,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Диагностика знаний",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                            IconButton(
                                onClick = onDismissDiagnostic,
                                modifier = Modifier.testTag("dismiss_diagnostic_button")
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "Пропустить диагностику",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                        Text(
                            text = "24 стратифицированных задания (узнавание, письмо, слух) помогут составить ваш личный план обучения. Можно прервать в любой момент.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onStartDiagnostic,
                                modifier = Modifier.testTag("start_diagnostic_button")
                            ) {
                                Text("Пройти диагностику")
                            }
                            OutlinedButton(onClick = onDismissDiagnostic) {
                                Text("Пропустить")
                            }
                        }
                    }
                }
            }
        }

        // Daily Training Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.CalendarToday, contentDescription = null)
                            Text("Ежедневная тренировка", style = MaterialTheme.typography.titleMedium)
                        }
                        if (metrics.overdueReviewCount > 0) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondary,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Повтор: ${metrics.overdueReviewCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        DailyDuration.entries.forEach { dur ->
                            FilterChip(
                                selected = selectedDailyDuration == dur,
                                onClick = { onSelectDailyDuration(dur) },
                                label = { Text("${dur.labelRu} (${dur.minutesLabel})") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onStartDaily,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_daily_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Начать ежедневную сессию")
                    }
                }
            }
        }

        // Training Mode Selector
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Режим тренировки", style = MaterialTheme.typography.titleLarge)
                primaryModes.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { mode ->
                            val isSelected = selectedMode == mode
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.medium)
                                    .clickable { onSelectMode(mode) }
                                    .testTag("mode_card_${mode.name}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surface
                                    }
                                ),
                                border = if (!isSelected) {
                                    androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                } else null
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Icon(
                                        imageVector = iconForMode(mode),
                                        contentDescription = null,
                                        tint = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = mode.titleRu,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        }
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mode.subtitleRu,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Mode-Specific Options
        if (selectedMode == TrainingMode.TELEPHONE) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Формат телефонных номеров", style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            TelephoneSubMode.entries.forEach { sub ->
                                FilterChip(
                                    selected = selectedPhoneSubMode == sub,
                                    onClick = { onSelectPhoneSubMode(sub) },
                                    label = { Text(sub.titleRu) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Только мобильные (06 / 07)", style = MaterialTheme.typography.bodyMedium)
                            Switch(checked = mobilePhonesOnly, onCheckedChange = onToggleMobileOnly)
                        }
                    }
                }
            }
        } else if (selectedMode == TrainingMode.LISTENING) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Вариант аудирования", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ListeningVariation.entries.forEach { v ->
                                FilterChip(
                                    selected = selectedListeningVariation == v,
                                    onClick = { onSelectListeningVariation(v) },
                                    label = { Text(v.titleRu) }
                                )
                            }
                        }
                    }
                }
            }
        } else if (selectedMode == TrainingMode.PRONUNCIATION) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Тип устной практики", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PronunciationActivity.entries.forEach { act ->
                                FilterChip(
                                    selected = selectedPronunciationActivity == act,
                                    onClick = { onSelectPronunciationActivity(act) },
                                    label = { Text(act.titleRu) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Difficulty & Range Selector
        if (selectedMode != TrainingMode.TELEPHONE && selectedMode != TrainingMode.GUIDED_RULES) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Уровень сложности и диапазон", style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DifficultyLevel.entries.forEach { diff ->
                                FilterChip(
                                    selected = selectedDifficulty == diff,
                                    onClick = { onSelectDifficulty(diff) },
                                    label = { Text("${diff.titleRu} (${diff.rangeLabel})") },
                                    modifier = Modifier.testTag("diff_chip_${diff.name}")
                                )
                            }
                        }
                        if (selectedDifficulty == DifficultyLevel.CUSTOM) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = customMinText,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(4)
                                        customMinText = digits
                                        val minV = digits.toIntOrNull() ?: 0
                                        val maxV = customMaxText.toIntOrNull() ?: preferences.customMax
                                        onCustomRangeChanged(minV, maxOf(minV, maxV))
                                    },
                                    label = { Text("От (0–9999)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = customMaxText,
                                    onValueChange = { raw ->
                                        val digits = raw.filter { it.isDigit() }.take(4)
                                        customMaxText = digits
                                        val minV = customMinText.toIntOrNull() ?: preferences.customMin
                                        val maxV = digits.toIntOrNull() ?: minV
                                        onCustomRangeChanged(minV, maxV)
                                    },
                                    label = { Text("До (0–9999)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Session Length & Main Action Buttons
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (selectedMode != TrainingMode.GUIDED_RULES) {
                        Text("Длина тренировки", style = MaterialTheme.typography.titleMedium)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SessionLength.entries.forEach { len ->
                                FilterChip(
                                    selected = selectedLength == len,
                                    onClick = { onSelectLength(len) },
                                    label = { Text(len.labelRu) }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onStartTraining,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_training_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (selectedMode == TrainingMode.GUIDED_RULES) "Открыть правила" else "Начать тренировку",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenMistakesReview,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_mistakes_button")
                        ) {
                            Icon(Icons.Filled.ErrorOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Работа над ошибками")
                        }
                        OutlinedButton(
                            onClick = onOpenStatistics,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("home_statistics_button")
                        ) {
                            Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Статистика")
                        }
                    }
                }
            }
        }
    }
}

private fun iconForMode(mode: TrainingMode): ImageVector = when (mode) {
    TrainingMode.DRAG_AND_DROP -> Icons.Filled.DragIndicator
    TrainingMode.WRITING -> Icons.Filled.Edit
    TrainingMode.ACTIVE_RECALL -> Icons.Filled.Psychology
    TrainingMode.WORD_ASSEMBLY -> Icons.Filled.Extension
    TrainingMode.LISTENING -> Icons.Filled.Hearing
    TrainingMode.PRONUNCIATION -> Icons.Filled.Mic
    TrainingMode.TELEPHONE -> Icons.Filled.Phone
    TrainingMode.CONTEXTUAL -> Icons.Filled.Public
    TrainingMode.SPEED_FLUENCY -> Icons.Filled.Bolt
    TrainingMode.GUIDED_RULES -> Icons.AutoMirrored.Filled.MenuBook
    else -> Icons.Filled.CheckCircle
}
