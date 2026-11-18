package com.example.domain

import java.util.UUID
import kotlin.random.Random

/**
 * Deterministic-capable exercise generator for all training modes, difficulty levels,
 * telephone numbers, confusion contrast drills, and practical contexts.
 */
class ExerciseGenerator(private val random: Random = Random.Default) {

    private val complexNumbers = listOf(
        21, 31, 41, 51, 61,
        70, 71, 72, 73, 74, 75, 76, 77, 78, 79,
        80, 81, 82, 85, 88, 89,
        90, 91, 92, 94, 96, 97, 98, 99,
        180, 200, 201, 280, 1000, 1971, 2080
    )

    private val candidateConfusionSets: List<List<Int>> = listOf(
        listOf(60, 70),
        listOf(70, 80, 90),
        listOf(71, 81, 91),
        listOf(15, 50),
        listOf(16, 60),
        listOf(17, 70),
        listOf(80, 81),
        listOf(90, 91),
        listOf(77, 87, 97),
        listOf(78, 68, 88),
        listOf(21, 31, 41),
        listOf(200, 201)
    )

    fun pickNumber(
        difficulty: DifficultyLevel,
        customMin: Int = 0,
        customMax: Int = 100,
        exclude: Set<Int> = emptySet()
    ): Int {
        if (difficulty == DifficultyLevel.COMPLEX) {
            val candidates = complexNumbers.filter { it !in exclude }
            return (candidates.ifEmpty { complexNumbers }).random(random)
        }
        val (min, max) = when (difficulty) {
            DifficultyLevel.CUSTOM -> {
                val lo = customMin.coerceIn(0, 9999)
                val hi = customMax.coerceIn(lo, 9999)
                lo to hi
            }
            else -> difficulty.minVal to difficulty.maxVal
        }
        if (min == max) return min
        for (attempt in 0..25) {
            val candidate = random.nextInt(min, max + 1)
            if (candidate !in exclude) return candidate
        }
        return random.nextInt(min, max + 1)
    }

    fun generateDragAndDrop(
        number: Int,
        isAssessment: Boolean = false,
        targetTimeMs: Long? = null,
        contrastExplanationRu: String? = null
    ): Exercise.DragAndDrop {
        val correctText = FrenchNumberEngine.toFrench(number)
        val distractorNumbers = generatePlausibleDistractorNumbers(number, count = 3)
        val options = (listOf(correctText) + distractorNumbers.map { FrenchNumberEngine.toFrench(it) })
            .distinct()
            .let { list ->
                // Guarantee 4 distinct options even for edge cases
                if (list.size == 4) list else fillOptionsToFour(number, list)
            }
            .shuffled(random)

        return Exercise.DragAndDrop(
            id = UUID.randomUUID().toString(),
            targetNumber = number,
            expectedFrench = correctText,
            ipa = FrenchNumberEngine.toIpa(number),
            decompositionRu = FrenchNumberEngine.decompose(number),
            options = options,
            isAssessmentItem = isAssessment,
            targetTimeMs = targetTimeMs,
            contrastExplanationRu = contrastExplanationRu
        )
    }

    fun generateWriting(
        number: Int,
        isActiveRecall: Boolean = false,
        isAssessment: Boolean = false,
        targetTimeMs: Long? = null,
        contrastExplanationRu: String? = null
    ): Exercise.Writing {
        return Exercise.Writing(
            id = UUID.randomUUID().toString(),
            targetNumber = number,
            expectedFrench = FrenchNumberEngine.toFrench(number),
            ipa = FrenchNumberEngine.toIpa(number),
            decompositionRu = FrenchNumberEngine.decompose(number),
            isActiveRecall = isActiveRecall,
            isAssessmentItem = isAssessment,
            targetTimeMs = targetTimeMs,
            contrastExplanationRu = contrastExplanationRu
        )
    }

    fun generateWordAssembly(
        number: Int,
        isAssessment: Boolean = false,
        targetTimeMs: Long? = null
    ): Exercise.WordAssembly {
        val expectedTokens = FrenchNumberEngine.tokenizeForAssembly(number)
        val distractors = generateNonAmbiguousTokenDistractors(number, expectedTokens)
        val pool = (expectedTokens + distractors).mapIndexed { idx, token ->
            WordToken(id = "${idx}_$token", text = token)
        }.shuffled(random)

        return Exercise.WordAssembly(
            id = UUID.randomUUID().toString(),
            targetNumber = number,
            expectedFrench = FrenchNumberEngine.toFrench(number),
            ipa = FrenchNumberEngine.toIpa(number),
            decompositionRu = FrenchNumberEngine.decompose(number),
            expectedTokens = expectedTokens,
            shuffledPool = pool,
            isAssessmentItem = isAssessment,
            targetTimeMs = targetTimeMs
        )
    }

    fun generateListening(
        number: Int,
        variation: ListeningVariation,
        isAssessment: Boolean = false,
        targetTimeMs: Long? = null
    ): Exercise.Listening {
        val choices = if (variation == ListeningVariation.MULTIPLE_CHOICE_NUMBERS) {
            (listOf(number) + generatePlausibleDistractorNumbers(number, count = 3)).shuffled(random)
        } else {
            emptyList()
        }
        return Exercise.Listening(
            id = UUID.randomUUID().toString(),
            targetNumber = number,
            expectedFrench = FrenchNumberEngine.toFrench(number),
            ipa = FrenchNumberEngine.toIpa(number),
            decompositionRu = FrenchNumberEngine.decompose(number),
            variation = variation,
            numericChoices = choices,
            isAssessmentItem = isAssessment,
            targetTimeMs = targetTimeMs
        )
    }

    fun generatePronunciation(
        number: Int,
        activity: PronunciationActivity
    ): Exercise.Pronunciation {
        return Exercise.Pronunciation(
            id = UUID.randomUUID().toString(),
            targetNumber = number,
            expectedFrench = FrenchNumberEngine.toFrench(number),
            ipa = FrenchNumberEngine.toIpa(number),
            decompositionRu = FrenchNumberEngine.decompose(number),
            activity = activity
        )
    }

    /**
     * Generates realistic metropolitan French phone numbers preserving leading zeros.
     * Mobile numbers start with 06 or 07; landlines/VoIP start with 01..05 or 09.
     */
    fun generateTelephone(
        subMode: TelephoneSubMode,
        mobileOnly: Boolean = true
    ): Exercise.Telephone {
        val pairs = generatePhonePairs(subMode.pairCount, mobileOnly)
        val expectedFrench = FrenchNumberEngine.phonePairsToFrench(pairs)
        val ipa = pairs.joinToString(", ") { FrenchNumberEngine.pairToIpa(it).removeSurrounding("/") }
        val decomp = pairs.joinToString(" · ") { "$it = ${FrenchNumberEngine.pairToFrench(it)}" }
        val representativeNumber = pairs.first().toInt()
        val pool = if (subMode == TelephoneSubMode.SINGLE_PAIR || subMode == TelephoneSubMode.TWO_PAIRS) {
            val tokens = pairs.flatMap { FrenchNumberEngine.pairToFrench(it).split(' ', '-') }
                .filter { it.isNotBlank() }
            val extra = listOf("zéro", "soixante", "quatre", "vingt", "dix")
                .filter { it !in tokens }
                .take(2)
            (tokens + extra).mapIndexed { i, t -> WordToken("tel_${i}_$t", t) }.shuffled(random)
        } else emptyList()

        return Exercise.Telephone(
            id = UUID.randomUUID().toString(),
            targetNumber = representativeNumber,
            pairs = pairs,
            subMode = subMode,
            isDictationListening = subMode == TelephoneSubMode.FULL_DICTATION_LISTENING,
            wordOptionsPool = pool,
            expectedFrench = expectedFrench,
            audioFrenchText = pairs.joinToString(", ") { FrenchNumberEngine.pairToFrench(it) },
            ipa = "/$ipa/",
            decompositionRu = decomp
        )
    }

    fun generatePhonePairs(pairCount: Int, mobileOnly: Boolean): List<String> {
        val prefixes = if (mobileOnly) listOf("06", "07") else listOf("01", "02", "03", "04", "05", "06", "07", "09")
        val pairs = mutableListOf<String>()
        if (pairCount == 1) {
            // Bias slightly toward leading-zero pairs like 01..09 so leading zero rule is practiced
            val valInt = if (random.nextFloat() < 0.45f) random.nextInt(0, 10) else random.nextInt(10, 100)
            pairs.add(valInt.toString().padStart(2, '0'))
        } else {
            pairs.add(prefixes.random(random))
            repeat(pairCount - 1) {
                val valInt = random.nextInt(0, 100)
                pairs.add(valInt.toString().padStart(2, '0'))
            }
        }
        return pairs
    }

    /**
     * Generates practical French number exercises: prices, addresses, apartments,
     * ages, dates, quantities, and phone numbers.
     */
    fun generateContextual(isListening: Boolean = false): Exercise {
        val kind = random.nextInt(6)
        return when (kind) {
            0 -> { // Price
                val price = pickNumber(DifficultyLevel.STANDARD)
                val fr = FrenchNumberEngine.toFrench(price)
                val sentenceFr = "Ça coûte $fr euros."
                val promptFr = "Ça coûte $price euros."
                val ru = "Это стоит $price евро."
                if (isListening) {
                    Exercise.Listening(
                        id = UUID.randomUUID().toString(),
                        targetNumber = price,
                        expectedFrench = fr,
                        audioFrenchText = sentenceFr,
                        ipa = FrenchNumberEngine.toIpa(price),
                        decompositionRu = "$ru · ${FrenchNumberEngine.decompose(price)}",
                        variation = ListeningVariation.TYPED_DIGITS,
                        contextSentenceFr = sentenceFr,
                        contextTranslationRu = ru
                    )
                } else {
                    Exercise.Writing(
                        id = UUID.randomUUID().toString(),
                        targetNumber = price,
                        displayTarget = promptFr,
                        expectedFrench = fr,
                        audioFrenchText = sentenceFr,
                        ipa = FrenchNumberEngine.toIpa(price),
                        decompositionRu = "$ru · ${FrenchNumberEngine.decompose(price)}",
                        isActiveRecall = true,
                        contextSentenceFr = sentenceFr,
                        contextTranslationRu = ru
                    )
                }
            }
            1 -> { // Street address
                val num = random.nextInt(1, 180)
                val fr = FrenchNumberEngine.toFrench(num)
                val sentenceFr = "J'habite au numéro $fr."
                val ru = "Я живу в доме номер $num."
                Exercise.Writing(
                    id = UUID.randomUUID().toString(),
                    targetNumber = num,
                    displayTarget = "J'habite au numéro $num.",
                    expectedFrench = fr,
                    audioFrenchText = sentenceFr,
                    ipa = FrenchNumberEngine.toIpa(num),
                    decompositionRu = "$ru · ${FrenchNumberEngine.decompose(num)}",
                    isActiveRecall = true,
                    contextSentenceFr = sentenceFr,
                    contextTranslationRu = ru
                )
            }
            2 -> { // Apartment number
                val apt = random.nextInt(10, 99)
                val fr = FrenchNumberEngine.toFrench(apt)
                val sentenceFr = "Appartement $fr."
                val ru = "Квартира $apt."
                Exercise.Listening(
                    id = UUID.randomUUID().toString(),
                    targetNumber = apt,
                    expectedFrench = fr,
                    audioFrenchText = sentenceFr,
                    ipa = FrenchNumberEngine.toIpa(apt),
                    decompositionRu = "$ru · ${FrenchNumberEngine.decompose(apt)}",
                    variation = ListeningVariation.MULTIPLE_CHOICE_NUMBERS,
                    numericChoices = (listOf(apt) + generatePlausibleDistractorNumbers(apt, 3)).shuffled(random),
                    contextSentenceFr = sentenceFr,
                    contextTranslationRu = ru
                )
            }
            3 -> { // Age
                val age = random.nextInt(12, 95)
                val fr = FrenchNumberEngine.toFrench(age)
                val sentenceFr = "J'ai $fr ans."
                val ru = "Мне $age лет."
                Exercise.Writing(
                    id = UUID.randomUUID().toString(),
                    targetNumber = age,
                    displayTarget = "J'ai $age ans.",
                    expectedFrench = fr,
                    audioFrenchText = sentenceFr,
                    ipa = FrenchNumberEngine.toIpa(age),
                    decompositionRu = "$ru · ${FrenchNumberEngine.decompose(age)}",
                    isActiveRecall = true,
                    contextSentenceFr = sentenceFr,
                    contextTranslationRu = ru
                )
            }
            4 -> { // Date
                val day = random.nextInt(2, 31)
                val months = listOf("janvier", "mars", "mai", "juillet", "août", "octobre")
                val month = months.random(random)
                val fr = FrenchNumberEngine.toFrench(day)
                val sentenceFr = "Le $fr $month."
                val ru = "Дата: $day $month."
                Exercise.Listening(
                    id = UUID.randomUUID().toString(),
                    targetNumber = day,
                    expectedFrench = fr,
                    audioFrenchText = sentenceFr,
                    ipa = FrenchNumberEngine.toIpa(day),
                    decompositionRu = "$ru · ${FrenchNumberEngine.decompose(day)}",
                    variation = ListeningVariation.TYPED_DIGITS,
                    contextSentenceFr = sentenceFr,
                    contextTranslationRu = ru
                )
            }
            else -> { // Quantity
                val qty = listOf(12, 21, 24, 30, 50, 75, 80, 99, 120, 200).random(random)
                val fr = FrenchNumberEngine.toFrench(qty)
                val sentenceFr = "Il y a $fr pages."
                val ru = "Здесь $qty страниц."
                Exercise.Writing(
                    id = UUID.randomUUID().toString(),
                    targetNumber = qty,
                    displayTarget = "Il y a $qty pages.",
                    expectedFrench = fr,
                    audioFrenchText = sentenceFr,
                    ipa = FrenchNumberEngine.toIpa(qty),
                    decompositionRu = "$ru · ${FrenchNumberEngine.decompose(qty)}",
                    isActiveRecall = true,
                    contextSentenceFr = sentenceFr,
                    contextTranslationRu = ru
                )
            }
        }
    }

    /**
     * Generates a focused contrast drill alternating between a user's confused numbers
     * (or a candidate confusion set if no errors logged yet).
     */
    fun generateConfusionDrillSequence(
        userProgress: Map<Int, NumberProgress>,
        count: Int = 10
    ): List<Exercise> {
        val topUserPairs = userProgress.values
            .flatMap { prog -> prog.confusionTargets.entries.map { (other, freq) -> Triple(prog.number, other, freq) } }
            .sortedByDescending { it.third }

        val activeSet: List<Int> = if (topUserPairs.isNotEmpty()) {
            val top = topUserPairs.first()
            listOf(top.first, top.second)
        } else {
            candidateConfusionSets.random(random)
        }

        val explain = buildContrastExplanation(activeSet)
        return (0 until count).map { idx ->
            val num = activeSet[idx % activeSet.size]
            when (idx % 3) {
                0 -> generateDragAndDrop(num, contrastExplanationRu = explain)
                1 -> generateListening(num, ListeningVariation.MULTIPLE_CHOICE_NUMBERS)
                else -> generateWriting(num, isActiveRecall = true, contrastExplanationRu = explain)
            }
        }
    }

    private fun buildContrastExplanation(numbers: List<Int>): String {
        return numbers.distinct().joinToString("\n") { n ->
            "$n = ${FrenchNumberEngine.decompose(n)}"
        }
    }

    /**
     * Generates 3 plausible incorrect numbers based on authentic French number confusions
     * (e.g., 78 -> 68, 88, 77; 71 -> 91, 61, 81; 16 -> 60, 15, 26).
     */
    fun generatePlausibleDistractorNumbers(target: Int, count: Int = 3): List<Int> {
        val candidates = linkedSetOf<Int>()

        // 1. Check candidate confusion sets first
        candidateConfusionSets.filter { target in it }.forEach { set ->
            set.filter { it != target }.forEach { candidates.add(it) }
        }

        val lastTwo = target % 100
        val base = target - lastTwo

        // 2. Same unit, shifted vigesimal / decimal tens
        val unit = target % 10
        when {
            lastTwo in 70..79 -> {
                candidates.add(base + 60 + (lastTwo - 70))
                candidates.add(base + 80 + (lastTwo - 70))
                candidates.add(base + 90 + (lastTwo - 70))
            }
            lastTwo in 80..99 -> {
                val offset = lastTwo - 80
                if (offset < 20) candidates.add(base + 60 + offset)
                candidates.add(base + (if (lastTwo >= 90) lastTwo - 20 else lastTwo + 10))
                candidates.add(base + (if (lastTwo >= 90) lastTwo - 10 else lastTwo - 20))
            }
            lastTwo in 13..19 -> {
                candidates.add(base + (lastTwo - 10) * 10)
            }
            lastTwo in listOf(30, 40, 50, 60) -> {
                candidates.add(base + 10 + lastTwo / 10)
            }
        }

        // 3. +/- 1, +/- 10, +/- 20, +/- 100 neighbors within 0..9999
        listOf(-1, 1, -10, 10, -20, 20, -100, 100).forEach { delta ->
            val neighbor = target + delta
            if (neighbor in 0..9999 && neighbor != target) {
                candidates.add(neighbor)
            }
        }
        if (unit in 1..9 && target >= 10) {
            val swap = (target - lastTwo) + unit * 10 + (lastTwo / 10)
            if (swap in 0..9999 && swap != target) candidates.add(swap)
        }

        val valid = candidates.filter { it in 0..9999 && it != target }.toMutableList()
        while (valid.size < count) {
            val fallback = if (target <= 100) random.nextInt(0, 101) else random.nextInt(0, 1000)
            if (fallback != target && fallback !in valid) valid.add(fallback)
        }
        return valid.shuffled(random).take(count)
    }

    private fun fillOptionsToFour(target: Int, existing: List<String>): List<String> {
        val result = existing.toMutableList()
        var candidate = 0
        while (result.size < 4 && candidate <= 9999) {
            if (candidate != target) {
                val fr = FrenchNumberEngine.toFrench(candidate)
                if (fr !in result) result.add(fr)
            }
            candidate++
        }
        return result.take(4)
    }

    private fun generateNonAmbiguousTokenDistractors(
        target: Int,
        expectedTokens: List<String>
    ): List<String> {
        val candidatePool = when {
            target in 70..99 -> listOf("soixante", "quatre", "vingt", "dix", "et", "onze", "neuf", "sept")
            target in 20..69 -> listOf("soixante", "et", "dix", "un", "deux", "vingt", "quatre")
            target >= 100 -> listOf("cents", "cent", "mille", "vingts", "et", "un", "deux")
            else -> listOf("dix", "et", "vingt", "sept", "neuf")
        }
        val expectedSet = expectedTokens.toSet()
        // Pick distractors that aren't identical to single-word answers and don't create ambiguity
        val filtered = candidatePool.filter { token ->
            token !in expectedSet &&
                // Avoid adding "cents" if "cent" is expected (or vice versa) to keep feedback clean, unless >100
                !(token == "et" && target % 100 in listOf(81, 91) && "vingt" in expectedSet)
        }
        return filtered.shuffled(random).take(3)
    }
}
