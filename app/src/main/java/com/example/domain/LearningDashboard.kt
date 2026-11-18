package com.example.domain

data class SkillMasterySummary(
    val mode: LearningMode,
    val attempts: Int,
    val accuracyPercent: Int?,
    val averageResponseMs: Long?,
    val masteredCount: Int,
    val selfReportedCount: Int = 0,
    val asrMatchedCount: Int = 0
)

data class RangeMasterySummary(
    val categoryCode: String,
    val labelRu: String,
    val attempts: Int,
    val accuracyPercent: Int?,
    val recognitionAccuracyPercent: Int?,
    val listeningAccuracyPercent: Int?,
    val writingAccuracyPercent: Int?
)

data class SessionTrendPoint(
    val timestamp: Long,
    val accuracyPercent: Int,
    val averageResponseMs: Long,
    val isAssessment: Boolean
)

data class ConfusedNumberStat(
    val number: Int,
    val french: String,
    val totalMistakes: Int,
    val topConfusedWith: List<Int>
)

data class DashboardMetrics(
    val totalAttempts: Int,
    val totalCorrect: Int,
    val totalIncorrect: Int,
    val overallAccuracyPercent: Int?,
    val verifiedAssessmentAccuracyPercent: Int?,
    val averageResponseMs: Long?,
    val overdueReviewCount: Int,
    val skillSummaries: List<SkillMasterySummary>,
    val rangeSummaries: List<RangeMasterySummary>,
    val topConfusedNumbers: List<ConfusedNumberStat>,
    val recentTrends: List<SessionTrendPoint>,
    val recommendationsRu: List<String>
)

/**
 * Computes transparent, evidence-based mastery analytics without arbitrary numbers.
 * Clearly distinguishes practice estimates from verified assessment results and
 * self-reported/ASR speaking records.
 */
object LearningDashboard {

    private val orderedCategories = listOf("0-19", "20-69", "70-99", "et-un", "100-999", "1000-9999")

    fun compute(
        progressMap: Map<Int, NumberProgress>,
        sessions: List<SessionTrendPoint>,
        scheduler: AdaptiveScheduler,
        nowMillis: Long
    ): DashboardMetrics {
        val allProgress = progressMap.values.toList()
        val totalAttempts = allProgress.sumOf { it.totalAttempts }
        val totalCorrect = allProgress.sumOf { it.totalCorrect }
        val totalIncorrect = totalAttempts - totalCorrect
        val overallAcc = if (totalAttempts == 0) null else ((totalCorrect * 100.0) / totalAttempts).toInt()

        val assessmentSessions = sessions.filter { it.isAssessment }
        val verifiedAcc = if (assessmentSessions.isEmpty()) null else {
            assessmentSessions.map { it.accuracyPercent }.average().toInt()
        }

        val allTimes = allProgress.flatMap { prog ->
            prog.skills.values.flatMap { it.recentResponseTimes }
        }
        val avgResponseMs = if (allTimes.isEmpty()) null else allTimes.average().toLong()

        val overdueCount = scheduler.getOverdueNumbers(progressMap, mode = null, nowMillis = nowMillis).size

        val skillSummaries = LearningMode.entries.map { mode ->
            val records = allProgress.mapNotNull { it.skills[mode] }.filter { it.attempts > 0 }
            val att = records.sumOf { it.attempts }
            val corr = records.sumOf { it.correct }
            val times = records.flatMap { it.recentResponseTimes }
            SkillMasterySummary(
                mode = mode,
                attempts = att,
                accuracyPercent = if (att == 0) null else ((corr * 100.0) / att).toInt(),
                averageResponseMs = if (times.isEmpty()) null else times.average().toLong(),
                masteredCount = records.count { it.consecutiveCorrect >= 3 },
                selfReportedCount = records.sumOf { it.selfReportedCorrect },
                asrMatchedCount = records.sumOf { it.asrMatchedCount }
            )
        }

        val rangeSummaries = orderedCategories.map { code ->
            val matching = allProgress.filter { code in it.categories }
            fun accFor(filterMode: LearningMode?): Pair<Int, Int?> {
                val recs = matching.flatMap { prog ->
                    if (filterMode == null) prog.skills.values else listOfNotNull(prog.skills[filterMode])
                }
                val a = recs.sumOf { it.attempts }
                val c = recs.sumOf { it.correct }
                return a to if (a == 0) null else ((c * 100.0) / a).toInt()
            }
            val (totA, totAcc) = accFor(null)
            val (_, recAcc) = accFor(LearningMode.RECOGNITION)
            val (_, listAcc) = accFor(LearningMode.LISTENING)
            val (_, writAcc) = accFor(LearningMode.WRITING)
            RangeMasterySummary(
                categoryCode = code,
                labelRu = DiagnosticEngine.categoryLabelRu(code),
                attempts = totA,
                accuracyPercent = totAcc,
                recognitionAccuracyPercent = recAcc,
                listeningAccuracyPercent = listAcc,
                writingAccuracyPercent = writAcc
            )
        }

        val confused = allProgress
            .filter { it.totalIncorrect > 0 }
            .sortedByDescending { it.totalIncorrect }
            .take(8)
            .map { prog ->
                ConfusedNumberStat(
                    number = prog.number,
                    french = FrenchNumberEngine.toFrench(prog.number),
                    totalMistakes = prog.totalIncorrect,
                    topConfusedWith = prog.confusionTargets.entries
                        .sortedByDescending { it.value }
                        .take(3)
                        .map { it.key }
                )
            }

        val recommendations = buildRecommendations(
            skillSummaries = skillSummaries,
            rangeSummaries = rangeSummaries,
            overdueCount = overdueCount,
            confused = confused
        )

        return DashboardMetrics(
            totalAttempts = totalAttempts,
            totalCorrect = totalCorrect,
            totalIncorrect = totalIncorrect,
            overallAccuracyPercent = overallAcc,
            verifiedAssessmentAccuracyPercent = verifiedAcc,
            averageResponseMs = avgResponseMs,
            overdueReviewCount = overdueCount,
            skillSummaries = skillSummaries,
            rangeSummaries = rangeSummaries,
            topConfusedNumbers = confused,
            recentTrends = sessions.takeLast(12),
            recommendationsRu = recommendations
        )
    }

    private fun buildRecommendations(
        skillSummaries: List<SkillMasterySummary>,
        rangeSummaries: List<RangeMasterySummary>,
        overdueCount: Int,
        confused: List<ConfusedNumberStat>
    ): List<String> {
        val recs = mutableListOf<String>()

        if (overdueCount > 0) {
            recs.add("К повторению готовы $overdueCount чисел по расписанию интервального повторения.")
        }

        rangeSummaries.forEach { range ->
            val recAcc = range.recognitionAccuracyPercent
            val listAcc = range.listeningAccuracyPercent
            val writAcc = range.writingAccuracyPercent
            if (recAcc != null && listAcc != null && recAcc >= 75 && listAcc < 65) {
                recs.add("${range.labelRu}: высокая точность при чтении ($recAcc%), но низкая на слух ($listAcc%). Рекомендуется «Понимание на слух».")
            } else if (recAcc != null && writAcc != null && recAcc >= 75 && writAcc < 65) {
                recs.add("${range.labelRu}: узнавание $recAcc%, но письмо $writAcc%. Потренируйте «Вспомни самостоятельно».")
            } else if (range.accuracyPercent != null && range.accuracyPercent < 65 && range.attempts >= 4) {
                recs.add("${range.labelRu}: точность ${range.accuracyPercent}%. Повторите правила и сделайте серию с карточками.")
            }
        }

        val listening = skillSummaries.firstOrNull { it.mode == LearningMode.LISTENING }
        if (listening != null && listening.attempts == 0 && skillSummaries.any { it.attempts > 0 }) {
            recs.add("Вы ещё не проверяли восприятие на слух — попробуйте «Понимание на слух».")
        }

        if (confused.isNotEmpty()) {
            val top = confused.first()
            recs.add("Частая ошибка: число ${top.number} (${top.french}). Запустите контрастный режим «Похожие числа».")
        }

        if (recs.isEmpty()) {
            recs.add("Начните с диагностики знаний или выберите любой режим тренировки для сбора статистики.")
        }
        return recs.take(4)
    }
}
