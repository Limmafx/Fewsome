package com.example.domain

enum class LearningMode(val titleRu: String) {
    RECOGNITION("Узнавание"),
    WRITING("Письмо"),
    LISTENING("Аудирование"),
    SPEAKING("Произношение")
}

enum class SpeakingOutcomeType {
    SELF_REPORTED,
    ASR_TRANSCRIPTION,
    MANUAL_RECORDING
}

data class SkillRecord(
    val attempts: Int = 0,
    val correct: Int = 0,
    val consecutiveCorrect: Int = 0,
    val lastPracticedAt: Long? = null,
    val nextReviewAt: Long? = null,
    val averageResponseMs: Long? = null,
    val recentResponseTimes: List<Long> = emptyList(),
    val reviewIntervalDays: Int = 0,
    val selfReportedCorrect: Int = 0,
    val asrMatchedCount: Int = 0
) {
    val accuracyPercent: Int?
        get() = if (attempts == 0) null else ((correct * 100.0) / attempts).toInt()
}

data class NumberProgress(
    val number: Int,
    val categories: List<String> = FrenchNumberEngine.categoriesFor(number),
    val skills: Map<LearningMode, SkillRecord> = LearningMode.entries.associateWith { SkillRecord() },
    val confusionTargets: Map<Int, Int> = emptyMap()
) {
    val totalAttempts: Int get() = skills.values.sumOf { it.attempts }
    val totalCorrect: Int get() = skills.values.sumOf { it.correct }
    val totalIncorrect: Int get() = totalAttempts - totalCorrect
}

enum class TrainingMode(
    val titleRu: String,
    val subtitleRu: String
) {
    DRAG_AND_DROP("Режим A · Карточки", "Перетаскивание или выбор верной карточки"),
    WRITING("Режим B · Письмо", "Напиши число по-французски"),
    ACTIVE_RECALL("Вспомни самостоятельно", "Активное извлечение из памяти без подсказок"),
    WORD_ASSEMBLY("Режим C · Сборка", "Собери число из карточек-слов"),
    LISTENING("Понимание на слух", "Распознай французское число по аудио"),
    PRONUNCIATION("Произнеси число", "Теневое повторение и речевой отклик"),
    TELEPHONE("Телефонные номера", "Пары цифр и 10-значные номера Франции"),
    CONFUSION_DRILL("Похожие числа", "Контрастная тренировка ваших частых ошибок"),
    MISTAKES_REVIEW("Работа над ошибками", "Прицельное повторение трудных чисел"),
    DAILY_TRAINING("Ежедневная тренировка", "Смешанная сессия по вашим приоритетам"),
    ASSESSMENT("Контроль знаний", "Независимая проверка без подсказок"),
    SPEED_FLUENCY("Реакция и скорость", "Ускорение беглого узнавания без потери точности"),
    CONTEXTUAL("Числа в жизни", "Цены, адреса, возраст, даты и телефоны"),
    GUIDED_RULES("Правила и разбор", "Этап 1: логика построения и примеры"),
    DIAGNOSTIC("Диагностика знаний", "Оценка навыков и персональный план")
}

enum class DifficultyLevel(
    val titleRu: String,
    val rangeLabel: String,
    val minVal: Int,
    val maxVal: Int
) {
    BEGINNER("Начальный", "0–20", 0, 20),
    ELEMENTARY("Базовый", "0–69", 0, 69),
    INTERMEDIATE("Средний", "70–99", 70, 99),
    STANDARD("Стандартный", "0–100", 0, 100),
    ADVANCED("Продвинутый", "0–999", 0, 999),
    EXPERT("Эксперт", "0–9999", 0, 9999),
    COMPLEX("Сложные французские числа", "21, 31, 70–99...", 21, 99),
    CUSTOM("Свой диапазон", "Настраиваемый", 0, 100)
}

enum class SessionLength(val labelRu: String, val count: Int) {
    TEN("10 вопросов", 10),
    TWENTY("20 вопросов", 20),
    FIFTY("50 вопросов", 50),
    UNLIMITED("Без ограничений", Int.MAX_VALUE)
}

enum class DailyDuration(val labelRu: String, val targetQuestions: Int, val minutesLabel: String) {
    QUICK_2_MIN("Быстрая", 8, "~2 мин"),
    STANDARD_10_MIN("Стандартная", 20, "~10 мин"),
    EXTENDED_20_MIN("Расширенная", 36, "~20 мин"),
    CUSTOM("Своя длина", 15, "Своя")
}

enum class TelephoneSubMode(val titleRu: String, val pairCount: Int) {
    SINGLE_PAIR("1 пара (напр. 06, 34)", 1),
    TWO_PAIRS("2 пары (напр. 06 12)", 2),
    THREE_PAIRS("3 пары (напр. 06 12 34)", 3),
    FULL_TEN_DIGITS("Полный номер (5 пар)", 5),
    FULL_DICTATION_LISTENING("Аудио-диктант номера", 5)
}

enum class ListeningVariation(val titleRu: String) {
    MULTIPLE_CHOICE_NUMBERS("Аудио → выбор числа"),
    TYPED_DIGITS("Аудио → ввод цифр")
}

enum class PronunciationActivity(val titleRu: String) {
    SHADOWING("A · Повтор за диктором (Shadowing)"),
    SPEECH_RECALL("B · Произнеси по памяти")
}

enum class PedagogicalStage(val stageNumber: Int, val titleRu: String, val descriptionRu: String) {
    STAGE_1_GUIDED(1, "Этап 1 · Понимание правил", "Знакомство со структурой и примерами"),
    STAGE_2_RECOGNITION(2, "Этап 2 · Узнавание", "Карточки и выбор правильного написания"),
    STAGE_3_PRODUCTION(3, "Этап 3 · Письменный отклик", "Самостоятельный ввод без вариантов"),
    STAGE_4_AUDIO_SPEECH(4, "Этап 4 · Слух и речь", "Аудирование и устное воспроизведение"),
    STAGE_5_FLUENCY(5, "Этап 5 · Беглость", "Смешанные задания с целевым временем"),
    STAGE_6_RETENTION(6, "Этап 6 · Долгосрочное удержание", "Отложенные контрольные проверки")
}

sealed class Exercise {
    abstract val id: String
    abstract val targetNumber: Int
    abstract val learningMode: LearningMode
    abstract val promptRu: String
    abstract val displayTarget: String
    abstract val expectedFrench: String
    abstract val audioFrenchText: String
    abstract val ipa: String
    abstract val decompositionRu: String
    abstract val isAssessmentItem: Boolean
    abstract val targetTimeMs: Long?

    data class DragAndDrop(
        override val id: String,
        override val targetNumber: Int,
        override val displayTarget: String = targetNumber.toString(),
        override val expectedFrench: String,
        override val audioFrenchText: String = expectedFrench,
        override val ipa: String,
        override val decompositionRu: String,
        val options: List<String>,
        override val isAssessmentItem: Boolean = false,
        override val targetTimeMs: Long? = null,
        val contrastExplanationRu: String? = null
    ) : Exercise() {
        override val learningMode = LearningMode.RECOGNITION
        override val promptRu = "Перетащите или выберите верную карточку"
    }

    data class Writing(
        override val id: String,
        override val targetNumber: Int,
        override val displayTarget: String = targetNumber.toString(),
        override val expectedFrench: String,
        override val audioFrenchText: String = expectedFrench,
        override val ipa: String,
        override val decompositionRu: String,
        val isActiveRecall: Boolean = false,
        val contextSentenceFr: String? = null,
        val contextTranslationRu: String? = null,
        override val isAssessmentItem: Boolean = false,
        override val targetTimeMs: Long? = null,
        val contrastExplanationRu: String? = null
    ) : Exercise() {
        override val learningMode = LearningMode.WRITING
        override val promptRu = if (isActiveRecall) {
            "Вспомни число по-французски без подсказок"
        } else {
            "Напиши число по-французски"
        }
    }

    data class WordAssembly(
        override val id: String,
        override val targetNumber: Int,
        override val displayTarget: String = targetNumber.toString(),
        override val expectedFrench: String,
        override val audioFrenchText: String = expectedFrench,
        override val ipa: String,
        override val decompositionRu: String,
        val expectedTokens: List<String>,
        val shuffledPool: List<WordToken>,
        override val isAssessmentItem: Boolean = false,
        override val targetTimeMs: Long? = null
    ) : Exercise() {
        override val learningMode = LearningMode.WRITING
        override val promptRu = "Собери число из карточек в верном порядке"
    }

    data class Listening(
        override val id: String,
        override val targetNumber: Int,
        override val expectedFrench: String,
        override val audioFrenchText: String = expectedFrench,
        override val ipa: String,
        override val decompositionRu: String,
        val variation: ListeningVariation,
        val numericChoices: List<Int> = emptyList(),
        val contextSentenceFr: String? = null,
        val contextTranslationRu: String? = null,
        override val isAssessmentItem: Boolean = false,
        override val targetTimeMs: Long? = null
    ) : Exercise() {
        override val learningMode = LearningMode.LISTENING
        override val displayTarget = "?"
        override val promptRu = when (variation) {
            ListeningVariation.MULTIPLE_CHOICE_NUMBERS -> "Прослушайте аудио и выберите число"
            ListeningVariation.TYPED_DIGITS -> "Прослушайте аудио и введите число цифрами"
        }
    }

    data class Pronunciation(
        override val id: String,
        override val targetNumber: Int,
        override val displayTarget: String = targetNumber.toString(),
        override val expectedFrench: String,
        override val audioFrenchText: String = expectedFrench,
        override val ipa: String,
        override val decompositionRu: String,
        val activity: PronunciationActivity,
        override val isAssessmentItem: Boolean = false,
        override val targetTimeMs: Long? = null
    ) : Exercise() {
        override val learningMode = LearningMode.SPEAKING
        override val promptRu = when (activity) {
            PronunciationActivity.SHADOWING -> "Прослушайте диктора, запишите свой голос и сравните"
            PronunciationActivity.SPEECH_RECALL -> "Произнесите число вслух по-французски"
        }
    }

    data class Telephone(
        override val id: String,
        override val targetNumber: Int, // representative first or combined hash for tracking
        val pairs: List<String>,
        val subMode: TelephoneSubMode,
        val isDictationListening: Boolean,
        val wordOptionsPool: List<WordToken> = emptyList(),
        override val expectedFrench: String,
        override val audioFrenchText: String = expectedFrench,
        override val ipa: String,
        override val decompositionRu: String,
        override val isAssessmentItem: Boolean = false,
        override val targetTimeMs: Long? = null
    ) : Exercise() {
        override val learningMode = if (isDictationListening) LearningMode.LISTENING else LearningMode.WRITING
        override val displayTarget = if (isDictationListening) "☎ ?" else pairs.joinToString(" ")
        override val promptRu = when {
            isDictationListening -> "Прослушайте номер парами и запишите цифры (сохраняя 0)"
            subMode == TelephoneSubMode.SINGLE_PAIR -> "Напишите телефонную пару по-французски (с ведущим нулём)"
            else -> "Введите телефонный номер по-французски (по парам)"
        }
    }
}

data class WordToken(
    val id: String,
    val text: String
)

enum class MistakeCategory(val titleRu: String) {
    VIGESIMAL_CONFUSION("Путаница 60/70/80/90"),
    TEEN_TEN_CONFUSION("Путаница 13–19 и десятков"),
    ET_UN_RULE("Правило союза «et»"),
    PLURAL_AGREEMENT("Окончание -s у vingts/cents"),
    LEADING_ZERO_OMITTED("Потеря ведущего нуля"),
    SPELLING_OR_ORDER("Орфография или порядок слов"),
    LISTENING_DISCRIMINATION("Различение на слух"),
    UNANSWERED("Пропущено / без ответа")
}

data class MistakeAnalysis(
    val category: MistakeCategory,
    val explanationRu: String,
    val decompositionRu: String,
    val confusedWithNumber: Int? = null
)

data class SessionQuestionOutcome(
    val exercise: Exercise,
    val userAnswer: String,
    val isCorrect: Boolean,
    val responseTimeMs: Long,
    val mistakeAnalysis: MistakeAnalysis? = null,
    val audioReplays: Int = 0,
    val speakingOutcomeType: SpeakingOutcomeType? = null
)

data class SessionSummary(
    val mode: TrainingMode,
    val totalAnswered: Int,
    val correctCount: Int,
    val incorrectCount: Int,
    val accuracyPercent: Int,
    val averageResponseMs: Long,
    val difficultItems: List<SessionQuestionOutcome>,
    val isAssessment: Boolean = false
)
