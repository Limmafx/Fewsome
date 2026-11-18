package com.example.domain

import java.text.Normalizer
import java.util.Locale

data class ValidationResult(
    val isCorrect: Boolean,
    val normalizedInput: String,
    val canonicalExpected: String,
    val recognizedNumericMatch: Int? = null
)

/**
 * Deterministic, accent-insensitive, hyphen/whitespace-tolerant French answer
 * normalization and validation engine. Does NOT accept misspelled or missing words.
 */
object AnswerNormalizer {

    private val diacriticsRegex = Regex("\\p{InCombiningDiacriticalMarks}+")
    private val separatorRegex = Regex("[\\s\\-,;]+")

    /**
     * Strips accents/diacritics (e.g., "zéro" -> "zero", "août" -> "aout"),
     * lowercases, and normalizes hyphens/commas/multiple spaces into single spaces.
     */
    fun normalizeFrench(input: String): String {
        val decomposed = Normalizer.normalize(input.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val withoutAccents = diacriticsRegex.replace(decomposed, "")
        return separatorRegex.replace(withoutAccents, " ").trim()
    }

    /**
     * Validates a user's French text answer against the target integer.
     * Rules:
     * 1. Case-insensitive.
     * 2. Accent-insensitive ("zero" == "zéro").
     * 3. Hyphens and spaces are equivalent separators ("quatre vingt onze" == "quatre-vingt-onze").
     * 4. Repeated whitespace collapsed.
     * 5. Valid orthographic variants accepted ("vingt et un" == "vingt-et-un").
     * 6. Incorrect spelling, missing words, or wrong plural/et forms are rejected.
     */
    fun validateNumberAnswer(userInput: String, expectedNumber: Int): ValidationResult {
        val normalizedInput = normalizeFrench(userInput)
        val canonical = FrenchNumberEngine.toFrench(expectedNumber)
        if (normalizedInput.isEmpty()) {
            return ValidationResult(
                isCorrect = false,
                normalizedInput = normalizedInput,
                canonicalExpected = canonical
            )
        }
        val acceptedNormalized = FrenchNumberEngine
            .acceptedFrenchVariants(expectedNumber)
            .map { normalizeFrench(it) }
            .toSet()

        if (normalizedInput in acceptedNormalized) {
            return ValidationResult(
                isCorrect = true,
                normalizedInput = normalizedInput,
                canonicalExpected = canonical,
                recognizedNumericMatch = expectedNumber
            )
        }

        // Check if the user's answer corresponds to a DIFFERENT valid French number (0..9999)
        // to diagnose confusion pairs accurately.
        val matchedOther = findMatchingNumber(normalizedInput)
        return ValidationResult(
            isCorrect = false,
            normalizedInput = normalizedInput,
            canonicalExpected = canonical,
            recognizedNumericMatch = matchedOther
        )
    }

    /**
     * Validates a single 2-digit phone pair ("00".."99"). Preserves leading zero!
     * "six" is REJECTED for "06".
     */
    fun validatePhonePairAnswer(userInput: String, expectedPair: String): ValidationResult {
        val normalizedInput = normalizeFrench(userInput)
        val canonical = FrenchNumberEngine.pairToFrench(expectedPair)
        val acceptedNormalized = FrenchNumberEngine
            .acceptedPairVariants(expectedPair)
            .map { normalizeFrench(it) }
            .toSet()

        val isCorrect = normalizedInput in acceptedNormalized
        return ValidationResult(
            isCorrect = isCorrect,
            normalizedInput = normalizedInput,
            canonicalExpected = canonical
        )
    }

    /**
     * Validates a full phone number written in French words across its 2-digit pairs.
     */
    fun validateFullPhoneWords(userInput: String, expectedPairs: List<String>): ValidationResult {
        val normalizedInput = normalizeFrench(userInput)
        val canonical = FrenchNumberEngine.phonePairsToFrench(expectedPairs)
        val expectedNormalized = normalizeFrench(canonical)
        if (normalizedInput == expectedNormalized) {
            return ValidationResult(
                isCorrect = true,
                normalizedInput = normalizedInput,
                canonicalExpected = canonical
            )
        }
        // Also support "double zéro" for any "00" pair in the sequence
        if (expectedPairs.contains("00")) {
            val altCanonical = expectedPairs.joinToString(", ") {
                if (it == "00") "double zéro" else FrenchNumberEngine.pairToFrench(it)
            }
            if (normalizedInput == normalizeFrench(altCanonical)) {
                return ValidationResult(
                    isCorrect = true,
                    normalizedInput = normalizedInput,
                    canonicalExpected = canonical
                )
            }
        }
        return ValidationResult(
            isCorrect = false,
            normalizedInput = normalizedInput,
            canonicalExpected = canonical
        )
    }

    /**
     * Validates typed digits for listening / dictation phone exercises.
     * Preserves leading zeros! E.g. "06 12 34 56 78" or "0612345678".
     */
    fun validatePhoneDigits(userInput: String, expectedPairs: List<String>): Boolean {
        val userDigits = userInput.filter { it.isDigit() }
        val expectedDigits = expectedPairs.joinToString("")
        return userDigits.isNotEmpty() && userDigits == expectedDigits
    }

    /**
     * Validates typed integer answer for Listening Variation B (audio -> digits).
     */
    fun validateNumericDigits(userInput: String, expectedNumber: Int): Boolean {
        val trimmed = userInput.trim()
        if (trimmed.isEmpty() || !trimmed.all { it.isDigit() }) return false
        return trimmed.toIntOrNull() == expectedNumber
    }

    /**
     * Deterministic reverse lookup over 0..9999 (fast ~1ms) to identify if
     * the learner accidentally typed another valid number (e.g., 71 instead of 91).
     */
    private val reverseLookupMap: Map<String, Int> by lazy {
        buildMap(10000) {
            for (n in 0..9999) {
                put(normalizeFrench(FrenchNumberEngine.toFrench(n)), n)
            }
        }
    }

    fun findMatchingNumber(normalizedInput: String): Int? = reverseLookupMap[normalizedInput]
}
