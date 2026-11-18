package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ThemePreference
import com.example.data.local.UserPreferences
import kotlin.math.roundToLong

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    ttsReady: Boolean,
    ttsVoicesCount: Int,
    ttsStatusMessage: String?,
    onSelectTheme: (ThemePreference) -> Unit,
    onToggleVoiceExercises: (Boolean) -> Unit,
    onToggleVaryVoices: (Boolean) -> Unit,
    onUpdateSpeedSettings: (Long, Boolean) -> Unit,
    onResetProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showResetConfirm by remember { mutableStateOf(false) }

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
                Text("Настройки", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Тема оформления, голосовые упражнения, синтез речи (fr-FR) и таймеры.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Theme selector
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Тема оформления", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = preferences.themePreference == pref,
                                onClick = { onSelectTheme(pref) },
                                label = { Text(pref.labelRu) },
                                modifier = Modifier.testTag("theme_chip_${pref.name}")
                            )
                        }
                    }
                }
            }
        }

        // Audio & Speech settings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Звук и речь (fr-FR)", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Голосовые задания", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Выключите, если сейчас неудобно говорить вслух",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.voiceExercisesEnabled,
                            onCheckedChange = onToggleVoiceExercises
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Чередовать голоса TTS", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                if (ttsReady) "Найдено голосов fr-FR: $ttsVoicesCount. Вариативность помогает привыкнуть к разному тембру."
                                else (ttsStatusMessage ?: "Проверка системного синтезатора речи..."),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.varyVoices,
                            onCheckedChange = onToggleVaryVoices
                        )
                    }
                    Text(
                        text = "Примечание: системное распознавание речи может обращаться к внешнему речевому сервису устройства. Аудиозаписи не сохраняются постоянно.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Speed & Fluency configuration
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Реакция и скорость (ориентиры)", style = MaterialTheme.typography.titleMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Режим без давления времени", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "Отключает таймеры в обычных тренировках",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = preferences.pressureFreeSpeed,
                            onCheckedChange = { onUpdateSpeedSettings(preferences.speedTargetMs, it) }
                        )
                    }
                    Text(
                        text = "Целевой порог узнавания: ${String.format("%.1f", preferences.speedTargetMs / 1000.0)} с",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Slider(
                        value = preferences.speedTargetMs.toFloat(),
                        onValueChange = { onUpdateSpeedSettings(it.roundToLong(), preferences.pressureFreeSpeed) },
                        valueRange = 1800f..6000f,
                        steps = 7
                    )
                    Text(
                        text = "Начальные интервалы повторения (1, 3, 7, 14, 30 дней) и пороги скорости являются тренировочными ориентирами и адаптируются по вашей точности.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Reset progress
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 680.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Данные и прогресс", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Весь прогресс хранится локально на устройстве без регистрации.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showResetConfirm = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("reset_progress_button")
                    ) {
                        Text("Сбросить весь прогресс")
                    }
                }
            }
        }
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Сбросить статистику?") },
            text = { Text("Будут удалены все записи о навыках, ошибках, расписание повторений и результаты диагностики.") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetProgress()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Сбросить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}
