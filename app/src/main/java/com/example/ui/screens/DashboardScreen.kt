package com.example.ui.screens

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DiagnosticSnapshotEntity
import com.example.data.local.UserPreferences
import com.example.domain.DashboardMetrics
import com.example.domain.LearningMode
import com.example.ui.theme.LocalSemanticColors

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    preferences: UserPreferences,
    latestDiagnostic: DiagnosticSnapshotEntity?,
    onStartConfusionDrill: () -> Unit,
    onStartAssessment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantics = LocalSemanticColors.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Text("Панель мастерства", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Независимый учёт навыков и диапазонов без усреднения устных оценок.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (metrics.totalAttempts == 0) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Insights,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("Пока нет данных тренировок", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Пройдите диагностику или любую серию упражнений, чтобы увидеть карту сильных и слабых чисел.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            // Core KPIs (Estimated practice vs Verified assessment)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Сводка показателей", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiBox(
                                label = "Всего ответов",
                                value = "${metrics.totalAttempts}",
                                sub = "✓ ${metrics.totalCorrect} · ✗ ${metrics.totalIncorrect}",
                                modifier = Modifier.weight(1f)
                            )
                            KpiBox(
                                label = "Точность (практика)",
                                value = metrics.overallAccuracyPercent?.let { "$it%" } ?: "—",
                                sub = "Лучшая серия: ${preferences.bestStreak}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            KpiBox(
                                label = "Контроль (провер.)",
                                value = metrics.verifiedAssessmentAccuracyPercent?.let { "$it%" } ?: "Не пройден",
                                sub = "Независимый срез",
                                modifier = Modifier.weight(1f)
                            )
                            KpiBox(
                                label = "Среднее время",
                                value = metrics.averageResponseMs?.let { String.format("%.1f с", it / 1000.0) } ?: "—",
                                sub = "К повтору: ${metrics.overdueReviewCount}",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Practical Recommendations
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text("Практические рекомендации", style = MaterialTheme.typography.titleMedium)
                        }
                        metrics.recommendationsRu.forEach { rec ->
                            Text("• $rec", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        OutlinedButton(onClick = onStartAssessment) {
                            Text("Пройти «Контроль знаний»")
                        }
                    }
                }
            }

            // Skill Mastery
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Навыки по отдельности", style = MaterialTheme.typography.titleMedium)
                        metrics.skillSummaries.forEach { skill ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(skill.mode.titleRu, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (skill.attempts == 0) "Нет попыток" else {
                                            "${skill.accuracyPercent}% (${skill.attempts} попыт.)"
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (skill.attempts > 0) {
                                    LinearProgressIndicator(
                                        progress = { (skill.accuracyPercent ?: 0) / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp),
                                        color = if ((skill.accuracyPercent ?: 0) >= 80) semantics.success else MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (skill.mode == LearningMode.SPEAKING && skill.attempts > 0) {
                                    Text(
                                        text = "Самооценка: ${skill.selfReportedCount} · Совпадения ASR (не оценка фонетики): ${skill.asrMatchedCount}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Range Mastery
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Карта числовых диапазонов", style = MaterialTheme.typography.titleMedium)
                        metrics.rangeSummaries.forEach { range ->
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(range.labelRu, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(
                                        text = range.accuracyPercent?.let { "$it% (${range.attempts})" } ?: "—",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (range.attempts > 0) {
                                    LinearProgressIndicator(
                                        progress = { (range.accuracyPercent ?: 0) / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                    )
                                    val breakdown = buildList {
                                        range.recognitionAccuracyPercent?.let { add("Узн: $it%") }
                                        range.writingAccuracyPercent?.let { add("Письмо: $it%") }
                                        range.listeningAccuracyPercent?.let { add("Слух: $it%") }
                                    }.joinToString(" · ")
                                    if (breakdown.isNotBlank()) {
                                        Text(
                                            text = breakdown,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Accuracy Trend Chart
            if (metrics.recentTrends.size >= 2) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Динамика точности по сессиям", style = MaterialTheme.typography.titleMedium)
                            val lineColor = MaterialTheme.colorScheme.primary
                            val dotColor = MaterialTheme.colorScheme.secondary
                            val points = metrics.recentTrends
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .padding(vertical = 8.dp)
                            ) {
                                val stepX = size.width / (points.size - 1).coerceAtLeast(1)
                                val path = Path()
                                points.forEachIndexed { idx, p ->
                                    val x = idx * stepX
                                    val y = size.height - (p.accuracyPercent.coerceIn(0, 100) / 100f) * size.height
                                    if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path, color = lineColor, style = Stroke(width = 5f, cap = StrokeCap.Round))
                                points.forEachIndexed { idx, p ->
                                    val x = idx * stepX
                                    val y = size.height - (p.accuracyPercent.coerceIn(0, 100) / 100f) * size.height
                                    drawCircle(
                                        color = if (p.isAssessment) dotColor else lineColor,
                                        radius = if (p.isAssessment) 8f else 6f,
                                        center = Offset(x, y)
                                    )
                                }
                            }
                            Text(
                                text = "Последняя сессия: ${points.last().accuracyPercent}% · ср. время ${String.format("%.1f с", points.last().averageResponseMs / 1000.0)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Frequently Confused Numbers
            if (metrics.topConfusedNumbers.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 680.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text("Числа с частыми ошибками", style = MaterialTheme.typography.titleMedium)
                            }
                            metrics.topConfusedNumbers.forEach { stat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("${stat.number} — ${stat.french}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                        if (stat.topConfusedWith.isNotEmpty()) {
                                            Text(
                                                "Путается с: ${stat.topConfusedWith.joinToString(", ")}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Surface(
                                        color = MaterialTheme.colorScheme.errorContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = "Ошибок: ${stat.totalMistakes}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                            Button(
                                onClick = onStartConfusionDrill,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_confusion_drill_button")
                            ) {
                                Text("Отработать «Похожие числа»")
                            }
                        }
                    }
                }
            }
        }

        if (latestDiagnostic != null) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 680.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Последняя диагностика (${latestDiagnostic.completedCount} заданий)", style = MaterialTheme.typography.titleMedium)
                        Text(latestDiagnostic.planSummaryRu, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiBox(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(2.dp))
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
