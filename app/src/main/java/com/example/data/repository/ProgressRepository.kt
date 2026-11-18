package com.example.data.repository

import com.example.data.local.DiagnosticSnapshotDao
import com.example.data.local.DiagnosticSnapshotEntity
import com.example.data.local.NumberProgressDao
import com.example.data.local.NumberProgressEntity
import com.example.data.local.SessionHistoryDao
import com.example.data.local.SessionHistoryEntity
import com.example.domain.AdaptiveScheduler
import com.example.domain.DiagnosticReport
import com.example.domain.LearningMode
import com.example.domain.NumberProgress
import com.example.domain.SessionSummary
import com.example.domain.SessionTrendPoint
import com.example.domain.SpeakingOutcomeType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProgressRepository(
    private val progressDao: NumberProgressDao,
    private val sessionDao: SessionHistoryDao,
    private val diagnosticDao: DiagnosticSnapshotDao,
    val scheduler: AdaptiveScheduler = AdaptiveScheduler()
) {

    val progressMapFlow: Flow<Map<Int, NumberProgress>> =
        progressDao.observeAll().map { entities ->
            entities.associate { it.number to it.toDomain() }
        }

    val sessionTrendsFlow: Flow<List<SessionTrendPoint>> =
        sessionDao.observeAll().map { list ->
            list.map {
                SessionTrendPoint(
                    timestamp = it.timestamp,
                    accuracyPercent = it.accuracyPercent,
                    averageResponseMs = it.averageResponseMs,
                    isAssessment = it.isAssessment
                )
            }
        }

    val latestDiagnosticFlow: Flow<DiagnosticSnapshotEntity?> = diagnosticDao.observeLatest()

    suspend fun recordExerciseAttempt(
        number: Int,
        mode: LearningMode,
        isCorrect: Boolean,
        responseTimeMs: Long?,
        confusedWith: Int? = null,
        speakingOutcomeType: SpeakingOutcomeType? = null,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        val existing = progressDao.getByNumber(number)?.toDomain()
        val updated = scheduler.recordAttempt(
            existing = existing,
            number = number,
            mode = mode,
            isCorrect = isCorrect,
            responseTimeMs = responseTimeMs,
            nowMillis = nowMillis,
            confusedWith = confusedWith,
            speakingOutcomeType = speakingOutcomeType
        )
        progressDao.upsert(NumberProgressEntity.fromDomain(updated, nowMillis))
    }

    suspend fun saveCompletedSession(
        summary: SessionSummary,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (summary.totalAnswered <= 0) return
        val missedCsv = summary.difficultItems
            .map { it.exercise.targetNumber }
            .distinct()
            .joinToString(",")
        sessionDao.insert(
            SessionHistoryEntity(
                timestamp = nowMillis,
                modeName = summary.mode.name,
                totalAnswered = summary.totalAnswered,
                correctCount = summary.correctCount,
                incorrectCount = summary.incorrectCount,
                accuracyPercent = summary.accuracyPercent,
                averageResponseMs = summary.averageResponseMs,
                isAssessment = summary.isAssessment,
                missedNumbersCsv = missedCsv
            )
        )
    }

    suspend fun saveDiagnosticReport(
        report: DiagnosticReport,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        diagnosticDao.insert(
            DiagnosticSnapshotEntity(
                timestamp = nowMillis,
                completedCount = report.completedCount,
                recognitionAccuracy = report.recognitionAccuracy,
                writingAccuracy = report.writingAccuracy,
                listeningAccuracy = report.listeningAccuracy,
                averageLatencyMs = report.averageLatencyMs,
                recommendedStageNumber = report.recommendedStage.stageNumber,
                recommendedDifficultyName = report.recommendedDifficulty.name,
                weakCategoriesCsv = report.weakCategories.joinToString(","),
                planSummaryRu = report.planSummaryRu
            )
        )
    }

    suspend fun getAllProgressSnapshot(): Map<Int, NumberProgress> {
        return progressDao.getAllOnce().associate { it.number to it.toDomain() }
    }

    suspend fun clearAllProgress() {
        progressDao.deleteAll()
        sessionDao.deleteAll()
        diagnosticDao.deleteAll()
        scheduler.clearSessionQueue()
    }
}
