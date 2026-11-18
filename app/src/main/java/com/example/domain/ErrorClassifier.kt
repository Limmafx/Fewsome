package com.example.domain

/**
 * Deterministic pedagogical error classifier. Identifies structural confusion
 * (e.g., 71 vs 91, 16 vs 60, missing -s on quatre-vingts/cents, missing et,
 * omitted leading zero) and produces actionable Russian explanations.
 */
object ErrorClassifier {

    fun classifyWrittenError(
        userInput: String,
        expectedNumber: Int,
        validation: ValidationResult
    ): MistakeAnalysis {
        val decomposition = FrenchNumberEngine.decompose(expectedNumber)
        val norm = validation.normalizedInput
        if (norm.isBlank()) {
            return MistakeAnalysis(
                category = MistakeCategory.UNANSWERED,
                explanationRu = "Ответ не введён. Запомните структуру: $decomposition.",
                decompositionRu = decomposition
            )
        }

        val canonicalNorm = AnswerNormalizer.normalizeFrench(validation.canonicalExpected)

        // 1. Check plural -s errors on 80 / x80 and x00
        val lastTwo = expectedNumber % 100
        if (lastTwo == 80 && norm == canonicalNorm.removeSuffix("s")) {
            return MistakeAnalysis(
                category = MistakeCategory.PLURAL_AGREEMENT,
                explanationRu = "В $expectedNumber слово «vingts» завершает число, поэтому пишется с окончанием -s: «${validation.canonicalExpected}».",
                decompositionRu = decomposition
            )
        }
        if (expectedNumber >= 200 && lastTwo == 0 && expectedNumber % 1000 != 0 && norm == canonicalNorm.removeSuffix("s")) {
            return MistakeAnalysis(
                category = MistakeCategory.PLURAL_AGREEMENT,
                explanationRu = "В круглых сотнях ($expectedNumber) без следующих цифр пишется «cents» с -s: «${validation.canonicalExpected}».",
                decompositionRu = decomposition
            )
        }
        if (lastTwo in 81..99 && norm.contains("vingts")) {
            return MistakeAnalysis(
                category = MistakeCategory.PLURAL_AGREEMENT,
                explanationRu = "Когда после 80 идут другие цифры ($expectedNumber), «vingt» теряет -s: «${validation.canonicalExpected}».",
                decompositionRu = decomposition
            )
        }
        if (expectedNumber > 200 && lastTwo != 0 && norm.contains("cents")) {
            return MistakeAnalysis(
                category = MistakeCategory.PLURAL_AGREEMENT,
                explanationRu = "Когда после сотен идут десятки или единицы ($expectedNumber), «cent» пишется без -s: «${validation.canonicalExpected}».",
                decompositionRu = decomposition
            )
        }

        // 2. Check if the learner typed a DIFFERENT valid French number (confusion pair!)
        val other = validation.recognizedNumericMatch
        if (other != null && other != expectedNumber) {
            return classifyNumericConfusion(expectedNumber, other)
        }

        // 3. Check "et" errors (missing et in 21..71, or extra et in 81/91)
        if (lastTwo in listOf(21, 31, 41, 51, 61, 71) && !norm.contains(" et ")) {
            return MistakeAnalysis(
                category = MistakeCategory.ET_UN_RULE,
                explanationRu = "В числах с 1 (и 71) до 79 используется союз «et»: «${validation.canonicalExpected}».",
                decompositionRu = decomposition
            )
        }
        if (lastTwo in listOf(81, 91) && norm.contains(" et ")) {
            return MistakeAnalysis(
                category = MistakeCategory.ET_UN_RULE,
                explanationRu = "После «quatre-vingt» союз «et» НЕ ставится: правильно «${validation.canonicalExpected}».",
                decompositionRu = decomposition
            )
        }

        return MistakeAnalysis(
            category = MistakeCategory.SPELLING_OR_ORDER,
            explanationRu = "Ожидалось «${validation.canonicalExpected}». Обратите внимание на точное написание и структуру: $decomposition.",
            decompositionRu = decomposition
        )
    }

    fun classifyNumericConfusion(expectedNumber: Int, chosenNumber: Int): MistakeAnalysis {
        val expDecomp = FrenchNumberEngine.decompose(expectedNumber)
        val chosenDecomp = FrenchNumberEngine.decompose(chosenNumber)
        val expFr = FrenchNumberEngine.toFrench(expectedNumber)
        val chosenFr = FrenchNumberEngine.toFrench(chosenNumber)

        val expLastTwo = expectedNumber % 100
        val chLastTwo = chosenNumber % 100

        // Teen vs Ten confusion (13..19 vs 30..90)
        val teenTenPairs = setOf(
            setOf(13, 30), setOf(14, 40), setOf(15, 50), setOf(16, 60),
            setOf(17, 70), setOf(18, 80), setOf(19, 90)
        )
        if (setOf(expLastTwo, chLastTwo) in teenTenPairs) {
            return MistakeAnalysis(
                category = MistakeCategory.TEEN_TEN_CONFUSION,
                explanationRu = "Частая путаница $expectedNumber ($expFr) и $chosenNumber ($chosenFr). Сравните: $expDecomp vs $chosenDecomp.",
                decompositionRu = "$expectedNumber = $expDecomp\n$chosenNumber = $chosenDecomp",
                confusedWithNumber = chosenNumber
            )
        }

        // Vigesimal / base-60 / base-80 confusion (60 vs 70 vs 80 vs 90, 71 vs 91, etc.)
        if (expLastTwo >= 60 || chLastTwo >= 60) {
            return MistakeAnalysis(
                category = MistakeCategory.VIGESIMAL_CONFUSION,
                explanationRu = "Путаница систем счёта 60+ и 4×20+: вы выбрали $chosenNumber ($chosenFr), а нужно $expectedNumber ($expFr).",
                decompositionRu = "$expectedNumber = $expDecomp\n$chosenNumber = $chosenDecomp",
                confusedWithNumber = chosenNumber
            )
        }

        return MistakeAnalysis(
            category = MistakeCategory.LISTENING_DISCRIMINATION,
            explanationRu = "Вы указали $chosenNumber («$chosenFr») вместо $expectedNumber («$expFr»).",
            decompositionRu = "$expectedNumber = $expDecomp",
            confusedWithNumber = chosenNumber
        )
    }

    fun classifyPhoneError(userInput: String, expectedPairs: List<String>): MistakeAnalysis {
        val norm = AnswerNormalizer.normalizeFrench(userInput)
        val canonical = FrenchNumberEngine.phonePairsToFrench(expectedPairs)
        val hasLeadingZeroPair = expectedPairs.any { it.startsWith("0") }
        val userDigits = userInput.filter { it.isDigit() }
        val expectedDigits = expectedPairs.joinToString("")

        if (hasLeadingZeroPair && (!norm.contains("zero") && userDigits.length < expectedDigits.length)) {
            return MistakeAnalysis(
                category = MistakeCategory.LEADING_ZERO_OMITTED,
                explanationRu = "Ведущий ноль в телефонной паре обязателен! Например, «06» читается как «zéro six», а не «six».",
                decompositionRu = expectedPairs.joinToString(" · ") { "$it = ${FrenchNumberEngine.pairToFrench(it)}" }
            )
        }
        return MistakeAnalysis(
            category = MistakeCategory.SPELLING_OR_ORDER,
            explanationRu = "Правильное чтение по парам: $canonical.",
            decompositionRu = expectedPairs.joinToString(" · ") { "$it = ${FrenchNumberEngine.pairToFrench(it)}" }
        )
    }
}
