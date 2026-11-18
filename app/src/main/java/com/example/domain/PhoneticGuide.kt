package com.example.domain

data class PhoneticLesson(
    val id: String,
    val titleRu: String,
    val focusAreaRu: String,
    val explanationRu: String,
    val examples: List<PhoneticExample>
)

data class PhoneticExample(
    val displayNumber: String,
    val frenchText: String,
    val ipa: String,
    val noteRu: String
)

data class GuidedRuleCard(
    val id: String,
    val titleRu: String,
    val rangeTag: String,
    val ruleExplanationRu: String,
    val examples: List<Pair<String, String>> // Display -> French
)

/**
 * Curated metropolitan French (fr-FR) phonetic rules & Stage 1 guided construction lessons.
 * Uses real IPA matching the exact spoken French expression (no approximated Russian transliteration).
 */
object PhoneticGuide {

    val lessons: List<PhoneticLesson> = listOf(
        PhoneticLesson(
            id = "nasals",
            titleRu = "Носовые гласные /ɑ̃/ и /ɛ̃/",
            focusAreaRu = "Носовой резонанс без призвука согласной [n]",
            explanationRu = "В сочетаниях -an-, -en- (trente, cent) воздух идёт через рот и нос одновременно, кончик языка опущен (/ɑ̃/). В сочетаниях -in-, -un- (cinq, vingt, quinze) звучит передний носовой /ɛ̃/ или /œ̃/.",
            examples = listOf(
                PhoneticExample("30", "trente", "/tʁɑ̃t/", "Открытый носовой /ɑ̃/, конечное -e не читается"),
                PhoneticExample("50", "cinquante", "/sɛ̃.kɑ̃t/", "Оба носовых в одном слове: /ɛ̃/ и /ɑ̃/"),
                PhoneticExample("20", "vingt", "/vɛ̃/", "Обе конечные согласные -gt немые"),
                PhoneticExample("15", "quinze", "/kɛ̃z/", "Буквосочетание qu- даёт твёрдый /k/")
            )
        ),
        PhoneticLesson(
            id = "vowels",
            titleRu = "Различение гласных /y/, /u/, /ø/, /œ/",
            focusAreaRu = "Огубленные передние и задние гласные",
            explanationRu = "В «huit» губы вытянуты трубочкой, а язык у передних зубов (/ɥi/). В «douze» язык оттянут назад (/u/). «Deux» звучит закрыто (/ø/), а «neuf» перед согласной f — более открыто (/œ/).",
            examples = listOf(
                PhoneticExample("8", "huit", "/ɥit/", "Полугласный /ɥ/ + /i/, конечное -t чёткое"),
                PhoneticExample("12", "douze", "/duz/", "Глубокий /u/ без призвука /ю/"),
                PhoneticExample("2", "deux", "/dø/", "Закрытый /ø/, конечное -x немое"),
                PhoneticExample("9", "neuf", "/nœf/", "Открытый /œ/ перед слышимым -f")
            )
        ),
        PhoneticLesson(
            id = "silent_finals",
            titleRu = "Немые конечные согласные",
            focusAreaRu = "Когда -s, -t, -x не произносятся",
            explanationRu = "В изолированных числах конечные буквы часто немые: -s в trois, quatre-vingts, deux cents; -t в vingt, cent; -x в deux. Но в 6 и 10 в конце фразы -x читается как /s/!",
            examples = listOf(
                PhoneticExample("3", "trois", "/tʁwa/", "Конечное -s немое"),
                PhoneticExample("80", "quatre-vingts", "/ka.tʁə.vɛ̃/", "Окончание -gts полностью немое"),
                PhoneticExample("200", "deux cents", "/dø sɑ̃/", "Немые -x и -ts"),
                PhoneticExample("6 / 10", "six, dix", "/sis/, /dis/", "В конце фразы -x звучит как /s/")
            )
        ),
        PhoneticLesson(
            id = "liaisons",
            titleRu = "Связность речи (liaison) в составных числах",
            focusAreaRu = "Связывание на стыке слов",
            explanationRu = "В «vingt et un» буква -t связывается с «et» (/vɛ̃.t‿e.œ̃/). В «dix-huit» и «dix-neuf» -x озвончается в /z/. А вот в «quatre-vingt-un» и «quatre-vingt-onze» связывания НЕТ (/ka.tʁə.vɛ̃.œ̃/)!",
            examples = listOf(
                PhoneticExample("21", "vingt et un", "/vɛ̃.t‿e.œ̃/", "Связывание /t/ перед союзом «et»"),
                PhoneticExample("18", "dix-huit", "/di.z‿ɥit/", "Озвончение -x в /z/ перед гласным звуком"),
                PhoneticExample("81", "quatre-vingt-un", "/ka.tʁə.vɛ̃.œ̃/", "Связывание /t/ НЕ делается"),
                PhoneticExample("91", "quatre-vingt-onze", "/ka.tʁə.vɛ̃.ɔ̃z/", "Связывание /t/ НЕ делается, пауза лёгкая")
            )
        ),
        PhoneticLesson(
            id = "rhythm",
            titleRu = "Ритм длинных чисел и телефонов",
            focusAreaRu = "Ударение на последний слог ритмической группы",
            explanationRu = "Французские составные числа объединяются в единую ритмическую группу с ударением только на последний произносимый слог. Телефонные номера делятся строго на 5 пар с лёгкой паузой между ними.",
            examples = listOf(
                PhoneticExample("97", "quatre-vingt-dix-sept", "/ka.tʁə.vɛ̃.di.sɛt/", "Ударение на финальном «-sept»"),
                PhoneticExample("78", "soixante-dix-huit", "/swa.sɑ̃t.di.z‿ɥit/", "Плавный переход с озвончением /z/"),
                PhoneticExample("06 12", "zéro six, douze", "/ze.ʁo sis, duz/", "Чёткое деление на двузначные пары")
            )
        )
    )

    val guidedStage1Rules: List<GuidedRuleCard> = listOf(
        GuidedRuleCard(
            id = "rule_0_19",
            titleRu = "1. База 0–19 (запоминаем корни)",
            rangeTag = "0–19",
            ruleExplanationRu = "Числа 0–16 имеют уникальные формы. С 17 по 19 строятся по схеме 10 + цифра: dix-sept, dix-huit, dix-neuf.",
            examples = listOf("0" to "zéro", "11" to "onze", "16" to "seize", "18" to "dix-huit")
        ),
        GuidedRuleCard(
            id = "rule_20_69",
            titleRu = "2. Десятки 20–69 и союз «et»",
            rangeTag = "20–69",
            ruleExplanationRu = "К десятку (vingt, trente, quarante, cinquante, soixante) для единицы «1» добавляется союз «et» (vingt et un), остальные соединяются дефисом.",
            examples = listOf("21" to "vingt et un", "34" to "trente-quatre", "51" to "cinquante et un", "69" to "soixante-neuf")
        ),
        GuidedRuleCard(
            id = "rule_70_79",
            titleRu = "3. Система 60+ (70–79)",
            rangeTag = "70–79",
            ruleExplanationRu = "Во Франции 70 строится как 60 + 10..19: soixante-dix (70), soixante et onze (71, с союзом et!), soixante-douze (72) ... soixante-dix-neuf (79).",
            examples = listOf("70" to "soixante-dix", "71" to "soixante et onze", "75" to "soixante-quinze", "78" to "soixante-dix-huit")
        ),
        GuidedRuleCard(
            id = "rule_80_99",
            titleRu = "4. Двадцатеричная система 4×20 (80–99)",
            rangeTag = "80–99",
            ruleExplanationRu = "80 = quatre-vingts (пишется с -s!). Но как только за 80 идут числа 81–99, -s исчезает, а союз «et» НЕ используется: quatre-vingt-un (81), quatre-vingt-onze (91).",
            examples = listOf("80" to "quatre-vingts", "81" to "quatre-vingt-un", "91" to "quatre-vingt-onze", "97" to "quatre-vingt-dix-sept")
        ),
        GuidedRuleCard(
            id = "rule_100_9999",
            titleRu = "5. Сотни и тысячи (согласование -s)",
            rangeTag = "100–9999",
            ruleExplanationRu = "«cent» (100) и «mille» (1000) употребляются без «un». Круглые сотни от 200 до 900 берут -s (deux cents), но перед другими цифрами -s исчезает (deux cent un). «mille» никогда не принимает -s.",
            examples = listOf("100" to "cent", "200" to "deux cents", "201" to "deux cent un", "2000" to "deux mille")
        ),
        GuidedRuleCard(
            id = "rule_phones",
            titleRu = "6. Французские телефонные номера",
            rangeTag = "06 XX XX XX XX",
            ruleExplanationRu = "10-значный номер всегда читается пятью парами по две цифры. Ведущий ноль обязательно произносится: 06 → zéro six, 09 → zéro neuf.",
            examples = listOf("06" to "zéro six", "09" to "zéro neuf", "06 12 34 56 78" to "zéro six, douze, trente-quatre, cinquante-six, soixante-dix-huit")
        )
    )
}
