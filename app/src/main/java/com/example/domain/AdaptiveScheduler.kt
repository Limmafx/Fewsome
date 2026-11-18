package com.example.domain

import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Deterministic adaptive spaced-repetition scheduler & same-session retry queue.
 *
 * Heuristics (not presented as proven clinical optima):
 * - Initial long-term review sequence: 1, 3, 7, 14, 30 days.
 * - Same-session retry after 3..5 intervening questions on error (never immediate).
 * - Independent per-skill updates (recognition does NOT inflate writing/listening/speaking).
 */
class AdaptiveScheduler(
    val intervalSequenceDays: List<Int> = listOf(1, 3, 7, 14, 30)
) {

    data class PendingRetry(
        val number: Int,
        val learningMode: LearningMode,
        var questionsUntilDue: Int
    )

    private val retryQueue = mutableListOf<PendingRetry>()

    fun clearSessionQueue() {
        retryQueue.clear()
    }

    /**
     * Decrements counters for all queued same-session retries and returns one if due.
     */
    fun pollDueRetry(allowedMode: LearningMode? = null): PendingRetry? {
        retryQueue.forEach { it.questionsUntilDue = (it.questionsUntilDue - 1).coerceAtLeast(0) }
        val idx = retryQueue.indexOfFirst {
            it.questionsUntilDue == 0 && (allowedMode == null || it.learningMode == allowedMode)
        }
        return if (idx >= 0) retryQueue.removeAt(idx) else null
    }

    /**
     * Enqueues a missed number to reappear after 3..5 intervening questions.
     */
    fun scheduleSameSessionRetry(
        number: Int,
        learningMode: LearningMode,
        delayQuestions: Int = Random.nextInt(3, 6)
    ) {
        if (retryQueue.none { it.number == number && it.learningMode == learningMode }) {
            retryQueue.add(
                PendingRetry(
                    number = number,
                    learningMode = learningMode,
                    questionsUntilDue = delayQuestions.coerceIn(3, 5)
                )
            )
        }
    }

    /**
     * Updates ONLY the targeted `LearningMode` SkillRecord for `number`.
     * Other skills (e.g. Speaking when practicing Drag&Drop) remain untouched.
     */
    fun recordAttempt(
        existing: NumberProgress?,
        number: Int,
        mode: LearningMode,
        isCorrect: Boolean,
        responseTimeMs: Long?,
        nowMillis: Long,
        confusedWith: Int? = null,
        speakingOutcomeType: SpeakingOutcomeType? = null
    ): NumberProgress {
        val base = existing ?: NumberProgress(number = number)
        val currentSkill = base.skills[mode] ?: SkillRecord()

        val updatedTimes = if (responseTimeMs != null && responseTimeMs > 0L) {
            (currentSkill.recentResponseTimes + responseTimeMs).takeLast(12)
        } else {
            currentSkill.recentResponseTimes
        }
        val avgTime = if (updatedTimes.isNotEmpty()) {
            updatedTimes.average().roundToLong()
        } else {
            currentSkill.averageResponseMs
        }

        val newConsecutive = if (isCorrect) currentSkill.consecutiveCorrect + 1 else 0
        val newIntervalDays = if (isCorrect) {
            nextIntervalDays(currentSkill.reviewIntervalDays, newConsecutive)
        } else {
            0 // reset short interval for prompt review
        }

        // Next review timestamp: on error, due in 4 hours; on success, intervalDays * 24h
        val nextReviewAt = if (isCorrect) {
            nowMillis + newIntervalDays.toLong() * DAY_MILLIS
        } else {
            nowMillis + 4L * HOUR_MILLIS
        }

        val updatedSkill = currentSkill.copy(
            attempts = currentSkill.attempts + 1,
            correct = currentSkill.correct + if (isCorrect) 1 else 0,
            consecutiveCorrect = newConsecutive,
            lastPracticedAt = nowMillis,
            nextReviewAt = nextReviewAt,
            averageResponseMs = avgTime,
            recentResponseTimes = updatedTimes,
            reviewIntervalDays = newIntervalDays,
            selfReportedCorrect = currentSkill.selfReportedCorrect +
                if (isCorrect && speakingOutcomeType == SpeakingOutcomeType.SELF_REPORTED) 1 else 0,
            asrMatchedCount = currentSkill.asrMatchedCount +
                if (isCorrect && speakingOutcomeType == SpeakingOutcomeType.ASR_TRANSCRIPTION) 1 else 0
        )

        val updatedConfusions = if (!isCorrect && confusedWith != null && confusedWith != number) {
            val prevCount = base.confusionTargets[confusedWith] ?: 0
            base.confusionTargets + (confusedWith to (prevCount + 1))
        } else {
            base.confusionTargets
        }

        return base.copy(
            skills = base.skills + (mode to updatedSkill),
            confusionTargets = updatedConfusions
        )
    }

    private fun nextIntervalDays(currentDays: Int, consecutiveCorrect: Int): Int {
        if (consecutiveCorrect <= 1) return intervalSequenceDays.first()
        val idx = intervalSequenceDays.indexOfFirst { it > currentDays }
        return if (idx >= 0) intervalSequenceDays[idx] else intervalSequenceDays.last()
    }

    fun getOverdueNumbers(
        allProgress: Map<Int, NumberProgress>,
        mode: LearningMode? = null,
        nowMillis: Long
    ): List<Int> {
        return allProgress.values
            .filter { prog ->
                val records = if (mode != null) listOfNotNull(prog.skills[mode]) else prog.skills.values
                records.any { rec ->
                    rec.attempts > 0 && rec.nextReviewAt != null && rec.nextReviewAt <= nowMillis
                }
            }
            .sortedBy { prog ->
                val records = if (mode != null) listOfNotNull(prog.skills[mode]) else prog.skills.values
                records.mapNotNull { it.nextReviewAt }.minOrNull() ?: Long.MAX_VALUE
            }
            .map { it.number }
    }

    /**
     * Returns numbers sorted by highest weakness / error rate for "Работа над ошибками".
     */
    fun getFrequentlyMissedNumbers(allProgress: Map<Int, NumberProgress>): List<Int> {
        return allProgress.values
            .filter { it.totalIncorrect > 0 }
            .sortedWith(
                compareByDescending<NumberProgress> { it.totalIncorrect.toDouble() / it.totalAttempts.coerceAtLeast(1) }
                    .thenByDescending { it.totalIncorrect }
            )
            .map { it.number }
    }

    /**
     * Picks next number using adaptive priority:
     * 1. Due same-session retry (3..5 questions after miss).
     * 2. Overdue review item within range.
     * 3. Weak/missed item in range (weighted probability, reduced if mastered).
     * 4. Fresh sampled number from difficulty range.
     */
    fun selectNextNumber(
        difficulty: DifficultyLevel,
        customMin: Int,
        customMax: Int,
        learningMode: LearningMode,
        allProgress: Map<Int, NumberProgress>,
        generator: ExerciseGenerator,
        nowMillis: Long,
        lastNumber: Int? = null,
        mistakesOnly: Boolean = false
    ): Int {
        // 1. Same-session retry
        val dueRetry = pollDueRetry(learningMode)
        if (dueRetry != null && dueRetry.number != lastNumber) {
            return dueRetry.number
        }

        // 2. Mistakes-only mode ("Работа над ошибками")
        if (mistakesOnly) {
            val missed = getFrequentlyMissedNumbers(allProgress).filter { it != lastNumber }
            if (missed.isNotEmpty()) {
                return missed.take(8).random()
            }
        }

        val exclude = if (lastNumber != null) setOf(lastNumber) else emptySet()

        // 3. Overdue items matching difficulty
        val overdueInRange = getOverdueNumbers(allProgress, learningMode, nowMillis)
            .filter { it != lastNumber && isInDifficultyRange(it, difficulty, customMin, customMax) }
        if (overdueInRange.isNotEmpty() && Random.nextFloat() < 0.6f) {
            return overdueInRange.first()
        }

        // 4. Boost weak items; avoid repeating already mastered items (consecutiveCorrect >= 3)
        val weakCandidates = allProgress.values
            .filter { prog ->
                val rec = prog.skills[learningMode]
                rec != null && rec.attempts > 0 && rec.consecutiveCorrect < 2 &&
                    prog.number != lastNumber &&
                    isInDifficultyRange(prog.number, difficulty, customMin, customMax)
            }
            .map { it.number }
        if (weakCandidates.isNotEmpty() && Random.nextFloat() < 0.45f) {
            return weakCandidates.random()
        }

        // 5. Fresh number, skipping mastered if avoidable
        val mastered = allProgress.values
            .filter { (it.skills[learningMode]?.consecutiveCorrect ?: 0) >= 3 }
            .map { it.number }
            .toSet()

        return generator.pickNumber(difficulty, customMin, customMax, exclude = exclude + mastered)
    }

    private fun isInDifficultyRange(
        number: Int,
        difficulty: DifficultyLevel,
        customMin: Int,
        customMax: Int
    ): Boolean {
        return when (difficulty) {
            DifficultyLevel.COMPLEX -> FrenchNumberEngine.isComplexNumber(number)
            DifficultyLevel.CUSTOM -> number in customMin.coerceIn(0, 9999)..customMax.coerceIn(0, 9999)
            else -> number in difficulty.minVal..difficulty.maxVal
        }
    }

    companion object {
        const val HOUR_MILLIS = 3_600_000L
        const val DAY_MILLIS = 86_400_000L
    }
}
