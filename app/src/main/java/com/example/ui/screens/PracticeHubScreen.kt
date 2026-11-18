package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.domain.DashboardMetrics
import com.example.domain.TrainingMode

@Composable
fun PracticeHubScreen(
    metrics: DashboardMetrics,
    onLaunchMode: (TrainingMode) -> Unit,
    onOpenPhonetics: () -> Unit,
    onOpenGuidedRules: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Text("Целевая практика", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Прицельная работа с ошибками, фонетика, диагностика и независимый контроль знаний.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            PracticeActionCard(
                title = "Работа над ошибками",
                description = if (metrics.totalIncorrect > 0) {
                    "Прицельное повторение трудных чисел (${metrics.topConfusedNumbers.size} в фокусе)."
                } else {
                    "Ошибок пока нет — при появлении трудных чисел они автоматически попадут сюда."
                },
                badgeText = if (metrics.overdueReviewCount > 0) "Повтор: ${metrics.overdueReviewCount}" else null,
                icon = Icons.Filled.ErrorOutline,
                tag = "practice_mistakes_card",
                onClick = { onLaunchMode(TrainingMode.MISTAKES_REVIEW) }
            )
        }

        item {
            PracticeActionCard(
                title = "Похожие числа",
                description = "Контрастные серии (60/70, 71/81/91, 15/50) на основе ваших частых смешений с разбором формул.",
                icon = Icons.Filled.CompareArrows,
                tag = "practice_confusion_card",
                onClick = { onLaunchMode(TrainingMode.CONFUSION_DRILL) }
            )
        }

        item {
            PracticeActionCard(
                title = "Контроль знаний",
                description = "Независимый срез (16 заданий) без подсказок и повторов. Проверяет реальное удержание.",
                icon = Icons.Filled.FactCheck,
                tag = "practice_assessment_card",
                onClick = { onLaunchMode(TrainingMode.ASSESSMENT) }
            )
        }

        item {
            PracticeActionCard(
                title = "Фонетика и МФА (IPA)",
                description = "Носовые гласные /ɑ̃/, /ɛ̃/, связывание (liaison), немые согласные и ритм с аудио.",
                icon = Icons.Filled.RecordVoiceOver,
                tag = "practice_phonetics_card",
                onClick = onOpenPhonetics
            )
        }

        item {
            PracticeActionCard(
                title = "Этап 1 · Правила построения",
                description = "Разбор систем 20–69, союза «et», 70–79, 80–99, сотен и телефонных пар.",
                icon = Icons.Filled.MenuBook,
                tag = "practice_rules_card",
                onClick = onOpenGuidedRules
            )
        }

        item {
            PracticeActionCard(
                title = "Диагностика знаний",
                description = "Стратифицированный тест по всем диапазонам и модальностям для обновления учебного плана.",
                icon = Icons.Filled.Assessment,
                tag = "practice_diagnostic_card",
                onClick = { onLaunchMode(TrainingMode.DIAGNOSTIC) }
            )
        }
    }
}

@Composable
private fun PracticeActionCard(
    title: String,
    description: String,
    icon: ImageVector,
    tag: String,
    badgeText: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 680.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag(tag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    if (badgeText != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
