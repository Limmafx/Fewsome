package com.example.domain

data class DiagnosticReport(
    val completedCount: Int,
    val recognitionAccuracy: Int?,
    val writingAccuracy: Int?,
    val listeningAccuracy: Int?,
    val averageLatencyMs: Long,
    val speakingAttemptedCount: Int,
    val speakingAsrMatchedCount: Int,
    val weakCategories: List<String>,
    val confusionPairs: List<Pair<Int, Int>>,
    val recommendedStage: PedagogicalStage,
    val recommendedDifficulty: DifficultyLevel,
    val planSummaryRu: String
)

/**
 * Stratified diagnostic sampler & analyzer ("Диагностика знаний").
 * Samples across:
 * 1. Basic numbers (0–19)
 * 2. Regular tens (20–69)
 * 3. Irregular constructions (70–99)
 * 4. Numbers with "et un" (21, 31, 41, 51, 61, 71)
 * 5. Hundreds & thousands (100–9999)
 *
 * Core proficiency excludes unverified ASR outcomes.
 */
class DiagnosticEngine(private val generator: ExerciseGenerator = ExerciseGenerator()) {

    fun buildStratifiedDiagnosticExercises(includeSpeaking: Boolean): List<Exercise> {
        val basicStratum = listOf(0, 8, 12, 16, 19).shuffled().take(4)
        val regularTensStratum = listOf(24, 38, 45, 57, 63).shuffled().take(4)
        val irregularStratum = listOf(70, 75, 78, 80, 84, 91, 97, 99).shuffled().take(6)
        val etUnStratum = listOf(21, 31, 41, 51, 61, 71).shuffled().take(4)
        val hundredsThousandsStratum = listOf(100, 180, 200, 201, 540, 1000, 1984, 2200).shuffled().take(4)

        val sampledNumbers = buildList {
            addAll(basicStratum)
            addAll(regularTensStratum)
            addAll(irregularStratum)
            addAll(etUnStratum)
            addAll(hundredsThousandsStratum)
        }.shuffled()

        val exercises = mutableListOf<Exercise>()
        sampledNumbers.forEachIndexed { idx, num ->
            val exercise = when (idx % 3) {
                0 -> generator.generateDragAndDrop(num, isAssessment = true)
                1 -> generator.generateWriting(num, isActiveRecall = true, isAssessment = true)
                else -> generator.generateListening(
                    num,
                    if (idx % 2 == 0) ListeningVariation.MULTIPLE_CHOICE_NUMBERS else ListeningVariation.TYPED_DIGITS,
                    isAssessment = true
                )
            }
            exercises.add(exercise)
        }

        if (includeSpeaking) {
            // 2 optional spoken assessment items — tracked separately, never mixed into core proficiency score
            exercises.add(generator.generatePronunciation(71, PronunciationActivity.SPEECH_RECALL))
            exercises.add(generator.generatePronunciation(96, PronunciationActivity.SPEECH_RECALL))
        }
        return exercises
    }

    fun evaluate(outcomes: List<SessionQuestionOutcome>): DiagnosticReport {
        val coreOutcomes = outcomes.filter { it.exercise.learningMode != LearningMode.SPEAKING }
        val recognition = coreOutcomes.filter { it.exercise.learningMode == LearningMode.RECOGNITION }
        val writing = coreOutcomes.filter { it.exercise.learningMode == LearningMode.WRITING }
        val listening = coreOutcomes.filter { it.exercise.learningMode == LearningMode.LISTENING }
        val speaking = outcomes.filter { it.exercise.learningMode == LearningMode.SPEAKING }

        val recAcc = accuracyOrNull(recognition)
        val writAcc = accuracyOrNull(writing)
        val listAcc = accuracyOrNull(listening)

        val avgLatency = if (coreOutcomes.isEmpty()) 0L else coreOutcomes.map { it.responseTimeMs }.average().toLong()

        // Aggregate category accuracy
        val categoryStats = mutableMapOf<String, Pair<Int, Int>>() // cat -> (correct, total)
        coreOutcomes.forEach { outcome ->
            val cats = FrenchNumberEngine.categoriesFor(outcome.exercise.targetNumber)
            cats.forEach { cat ->
                val prev = categoryStats[cat] ?: (0 to 0)
                categoryStats[cat] = (prev.first + if (outcome.isCorrect) 1 else 0) to (prev.second + 1)
            }
        }

        val weakCategories = categoryStats.entries
            .filter { (_, stats) -> stats.second > 0 && (stats.first.toDouble() / stats.second) < 0.75 }
            .sortedBy { it.value.first.toDouble() / it.value.second }
            .map { it.key }

        val confusions = coreOutcomes
            .mapNotNull { outcome ->
                val confused = outcome.mistakeAnalysis?.confusedWithNumber
                if (!outcome.isCorrect && confused != null) outcome.exercise.targetNumber to confused else null
            }
            .distinct()

        // Determine recommended stage from CORE accuracy ONLY (no ASR mixing)
        val coreAccValues = listOfNotNull(recAcc, writAcc, listAcc)
        val overallCoreAcc = if (coreAccValues.isEmpty()) 0 else coreAccValues.average().toInt()

        val recommendedStage = when {
            overallCoreAcc >= 90 && avgLatency in 1..3500 -> PedagogicalStage.STAGE_5_FLUENCY
            overallCoreAcc >= 80 && (listAcc ?: 0) >= 75 -> PedagogicalStage.STAGE_4_AUDIO_SPEECH
            overallCoreAcc >= 65 && (writAcc ?: 0) >= 60 -> PedagogicalStage.STAGE_3_PRODUCTION
            overallCoreAcc >= 45 -> PedagogicalStage.STAGE_2_RECOGNITION
            else -> PedagogicalStage.STAGE_1_GUIDED
        }

        val recommendedDifficulty = when {
            "0-19" in weakCategories -> DifficultyLevel.BEGINNER
            "20-69" in weakCategories -> DifficultyLevel.ELEMENTARY
            "70-99" in weakCategories || "et-un" in weakCategories -> DifficultyLevel.INTERMEDIATE
            "100-999" in weakCategories -> DifficultyLevel.ADVANCED
            "1000-9999" in weakCategories -> DifficultyLevel.EXPERT
            overallCoreAcc >= 85 -> DifficultyLevel.ADVANCED
            else -> DifficultyLevel.STANDARD
        }

        val planSummaryRu = buildPlanSummary(
            overallCoreAcc = overallCoreAcc,
            recAcc = recAcc,
            writAcc = writAcc,
            listAcc = listAcc,
            weakCategories = weakCategories,
            recommendedStage = recommendedStage
        )

        return DiagnosticReport(
            completedCount = outcomes.size,
            recognitionAccuracy = recAcc,
            writingAccuracy = writAcc,
            listeningAccuracy = listAcc,
            averageLatencyMs = avgLatency,
            speakingAttemptedCount = speaking.size,
            speakingAsrMatchedCount = speaking.count {
                it.isCorrect && it.speakingOutcomeType == SpeakingOutcomeType.ASR_TRANSCRIPTION
            },
            weakCategories = weakCategories,
            confusionPairs = confusions,
            recommendedStage = recommendedStage,
            recommendedDifficulty = recommendedDifficulty,
            planSummaryRu = planSummaryRu
        )
    }

    private fun accuracyOrNull(list: List<SessionQuestionOutcome>): Int? {
        if (list.isEmpty()) return null
        return ((list.count { it.isCorrect } * 100.0) / list.size).toInt()
    }

    private fun buildPlanSummary(
        overallCoreAcc: Int,
        recAcc: Int?,
        writAcc: Int?,
        listAcc: Int?,
        weakCategories: List<String>,
        recommendedStage: PedagogicalStage
    ): String {
        val lines = mutableListOf<String>()
        lines.add("Базовый результат диагностики: $overallCoreAcc% · ${recommendedStage.titleRu}.")
        if (weakCategories.isNotEmpty()) {
            val readable = weakCategories.joinToString(", ") { categoryLabelRu(it) }
            lines.add("Приоритетные зоны роста: $readable.")
        } else if (overallCoreAcc >= 80) {
            lines.add("Все базовые числовые диапазоны освоены уверенно — переходите к ускорению и сложным числам.")
        }
        if (recAcc != null && listAcc != null && recAcc - listAcc >= 20) {
            lines.add("Зрительное узнавание ($recAcc%) опережает восприятие на слух ($listAcc%) — добавьте «Понимание на слух».")
        }
        if (recAcc != null && writAcc != null && recAcc - writAcc >= 20) {
            lines.add("Правописание ($writAcc%) отстаёт от узнавания ($recAcc%) — практикуйте «Вспомни самостоятельно».")
        }
        return lines.joinToString("\n")
    }

    companion object {
        fun categoryLabelRu(code: String): String = when (code) {
            "0-19" -> "Числа 0–19"
            "20-69" -> "Десятки 20–69"
            "70-99" -> "Конструкции 70–99"
            "et-un" -> "Числа с «et un»"
            "100-999" -> "Сотни 100–999"
            "1000-9999" -> "Тысячи 1000–9999"
            "complex" -> "Сложные числа"
            else -> code
        }
    }
}
