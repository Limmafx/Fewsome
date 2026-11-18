package com.example.domain

/**
 * Deterministic metropolitan French (fr-FR) number conversion, decomposition,
 * and phonetic/IPA engine for integers 0..9999 and 2-digit telephone pairs.
 */
object FrenchNumberEngine {

    private val units = arrayOf(
        "zéro", "un", "deux", "trois", "quatre", "cinq",
        "six", "sept", "huit", "neuf", "dix", "onze",
        "douze", "treize", "quatorze", "quinze", "seize",
        "dix-sept", "dix-huit", "dix-neuf"
    )

    private val tens = arrayOf(
        "", "", "vingt", "trente", "quarante", "cinquante", "soixante"
    )

    private val ipaUnits = arrayOf(
        "ze.ʁo", "œ̃", "dø", "tʁwa", "katʁ", "sɛ̃k",
        "sis", "sɛt", "ɥit", "nœf", "dis", "ɔ̃z",
        "duz", "tʁɛz", "ka.tɔʁz", "kɛ̃z", "sɛz",
        "di.sɛt", "di.z‿ɥit", "diz.nœf"
    )

    fun toFrench(number: Int): String {
        require(number in 0..9999) { "Number must be between 0 and 9999, got $number" }
        return convertInternal(number)
    }

    private fun convertInternal(n: Int): String {
        return when {
            n < 20 -> units[n]
            n < 70 -> {
                val t = n / 10
                val u = n % 10
                when (u) {
                    0 -> tens[t]
                    1 -> "${tens[t]} et un"
                    else -> "${tens[t]}-${units[u]}"
                }
            }
            n < 80 -> {
                val rem = n - 60
                when (rem) {
                    11 -> "soixante et onze"
                    else -> "soixante-${units[rem]}"
                }
            }
            n == 80 -> "quatre-vingts"
            n < 100 -> {
                val rem = n - 80
                "quatre-vingt-${units[rem]}"
            }
            n == 100 -> "cent"
            n < 200 -> "cent ${convertInternal(n - 100)}"
            n < 1000 -> {
                val hundreds = n / 100
                val rem = n % 100
                if (rem == 0) {
                    "${units[hundreds]} cents"
                } else {
                    "${units[hundreds]} cent ${convertInternal(rem)}"
                }
            }
            n == 1000 -> "mille"
            n < 2000 -> "mille ${convertInternal(n - 1000)}"
            else -> {
                val thousands = n / 1000
                val rem = n % 1000
                if (rem == 0) {
                    "${units[thousands]} mille"
                } else {
                    "${units[thousands]} mille ${convertInternal(rem)}"
                }
            }
        }
    }

    /**
     * Converts a 2-digit telephone pair string ("00".."99") preserving leading zeros.
     */
    fun pairToFrench(pair: String): String {
        val clean = pair.trim()
        require(clean.length == 2 && clean.all { it.isDigit() }) {
            "Telephone pair must be 2 digits, got '$pair'"
        }
        val d1 = clean[0].digitToInt()
        val d2 = clean[1].digitToInt()
        return if (d1 == 0) {
            "zéro ${units[d2]}"
        } else {
            toFrench(clean.toInt())
        }
    }

    /**
     * Converts a list of 2-digit telephone pairs into spoken metropolitan French.
     */
    fun phonePairsToFrench(pairs: List<String>): String {
        return pairs.joinToString(", ") { pairToFrench(it) }
    }

    /**
     * Returns all valid canonical/1990-reform orthographic variants for a given integer.
     * Enforces correct plural -s on quatre-vingts / cents and proper "et".
     */
    fun acceptedFrenchVariants(number: Int): List<String> {
        val canonical = toFrench(number)
        val variants = linkedSetOf(canonical)
        // 1990 spelling reform allows hyphens between all number words (e.g. "vingt-et-un", "deux-cents")
        variants.add(canonical.replace(" ", "-"))
        return variants.toList()
    }

    fun acceptedPairVariants(pair: String): List<String> {
        val canonical = pairToFrench(pair)
        val variants = linkedSetOf(canonical, canonical.replace(" ", "-"))
        if (pair == "00") {
            variants.add("double zéro")
            variants.add("double-zéro")
        }
        return variants.toList()
    }

    /**
     * Splits a French number expression into individual word tokens for Word Assembly mode.
     */
    fun tokenizeForAssembly(number: Int): List<String> {
        return toFrench(number)
            .split('-', ' ')
            .filter { it.isNotBlank() }
    }

    /**
     * Provides a concise structural decomposition in Russian for pedagogical feedback.
     */
    fun decompose(number: Int): String {
        if (number !in 0..9999) return ""
        val parts = mutableListOf<String>()
        val thousands = number / 1000
        val hundreds = (number % 1000) / 100
        val lastTwo = number % 100

        if (thousands > 0) {
            parts.add(if (thousands == 1) "1000 (mille)" else "$thousands × 1000 (${units[thousands]} mille)")
        }
        if (hundreds > 0) {
            parts.add(
                when {
                    hundreds == 1 -> "100 (cent)"
                    lastTwo == 0 -> "$hundreds × 100 (${units[hundreds]} cents — мн.ч. -s)"
                    else -> "$hundreds × 100 (${units[hundreds]} cent — без -s)"
                }
            )
        }
        if (lastTwo > 0 || number == 0) {
            when {
                lastTwo == 0 && number == 0 -> parts.add("0 (zéro)")
                lastTwo < 20 -> parts.add("$lastTwo (${units[lastTwo]})")
                lastTwo in listOf(21, 31, 41, 51, 61) -> {
                    val t = (lastTwo / 10) * 10
                    parts.add("$t + 1 (${tens[lastTwo / 10]} + et + un)")
                }
                lastTwo < 70 -> {
                    val t = (lastTwo / 10) * 10
                    val u = lastTwo % 10
                    if (u == 0) parts.add("$t (${tens[lastTwo / 10]})")
                    else parts.add("$t + $u (${tens[lastTwo / 10]}-${units[u]})")
                }
                lastTwo == 70 -> parts.add("60 + 10 (soixante-dix)")
                lastTwo == 71 -> parts.add("60 + 11 (soixante et onze)")
                lastTwo in 72..79 -> parts.add("60 + ${lastTwo - 60} (soixante-${units[lastTwo - 60]})")
                lastTwo == 80 -> parts.add("4 × 20 (quatre-vingts — с -s на конце)")
                lastTwo in 81..89 -> parts.add("4 × 20 + ${lastTwo - 80} (quatre-vingt-${units[lastTwo - 80]} — без et, без -s)")
                lastTwo == 90 -> parts.add("4 × 20 + 10 (quatre-vingt-dix)")
                lastTwo == 91 -> parts.add("4 × 20 + 11 (quatre-vingt-onze — без et)")
                else -> parts.add("4 × 20 + ${lastTwo - 80} (quatre-vingt-${units[lastTwo - 80]})")
            }
        }
        return parts.joinToString(" + ")
    }

    /**
     * Classifies a number into structural pedagogical categories.
     */
    fun categoriesFor(number: Int): List<String> {
        val result = mutableListOf<String>()
        val lastTwo = number % 100
        when {
            number in 0..19 -> result.add("0-19")
            number in 20..69 -> result.add("20-69")
            number in 70..99 -> result.add("70-99")
            number in 100..999 -> result.add("100-999")
            number >= 1000 -> result.add("1000-9999")
        }
        if (lastTwo in listOf(21, 31, 41, 51, 61, 71)) {
            result.add("et-un")
        }
        if (isComplexNumber(number)) {
            result.add("complex")
        }
        return result
    }

    fun isComplexNumber(number: Int): Boolean {
        val lastTwo = number % 100
        return lastTwo in listOf(21, 31, 41, 51, 61) || lastTwo in 70..99
    }

    /**
     * Generates accurate metropolitan French IPA transcription corresponding
     * to the canonical spoken expression of `number`.
     */
    fun toIpa(number: Int): String {
        require(number in 0..9999)
        return "/${ipaInternal(number)}/"
    }

    fun pairToIpa(pair: String): String {
        val clean = pair.trim()
        require(clean.length == 2 && clean.all { it.isDigit() })
        return if (clean[0] == '0') {
            "/ze.ʁo ${ipaUnits[clean[1].digitToInt()]}/"
        } else {
            toIpa(clean.toInt())
        }
    }

    private fun ipaInternal(n: Int): String {
        return when {
            n < 20 -> ipaUnits[n]
            n < 70 -> {
                val t = n / 10
                val u = n % 10
                val tensIpa = when (t) {
                    2 -> "vɛ̃"
                    3 -> "tʁɑ̃t"
                    4 -> "ka.ʁɑ̃t"
                    5 -> "sɛ̃.kɑ̃t"
                    6 -> "swa.sɑ̃t"
                    else -> ""
                }
                when (u) {
                    0 -> tensIpa
                    1 -> if (t == 2) "vɛ̃.t‿e.œ̃" else "$tensIpa.t‿e.œ̃".replace(".t‿", ".e.")
                        .let { if (t == 2) "vɛ̃.t‿e.œ̃" else "$tensIpa.e.œ̃" }
                    else -> {
                        val prefix = if (t == 2) "vɛ̃t" else tensIpa
                        "$prefix.${ipaUnits[u]}"
                    }
                }
            }
            n == 70 -> "swa.sɑ̃t.dis"
            n == 71 -> "swa.sɑ̃t.e.ɔ̃z"
            n in 72..79 -> "swa.sɑ̃t.${ipaUnits[n - 60]}"
            n == 80 -> "ka.tʁə.vɛ̃"
            n in 81..99 -> "ka.tʁə.vɛ̃.${ipaUnits[n - 80]}"
            n == 100 -> "sɑ̃"
            n < 200 -> "sɑ̃ ${ipaInternal(n - 100)}"
            n < 1000 -> {
                val h = n / 100
                val rem = n % 100
                val hIpa = when (h) {
                    2 -> "dø"; 3 -> "tʁwa"; 4 -> "katʁ"; 5 -> "sɛ̃k"
                    6 -> "sis"; 7 -> "sɛt"; 8 -> "ɥit"; 9 -> "nœf"
                    else -> ""
                }
                if (rem == 0) "$hIpa sɑ̃" else "$hIpa sɑ̃ ${ipaInternal(rem)}"
            }
            n == 1000 -> "mil"
            n < 2000 -> "mil ${ipaInternal(n - 1000)}"
            else -> {
                val k = n / 1000
                val rem = n % 1000
                val kIpa = ipaUnits[k]
                if (rem == 0) "$kIpa mil" else "$kIpa mil ${ipaInternal(rem)}"
            }
        }
    }
}
