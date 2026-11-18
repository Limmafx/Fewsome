package com.example

import com.example.domain.AdaptiveScheduler
import com.example.domain.AnswerNormalizer
import com.example.domain.DiagnosticEngine
import com.example.domain.DifficultyLevel
import com.example.domain.ErrorClassifier
import com.example.domain.ExerciseGenerator
import com.example.domain.FrenchNumberEngine
import com.example.domain.LearningMode
import com.example.domain.MistakeCategory
import com.example.domain.TelephoneSubMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FrenchNumberEngineTest {

    @Test
    fun `all integers 0 to 9999 convert uniquely and round-trip deterministically`() {
        val seenNormalized = mutableSetOf<String>()
        for (n in 0..9999) {
            val fr = FrenchNumberEngine.toFrench(n)
            assertTrue("Empty French for $n", fr.isNotBlank())
            val norm = AnswerNormalizer.normalizeFrench(fr)
            assertTrue("Duplicate normalized French '$norm' at $n", seenNormalized.add(norm))
            assertEquals(n, AnswerNormalizer.findMatchingNumber(norm))
        }
        assertEquals(10000, seenNormalized.size)
    }

    @Test
    fun `critical French number boundaries match metropolitan rules`() {
        assertEquals("zéro", FrenchNumberEngine.toFrench(0))
        assertEquals("dix-neuf", FrenchNumberEngine.toFrench(19))
        assertEquals("vingt", FrenchNumberEngine.toFrench(20))
        assertEquals("vingt et un", FrenchNumberEngine.toFrench(21))
        assertEquals("soixante-neuf", FrenchNumberEngine.toFrench(69))
        assertEquals("soixante-dix", FrenchNumberEngine.toFrench(70))
        assertEquals("soixante et onze", FrenchNumberEngine.toFrench(71))
        assertEquals("soixante-douze", FrenchNumberEngine.toFrench(72))
        assertEquals("soixante-dix-neuf", FrenchNumberEngine.toFrench(79))
        assertEquals("quatre-vingts", FrenchNumberEngine.toFrench(80))
        assertEquals("quatre-vingt-un", FrenchNumberEngine.toFrench(81))
        assertEquals("quatre-vingt-neuf", FrenchNumberEngine.toFrench(89))
        assertEquals("quatre-vingt-dix", FrenchNumberEngine.toFrench(90))
        assertEquals("quatre-vingt-onze", FrenchNumberEngine.toFrench(91))
        assertEquals("quatre-vingt-dix-neuf", FrenchNumberEngine.toFrench(99))
        assertEquals("cent", FrenchNumberEngine.toFrench(100))
        assertEquals("cent un", FrenchNumberEngine.toFrench(101))
        assertEquals("cent quatre-vingts", FrenchNumberEngine.toFrench(180))
        assertEquals("cent quatre-vingt-dix-neuf", FrenchNumberEngine.toFrench(199))
        assertEquals("deux cents", FrenchNumberEngine.toFrench(200))
        assertEquals("deux cent un", FrenchNumberEngine.toFrench(201))
        assertEquals("neuf cent quatre-vingt-dix-neuf", FrenchNumberEngine.toFrench(999))
        assertEquals("mille", FrenchNumberEngine.toFrench(1000))
        assertEquals("mille un", FrenchNumberEngine.toFrench(1001))
        assertEquals("deux mille deux cents", FrenchNumberEngine.toFrench(2200))
        assertEquals("deux mille deux cent un", FrenchNumberEngine.toFrench(2201))
    }

    @Test
    fun `accent-insensitive and hyphen-space normalization works while rejecting errors`() {
        assertEquals("zero", AnswerNormalizer.normalizeFrench("Zéro"))
        assertEquals("aout", AnswerNormalizer.normalizeFrench("AOÛT"))
        assertTrue(AnswerNormalizer.validateNumberAnswer("zero", 0).isCorrect)
        assertTrue(AnswerNormalizer.validateNumberAnswer("quatre   vingt onze", 91).isCorrect)
        assertTrue(AnswerNormalizer.validateNumberAnswer("vingt-et-un", 21).isCorrect)
        assertTrue(AnswerNormalizer.validateNumberAnswer("vingt et un", 21).isCorrect)
        assertTrue(AnswerNormalizer.validateNumberAnswer("SOIXANTE ET ONZE", 71).isCorrect)

        // Incorrect spelling, missing words, wrong plural -s, or wrong et are rejected
        assertFalse(AnswerNormalizer.validateNumberAnswer("katre vingt onze", 91).isCorrect)
        assertFalse(AnswerNormalizer.validateNumberAnswer("quatre vingt et onze", 91).isCorrect)
        assertFalse(AnswerNormalizer.validateNumberAnswer("quatre vingt", 80).isCorrect)
        assertFalse(AnswerNormalizer.validateNumberAnswer("deux cent", 200).isCorrect)
        assertFalse(AnswerNormalizer.validateNumberAnswer("deux cents un", 201).isCorrect)
        assertFalse(AnswerNormalizer.validateNumberAnswer("soixante onze", 71).isCorrect)
        assertFalse(AnswerNormalizer.validateNumberAnswer("", 10).isCorrect)
    }

    @Test
    fun `error classifier diagnoses plural -s, et-un, and confusion pairs`() {
        val plural80 = AnswerNormalizer.validateNumberAnswer("quatre-vingt", 80)
        assertFalse(plural80.isCorrect)
        assertEquals(MistakeCategory.PLURAL_AGREEMENT, ErrorClassifier.classifyWrittenError("quatre-vingt", 80, plural80).category)

        val confusion71vs91 = AnswerNormalizer.validateNumberAnswer("soixante et onze", 91)
        assertFalse(confusion71vs91.isCorrect)
        val analysis = ErrorClassifier.classifyWrittenError("soixante et onze", 91, confusion71vs91)
        assertEquals(MistakeCategory.VIGESIMAL_CONFUSION, analysis.category)
        assertEquals(71, analysis.confusedWithNumber)

        val unanswered = AnswerNormalizer.validateNumberAnswer("   ", 42)
        assertEquals(MistakeCategory.UNANSWERED, ErrorClassifier.classifyWrittenError("   ", 42, unanswered).category)
    }

    @Test
    fun `telephone numbers preserve leading zeros in both words and digits`() {
        assertEquals("zéro six", FrenchNumberEngine.pairToFrench("06"))
        assertEquals("zéro neuf", FrenchNumberEngine.pairToFrench("09"))
        assertEquals(
            "zéro six, douze, trente-quatre, cinquante-six, soixante-dix-huit",
            FrenchNumberEngine.phonePairsToFrench(listOf("06", "12", "34", "56", "78"))
        )
        assertFalse(AnswerNormalizer.validatePhonePairAnswer("six", "06").isCorrect)
        assertTrue(AnswerNormalizer.validatePhonePairAnswer("zero six", "06").isCorrect)
        assertTrue(AnswerNormalizer.validatePhoneDigits("06 12 34 56 78", listOf("06", "12", "34", "56", "78")))
        assertFalse(AnswerNormalizer.validatePhoneDigits("6 12 34 56 78", listOf("06", "12", "34", "56", "78")))
    }

    @Test
    fun `generated drag and drop questions always have 4 unique options and exactly 1 correct`() {
        val generator = ExerciseGenerator(Random(42))
        val testNumbers = listOf(0, 6, 15, 21, 50, 60, 70, 71, 78, 80, 81, 91, 97, 100, 200, 201, 1984, 9999)
        testNumbers.forEach { n ->
            val ex = generator.generateDragAndDrop(n)
            assertEquals(4, ex.options.size)
            assertEquals(4, ex.options.distinct().size)
            assertEquals(1, ex.options.count { it == ex.expectedFrench })
        }
    }

    @Test
    fun `word assembly tokens cover expected number cleanly`() {
        val generator = ExerciseGenerator(Random(7))
        val ex = generator.generateWordAssembly(97)
        assertEquals(listOf("quatre", "vingt", "dix", "sept"), ex.expectedTokens)
        assertTrue(ex.shuffledPool.size >= ex.expectedTokens.size)
    }

    @Test
    fun `skill mastery is strictly independent across modalities`() {
        val scheduler = AdaptiveScheduler()
        val now = 1_700_000_000_000L
        val updated = scheduler.recordAttempt(
            existing = null,
            number = 78,
            mode = LearningMode.RECOGNITION,
            isCorrect = true,
            responseTimeMs = 1500L,
            nowMillis = now
        )
        assertEquals(1, updated.skills[LearningMode.RECOGNITION]?.correct)
        assertEquals(0, updated.skills[LearningMode.SPEAKING]?.attempts)
        assertEquals(0, updated.skills[LearningMode.LISTENING]?.attempts)
        assertEquals(0, updated.skills[LearningMode.WRITING]?.attempts)
        assertNull(updated.skills[LearningMode.SPEAKING]?.accuracyPercent)
    }

    @Test
    fun `listening error updates listening weakness and same-session retry schedules after 3 to 5 items`() {
        val scheduler = AdaptiveScheduler()
        val now = 1_700_000_000_000L

        val afterErr = scheduler.recordAttempt(
            existing = null,
            number = 91,
            mode = LearningMode.LISTENING,
            isCorrect = false,
            responseTimeMs = 3200L,
            nowMillis = now,
            confusedWith = 71
        )
        assertEquals(1, afterErr.skills[LearningMode.LISTENING]?.attempts)
        assertEquals(0, afterErr.skills[LearningMode.LISTENING]?.correct)
        assertEquals(1, afterErr.confusionTargets[71])
        assertTrue(91 in scheduler.getFrequentlyMissedNumbers(mapOf(91 to afterErr)))

        scheduler.scheduleSameSessionRetry(91, LearningMode.LISTENING, delayQuestions = 4)
        assertNull(scheduler.pollDueRetry(LearningMode.LISTENING)) // 3 left
        assertNull(scheduler.pollDueRetry(LearningMode.LISTENING)) // 2 left
        assertNull(scheduler.pollDueRetry(LearningMode.LISTENING)) // 1 left
        val due = scheduler.pollDueRetry(LearningMode.LISTENING) // 0 left
        assertNotNull(due)
        assertEquals(91, due?.number)
    }

    @Test
    fun `mastered number is scheduled for delayed review across heuristic intervals`() {
        val scheduler = AdaptiveScheduler(listOf(1, 3, 7, 14, 30))
        var now = 1_700_000_000_000L
        var prog = scheduler.recordAttempt(null, 80, LearningMode.WRITING, true, 2100L, now)
        assertEquals(1, prog.skills[LearningMode.WRITING]?.reviewIntervalDays)

        now += AdaptiveScheduler.DAY_MILLIS
        prog = scheduler.recordAttempt(prog, 80, LearningMode.WRITING, true, 1900L, now)
        assertEquals(3, prog.skills[LearningMode.WRITING]?.reviewIntervalDays)

        now += 3 * AdaptiveScheduler.DAY_MILLIS
        prog = scheduler.recordAttempt(prog, 80, LearningMode.WRITING, true, 1700L, now)
        assertEquals(7, prog.skills[LearningMode.WRITING]?.reviewIntervalDays)

        val expectedNext = now + 7 * AdaptiveScheduler.DAY_MILLIS
        assertEquals(expectedNext, prog.skills[LearningMode.WRITING]?.nextReviewAt)
        assertTrue(scheduler.getOverdueNumbers(mapOf(80 to prog), LearningMode.WRITING, now + 1000L).isEmpty())
        assertEquals(listOf(80), scheduler.getOverdueNumbers(mapOf(80 to prog), LearningMode.WRITING, expectedNext + 1000L))
    }

    @Test
    fun `diagnostic engine samples across all strata and excludes ASR from core score`() {
        val engine = DiagnosticEngine(ExerciseGenerator(Random(11)))
        val items = engine.buildStratifiedDiagnosticExercises(includeSpeaking = false)
        assertTrue(items.size >= 20)
        val categoriesCovered = items.flatMap { FrenchNumberEngine.categoriesFor(it.targetNumber) }.toSet()
        assertTrue("0-19" in categoriesCovered)
        assertTrue("20-69" in categoriesCovered)
        assertTrue("70-99" in categoriesCovered)
        assertTrue("et-un" in categoriesCovered)
    }

    @Test
    fun `difficulty categorization identifies complex numbers`() {
        assertTrue(FrenchNumberEngine.isComplexNumber(21))
        assertTrue(FrenchNumberEngine.isComplexNumber(71))
        assertTrue(FrenchNumberEngine.isComplexNumber(80))
        assertTrue(FrenchNumberEngine.isComplexNumber(91))
        assertFalse(FrenchNumberEngine.isComplexNumber(24))
        assertFalse(FrenchNumberEngine.isComplexNumber(10))
    }

    @Test
    fun `mobile phone generation produces valid 5-pair 06 or 07 prefixes`() {
        val gen = ExerciseGenerator(Random(99))
        val ex = gen.generateTelephone(TelephoneSubMode.FULL_TEN_DIGITS, mobileOnly = true)
        assertEquals(5, ex.pairs.size)
        assertTrue(ex.pairs.first() == "06" || ex.pairs.first() == "07")
        assertTrue(ex.pairs.all { it.length == 2 && it.all { ch -> ch.isDigit() } })
    }
}
