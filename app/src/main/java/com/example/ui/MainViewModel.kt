package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.audio.ListeningTrainer
import com.example.data.audio.PronunciationTrainer
import com.example.data.local.AppDatabase
import com.example.data.local.DiagnosticSnapshotEntity
import com.example.data.local.PreferencesRepository
import com.example.data.local.ThemePreference
import com.example.data.local.UserPreferences
import com.example.data.repository.ProgressRepository
import com.example.domain.AnswerNormalizer
import com.example.domain.DailyDuration
import com.example.domain.DashboardMetrics
import com.example.domain.DiagnosticEngine
import com.example.domain.DiagnosticReport
import com.example.domain.DifficultyLevel
import com.example.domain.ErrorClassifier
import com.example.domain.Exercise
import com.example.domain.ExerciseGenerator
import com.example.domain.FrenchNumberEngine
import com.example.domain.LearningDashboard
import com.example.domain.LearningMode
import com.example.domain.ListeningVariation
import com.example.domain.MistakeAnalysis
import com.example.domain.NumberProgress
import com.example.domain.PronunciationActivity
import com.example.domain.SessionLength
import com.example.domain.SessionQuestionOutcome
import com.example.domain.SessionSummary
import com.example.domain.SpeakingOutcomeType
import com.example.domain.TelephoneSubMode
import com.example.domain.TrainingMode
import com.example.domain.WordToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppScreen {
    MAIN_TABS,
    GUIDED_RULES,
    PHONETICS,
    EXERCISE,
    SESSION_SUMMARY,
    DIAGNOSTIC_REPORT
}

data class ExerciseFeedback(
    val isCorrect: Boolean,
    val userAnswerDisplay: String,
    val expectedFrench: String,
    val ipa: String,
    val decompositionRu: String,
    val mistakeAnalysis: MistakeAnalysis?,
    val responseTimeMs: Long,
    val metSpeedTarget: Boolean? = null,
    val asrTranscript: String? = null,
    val speakingOutcomeType: SpeakingOutcomeType? = null
)

data class ActiveSessionUiState(
    val mode: TrainingMode,
    val difficulty: DifficultyLevel,
    val plannedCount: Int,
    val currentIndex: Int = 0,
    val currentExercise: Exercise,
    val prebuiltQueue: List<Exercise> = emptyList(),
    val outcomes: List<SessionQuestionOutcome> = emptyList(),
    val feedback: ExerciseFeedback? = null,
    val textInput: String = "",
    val assembledTokens: List<WordToken> = emptyList(),
    val selectedOption: String? = null,
    val audioReplays: Int = 0,
    val questionTimerStartMs: Long = System.currentTimeMillis(),
    val listeningAudioFinished: Boolean = false,
    val asrStatusMessage: String? = null,
    val asrTranscript: String? = null,
    val isPressureFree: Boolean = true,
    val speedTargetMs: Long = 2800L,
    val sessionStreak: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val progressRepo = ProgressRepository(
        progressDao = db.numberProgressDao(),
        sessionDao = db.sessionHistoryDao(),
        diagnosticDao = db.diagnosticSnapshotDao()
    )
    private val prefsRepo = PreferencesRepository(application)
    val listeningTrainer = ListeningTrainer(application)
    val pronunciationTrainer = PronunciationTrainer(application)

    private val generator = ExerciseGenerator()
    private val diagnosticEngine = DiagnosticEngine(generator)

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferencesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserPreferences()
    )

    val progressMap: StateFlow<Map<Int, NumberProgress>> = progressRepo.progressMapFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val latestDiagnostic: StateFlow<DiagnosticSnapshotEntity?> = progressRepo.latestDiagnosticFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        progressRepo.progressMapFlow,
        progressRepo.sessionTrendsFlow
    ) { map, trends ->
        LearningDashboard.compute(
            progressMap = map,
            sessions = trends,
            scheduler = progressRepo.scheduler,
            nowMillis = System.currentTimeMillis()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LearningDashboard.compute(
            progressMap = emptyMap(),
            sessions = emptyList(),
            scheduler = progressRepo.scheduler,
            nowMillis = System.currentTimeMillis()
        )
    )

    // Home / configuration selections
    private val _selectedMode = MutableStateFlow(TrainingMode.DRAG_AND_DROP)
    val selectedMode: StateFlow<TrainingMode> = _selectedMode.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(DifficultyLevel.STANDARD)
    val selectedDifficulty: StateFlow<DifficultyLevel> = _selectedDifficulty.asStateFlow()

    private val _selectedLength = MutableStateFlow(SessionLength.TEN)
    val selectedLength: StateFlow<SessionLength> = _selectedLength.asStateFlow()

    private val _selectedDailyDuration = MutableStateFlow(DailyDuration.STANDARD_10_MIN)
    val selectedDailyDuration: StateFlow<DailyDuration> = _selectedDailyDuration.asStateFlow()

    private val _selectedTelephoneSubMode = MutableStateFlow(TelephoneSubMode.FULL_TEN_DIGITS)
    val selectedTelephoneSubMode: StateFlow<TelephoneSubMode> = _selectedTelephoneSubMode.asStateFlow()

    private val _mobilePhonesOnly = MutableStateFlow(true)
    val mobilePhonesOnly: StateFlow<Boolean> = _mobilePhonesOnly.asStateFlow()

    private val _selectedListeningVariation = MutableStateFlow(ListeningVariation.MULTIPLE_CHOICE_NUMBERS)
    val selectedListeningVariation: StateFlow<ListeningVariation> = _selectedListeningVariation.asStateFlow()

    private val _selectedPronunciationActivity = MutableStateFlow(PronunciationActivity.SHADOWING)
    val selectedPronunciationActivity: StateFlow<PronunciationActivity> = _selectedPronunciationActivity.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.MAIN_TABS)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeSession = MutableStateFlow<ActiveSessionUiState?>(null)
    val activeSession: StateFlow<ActiveSessionUiState?> = _activeSession.asStateFlow()

    private val _sessionSummary = MutableStateFlow<SessionSummary?>(null)
    val sessionSummary: StateFlow<SessionSummary?> = _sessionSummary.asStateFlow()

    private val _diagnosticReport = MutableStateFlow<DiagnosticReport?>(null)
    val diagnosticReport: StateFlow<DiagnosticReport?> = _diagnosticReport.asStateFlow()

    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()

    fun selectTab(index: Int) {
        _selectedTabIndex.value = index.coerceIn(0, 3)
    }

    fun selectMode(mode: TrainingMode) { _selectedMode.value = mode }
    fun selectDifficulty(diff: DifficultyLevel) { _selectedDifficulty.value = diff }
    fun selectLength(length: SessionLength) { _selectedLength.value = length }
    fun selectDailyDuration(dur: DailyDuration) { _selectedDailyDuration.value = dur }
    fun selectTelephoneSubMode(sub: TelephoneSubMode) { _selectedTelephoneSubMode.value = sub }
    fun setMobilePhonesOnly(mobileOnly: Boolean) { _mobilePhonesOnly.value = mobileOnly }
    fun selectListeningVariation(variation: ListeningVariation) { _selectedListeningVariation.value = variation }
    fun selectPronunciationActivity(activity: PronunciationActivity) { _selectedPronunciationActivity.value = activity }

    fun openGuidedRules() { _currentScreen.value = AppScreen.GUIDED_RULES }
    fun openPhonetics() { _currentScreen.value = AppScreen.PHONETICS }

    fun returnToMainTabs() {
        listeningTrainer.stop()
        pronunciationTrainer.release()
        _activeSession.value = null
        _currentScreen.value = AppScreen.MAIN_TABS
    }

    // Preferences mutations
    fun setTheme(theme: ThemePreference) = viewModelScope.launch { prefsRepo.setTheme(theme) }
    fun dismissDiagnosticBanner() = viewModelScope.launch { prefsRepo.setDiagnosticDismissed(true) }
    fun setVoiceExercisesEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setVoiceExercisesEnabled(enabled) }
    fun setVaryVoices(vary: Boolean) = viewModelScope.launch { prefsRepo.setVaryVoices(vary) }
    fun setCustomRange(min: Int, max: Int) = viewModelScope.launch { prefsRepo.setCustomRange(min, max) }
    fun setSpeedSettings(targetMs: Long, pressureFree: Boolean) = viewModelScope.launch {
        prefsRepo.setSpeedSettings(targetMs, pressureFree)
    }
    fun resetAllProgress() = viewModelScope.launch {
        progressRepo.clearAllProgress()
        prefsRepo.resetAll()
    }

    fun speakText(frenchText: String, slow: Boolean = false, onFinished: () -> Unit = {}) {
        val rate = if (slow) 0.72f else 1.0f
        listeningTrainer.speak(
            frenchText = frenchText,
            speechRate = rate,
            varyVoice = preferences.value.varyVoices,
            onComplete = onFinished
        )
    }

    private fun ListeningTrainer.speak(
        frenchText: String,
        speechRate: Float,
        varyVoice: Boolean,
        onComplete: () -> Unit
    ) {
        this.speak(
            frenchText = frenchText,
            speechRate = speechRate,
            varyVoice = varyVoice,
            preferredVoiceIndex = null,
            onFinished = onComplete
        )
    }

    fun startTrainingSession(
        mode: TrainingMode = _selectedMode.value,
        difficulty: DifficultyLevel = _selectedDifficulty.value,
        length: SessionLength = _selectedLength.value,
        customQueueNumbers: List<Int>? = null
    ) {
        if (mode == TrainingMode.GUIDED_RULES) {
            openGuidedRules()
            return
        }
        viewModelScope.launch {
            progressRepo.scheduler.clearSessionQueue()
            pronunciationTrainer.clearTempSample()
            val prefs = preferences.value
            val snapshot = progressRepo.getAllProgressSnapshot()

            val queue: List<Exercise>
            val plannedCount: Int
            val firstExercise: Exercise

            when (mode) {
                TrainingMode.DIAGNOSTIC -> {
                    queue = diagnosticEngine.buildStratifiedDiagnosticExercises(
                        includeSpeaking = prefs.voiceExercisesEnabled && pronunciationTrainer.isSpeechRecognitionSupported()
                    )
                    plannedCount = queue.size
                    firstExercise = queue.first()
                }
                TrainingMode.CONFUSION_DRILL -> {
                    val count = if (length == SessionLength.UNLIMITED) 12 else length.count
                    queue = generator.generateConfusionDrillSequence(snapshot, count = count)
                    plannedCount = queue.size
                    firstExercise = queue.first()
                }
                TrainingMode.ASSESSMENT -> {
                    val count = 16
                    queue = buildAssessmentQueue(count, difficulty, prefs)
                    plannedCount = queue.size
                    firstExercise = queue.first()
                }
                TrainingMode.DAILY_TRAINING -> {
                    val count = _selectedDailyDuration.value.targetQuestions
                    queue = buildDailyQueue(count, difficulty, prefs, snapshot)
                    plannedCount = queue.size
                    firstExercise = queue.first()
                }
                else -> {
                    if (customQueueNumbers != null && customQueueNumbers.isNotEmpty()) {
                        queue = customQueueNumbers.map { num ->
                            buildExerciseForMode(mode, num, difficulty, prefs, snapshot)
                        }
                        plannedCount = queue.size
                        firstExercise = queue.first()
                    } else {
                        queue = emptyList()
                        plannedCount = length.count
                        firstExercise = buildNextDynamicExercise(mode, difficulty, prefs, snapshot, lastNumber = null)
                    }
                }
            }

            _sessionSummary.value = null
            _diagnosticReport.value = null
            _activeSession.value = ActiveSessionUiState(
                mode = mode,
                difficulty = difficulty,
                plannedCount = plannedCount,
                currentIndex = 0,
                currentExercise = firstExercise,
                prebuiltQueue = if (queue.isNotEmpty()) queue.drop(1) else emptyList(),
                questionTimerStartMs = System.currentTimeMillis(),
                listeningAudioFinished = firstExercise !is Exercise.Listening &&
                    !(firstExercise is Exercise.Telephone && firstExercise.isDictationListening),
                isPressureFree = mode != TrainingMode.SPEED_FLUENCY && prefs.pressureFreeSpeed,
                speedTargetMs = prefs.speedTargetMs
            )
            _currentScreen.value = AppScreen.EXERCISE
            maybeAutoPlayAudio(firstExercise)
        }
    }

    private fun buildAssessmentQueue(
        count: Int,
        difficulty: DifficultyLevel,
        prefs: UserPreferences
    ): List<Exercise> {
        val chosenNumbers = linkedSetOf<Int>()
        val strata = listOf(
            generator.pickNumber(DifficultyLevel.BEGINNER),
            generator.pickNumber(DifficultyLevel.BEGINNER),
            generator.pickNumber(DifficultyLevel.ELEMENTARY),
            generator.pickNumber(DifficultyLevel.ELEMENTARY),
            generator.pickNumber(DifficultyLevel.INTERMEDIATE),
            generator.pickNumber(DifficultyLevel.INTERMEDIATE),
            generator.pickNumber(DifficultyLevel.COMPLEX),
            generator.pickNumber(DifficultyLevel.COMPLEX),
            generator.pickNumber(DifficultyLevel.ADVANCED),
            generator.pickNumber(DifficultyLevel.EXPERT)
        )
        chosenNumbers.addAll(strata)
        while (chosenNumbers.size < count) {
            chosenNumbers.add(generator.pickNumber(difficulty, prefs.customMin, prefs.customMax, chosenNumbers))
        }
        return chosenNumbers.take(count).mapIndexed { idx, num ->
            when (idx % 4) {
                0 -> generator.generateDragAndDrop(num, isAssessment = true)
                1 -> generator.generateWriting(num, isActiveRecall = true, isAssessment = true)
                2 -> generator.generateListening(num, ListeningVariation.MULTIPLE_CHOICE_NUMBERS, isAssessment = true)
                else -> generator.generateListening(num, ListeningVariation.TYPED_DIGITS, isAssessment = true)
            }
        }
    }

    private fun buildDailyQueue(
        count: Int,
        difficulty: DifficultyLevel,
        prefs: UserPreferences,
        snapshot: Map<Int, NumberProgress>
    ): List<Exercise> {
        val overdue = progressRepo.scheduler.getOverdueNumbers(snapshot, nowMillis = System.currentTimeMillis())
        val missed = progressRepo.scheduler.getFrequentlyMissedNumbers(snapshot)
        val priorityPool = (overdue + missed).distinct()

        return (0 until count).map { idx ->
            val num = if (idx < priorityPool.size && idx % 2 == 0) {
                priorityPool[idx]
            } else {
                generator.pickNumber(difficulty, prefs.customMin, prefs.customMax)
            }
            when (idx % 5) {
                0 -> generator.generateDragAndDrop(num)
                1 -> generator.generateWriting(num, isActiveRecall = true)
                2 -> generator.generateListening(num, ListeningVariation.TYPED_DIGITS)
                3 -> generator.generateWordAssembly(num)
                else -> if (prefs.voiceExercisesEnabled) {
                    generator.generatePronunciation(num, PronunciationActivity.SHADOWING)
                } else {
                    generator.generateListening(num, ListeningVariation.MULTIPLE_CHOICE_NUMBERS)
                }
            }
        }
    }

    private fun buildNextDynamicExercise(
        mode: TrainingMode,
        difficulty: DifficultyLevel,
        prefs: UserPreferences,
        snapshot: Map<Int, NumberProgress>,
        lastNumber: Int?
    ): Exercise {
        val mistakesOnly = mode == TrainingMode.MISTAKES_REVIEW
        val targetLearningMode = when (mode) {
            TrainingMode.DRAG_AND_DROP, TrainingMode.SPEED_FLUENCY -> LearningMode.RECOGNITION
            TrainingMode.LISTENING -> LearningMode.LISTENING
            TrainingMode.PRONUNCIATION -> LearningMode.SPEAKING
            else -> LearningMode.WRITING
        }
        val num = progressRepo.scheduler.selectNextNumber(
            difficulty = difficulty,
            customMin = prefs.customMin,
            customMax = prefs.customMax,
            learningMode = targetLearningMode,
            allProgress = snapshot,
            generator = generator,
            nowMillis = System.currentTimeMillis(),
            lastNumber = lastNumber,
            mistakesOnly = mistakesOnly
        )
        return buildExerciseForMode(mode, num, difficulty, prefs, snapshot)
    }

    private fun buildExerciseForMode(
        mode: TrainingMode,
        number: Int,
        difficulty: DifficultyLevel,
        prefs: UserPreferences,
        snapshot: Map<Int, NumberProgress>
    ): Exercise {
        val targetMs = if (mode == TrainingMode.SPEED_FLUENCY) prefs.speedTargetMs else null
        return when (mode) {
            TrainingMode.DRAG_AND_DROP -> generator.generateDragAndDrop(number)
            TrainingMode.SPEED_FLUENCY -> generator.generateDragAndDrop(number, targetTimeMs = targetMs)
            TrainingMode.WRITING -> generator.generateWriting(number, isActiveRecall = false)
            TrainingMode.ACTIVE_RECALL -> generator.generateWriting(number, isActiveRecall = true)
            TrainingMode.WORD_ASSEMBLY -> generator.generateWordAssembly(number)
            TrainingMode.LISTENING -> generator.generateListening(number, _selectedListeningVariation.value)
            TrainingMode.PRONUNCIATION -> generator.generatePronunciation(number, _selectedPronunciationActivity.value)
            TrainingMode.TELEPHONE -> generator.generateTelephone(_selectedTelephoneSubMode.value, _mobilePhonesOnly.value)
            TrainingMode.CONTEXTUAL -> generator.generateContextual(isListening = (System.currentTimeMillis() % 2L == 0L))
            TrainingMode.MISTAKES_REVIEW -> {
                val prog = snapshot[number]
                val weakestSkill = prog?.skills?.entries
                    ?.filter { it.value.attempts > 0 }
                    ?.minByOrNull { it.value.correct.toDouble() / it.value.attempts }
                    ?.key
                when (weakestSkill) {
                    LearningMode.LISTENING -> generator.generateListening(number, ListeningVariation.TYPED_DIGITS)
                    LearningMode.RECOGNITION -> generator.generateDragAndDrop(number)
                    else -> generator.generateWriting(number, isActiveRecall = true)
                }
            }
            else -> generator.generateDragAndDrop(number)
        }
    }

    private fun maybeAutoPlayAudio(exercise: Exercise) {
        val shouldAutoPlay = exercise is Exercise.Listening ||
            (exercise is Exercise.Telephone && exercise.isDictationListening) ||
            (exercise is Exercise.Pronunciation && exercise.activity == PronunciationActivity.SHADOWING)
        if (shouldAutoPlay) {
            playCurrentExerciseAudio(slow = false)
        }
    }

    fun playCurrentExerciseAudio(slow: Boolean = false) {
        val session = _activeSession.value ?: return
        val ex = session.currentExercise
        _activeSession.update { it?.copy(audioReplays = it.audioReplays + 1) }
        listeningTrainer.speak(
            frenchText = ex.audioFrenchText,
            speechRate = if (slow) 0.72f else 1.0f,
            varyVoice = preferences.value.varyVoices,
            onFinished = {
                _activeSession.update { current ->
                    if (current != null && current.currentExercise.id == ex.id && !current.listeningAudioFinished) {
                        // Start response timer AFTER audio finishes playing for Listening exercises
                        current.copy(
                            listeningAudioFinished = true,
                            questionTimerStartMs = System.currentTimeMillis()
                        )
                    } else {
                        current
                    }
                }
            }
        )
    }

    fun updateTextInput(value: String) {
        _activeSession.update { state ->
            if (state?.feedback != null) state else state?.copy(textInput = value)
        }
    }

    fun selectDragOrChoiceOption(option: String) {
        _activeSession.update { state ->
            if (state?.feedback != null) state else state?.copy(selectedOption = option)
        }
    }

    fun submitDragAndDropOption(option: String) {
        val state = _activeSession.value ?: return
        if (state.feedback != null) return
        _activeSession.update { it?.copy(selectedOption = option) }
        submitCurrentAnswer()
    }

    fun toggleAssemblyToken(token: WordToken) {
        _activeSession.update { state ->
            if (state == null || state.feedback != null) return@update state
            val current = state.assembledTokens.toMutableList()
            val idx = current.indexOfFirst { it.id == token.id }
            if (idx >= 0) current.removeAt(idx) else current.add(token)
            state.copy(assembledTokens = current)
        }
    }

    fun moveAssemblyToken(index: Int, delta: Int) {
        _activeSession.update { state ->
            if (state == null || state.feedback != null) return@update state
            val list = state.assembledTokens.toMutableList()
            val targetIdx = index + delta
            if (index !in list.indices || targetIdx !in list.indices) return@update state
            val item = list.removeAt(index)
            list.add(targetIdx, item)
            state.copy(assembledTokens = list)
        }
    }

    fun resetAssembly() {
        _activeSession.update { state ->
            if (state == null || state.feedback != null) return@update state
            state.copy(assembledTokens = emptyList())
        }
    }

    fun startSpeechRecallAsr() {
        _activeSession.update { it?.copy(asrStatusMessage = "Слушаю (fr-FR)...", asrTranscript = null) }
        pronunciationTrainer.startSpeechRecognition(
            onTranscript = { transcript ->
                _activeSession.update { state ->
                    state?.copy(
                        asrTranscript = transcript,
                        asrStatusMessage = "Распознано: «$transcript». Проверьте результат ниже."
                    )
                }
            },
            onServiceError = { msg ->
                _activeSession.update { state ->
                    state?.copy(asrStatusMessage = msg)
                }
            }
        )
    }

    fun startOrStopShadowRecording(onPermissionMissing: () -> Unit) {
        if (pronunciationTrainer.isRecordingAudio.value) {
            pronunciationTrainer.stopRecordingSample()
        } else {
            pronunciationTrainer.startRecordingSample { err ->
                _activeSession.update { it?.copy(asrStatusMessage = err) }
                if (err.contains("разрешение", ignoreCase = true)) {
                    onPermissionMissing()
                }
            }
        }
    }

    fun submitPronunciationSelfAssessment(
        succeeded: Boolean,
        outcomeType: SpeakingOutcomeType
    ) {
        val state = _activeSession.value ?: return
        if (state.feedback != null) return
        val ex = state.currentExercise
        val elapsed = (System.currentTimeMillis() - state.questionTimerStartMs).coerceAtLeast(100L)
        val userDisplay = state.asrTranscript ?: if (succeeded) "Самооценка: получилось" else "Нужна повторная практика"

        val feedback = ExerciseFeedback(
            isCorrect = succeeded,
            userAnswerDisplay = userDisplay,
            expectedFrench = ex.expectedFrench,
            ipa = ex.ipa,
            decompositionRu = ex.decompositionRu,
            mistakeAnalysis = if (succeeded) null else MistakeAnalysis(
                category = com.example.domain.MistakeCategory.SPELLING_OR_ORDER,
                explanationRu = "Прослушайте эталон и обратите внимание на ритм и связывание: ${ex.ipa}",
                decompositionRu = ex.decompositionRu
            ),
            responseTimeMs = elapsed,
            asrTranscript = state.asrTranscript,
            speakingOutcomeType = outcomeType
        )
        finalizeExerciseOutcome(state, feedback, confusedWith = null)
    }

    fun submitCurrentAnswer() {
        val state = _activeSession.value ?: return
        if (state.feedback != null) return
        val ex = state.currentExercise
        val now = System.currentTimeMillis()
        val elapsed = (now - state.questionTimerStartMs).coerceAtLeast(120L)

        val feedback: ExerciseFeedback
        var confusedWith: Int? = null

        when (ex) {
            is Exercise.DragAndDrop -> {
                val selected = state.selectedOption ?: return
                val isCorrect = selected == ex.expectedFrench
                val chosenNum = if (!isCorrect) {
                    AnswerNormalizer.findMatchingNumber(AnswerNormalizer.normalizeFrench(selected))
                } else null
                confusedWith = chosenNum
                val mistake = if (!isCorrect) {
                    if (chosenNum != null) ErrorClassifier.classifyNumericConfusion(ex.targetNumber, chosenNum)
                    else MistakeAnalysis(
                        com.example.domain.MistakeCategory.SPELLING_OR_ORDER,
                        "Верный вариант: «${ex.expectedFrench}».",
                        ex.decompositionRu
                    )
                } else null

                val metTarget = ex.targetTimeMs?.let { isCorrect && elapsed <= it }
                feedback = ExerciseFeedback(
                    isCorrect = isCorrect,
                    userAnswerDisplay = selected,
                    expectedFrench = ex.expectedFrench,
                    ipa = ex.ipa,
                    decompositionRu = ex.decompositionRu,
                    mistakeAnalysis = mistake,
                    responseTimeMs = elapsed,
                    metSpeedTarget = metTarget
                )
            }

            is Exercise.Writing -> {
                val input = state.textInput
                val validation = AnswerNormalizer.validateNumberAnswer(input, ex.targetNumber)
                confusedWith = if (!validation.isCorrect) validation.recognizedNumericMatch else null
                val mistake = if (!validation.isCorrect) {
                    ErrorClassifier.classifyWrittenError(input, ex.targetNumber, validation)
                } else null

                feedback = ExerciseFeedback(
                    isCorrect = validation.isCorrect,
                    userAnswerDisplay = input.ifBlank { "—" },
                    expectedFrench = ex.expectedFrench,
                    ipa = ex.ipa,
                    decompositionRu = ex.decompositionRu,
                    mistakeAnalysis = mistake,
                    responseTimeMs = elapsed
                )
            }

            is Exercise.WordAssembly -> {
                val joined = state.assembledTokens.joinToString(" ") { it.text }
                val validation = AnswerNormalizer.validateNumberAnswer(joined, ex.targetNumber)
                confusedWith = if (!validation.isCorrect) validation.recognizedNumericMatch else null
                val mistake = if (!validation.isCorrect) {
                    ErrorClassifier.classifyWrittenError(joined, ex.targetNumber, validation)
                } else null

                feedback = ExerciseFeedback(
                    isCorrect = validation.isCorrect,
                    userAnswerDisplay = joined.ifBlank { "—" },
                    expectedFrench = ex.expectedFrench,
                    ipa = ex.ipa,
                    decompositionRu = ex.decompositionRu,
                    mistakeAnalysis = mistake,
                    responseTimeMs = elapsed
                )
            }

            is Exercise.Listening -> {
                when (ex.variation) {
                    ListeningVariation.MULTIPLE_CHOICE_NUMBERS -> {
                        val chosenStr = state.selectedOption ?: return
                        val chosenInt = chosenStr.toIntOrNull()
                        val isCorrect = chosenInt == ex.targetNumber
                        confusedWith = if (!isCorrect) chosenInt else null
                        val mistake = if (!isCorrect && chosenInt != null) {
                            ErrorClassifier.classifyNumericConfusion(ex.targetNumber, chosenInt)
                        } else null
                        feedback = ExerciseFeedback(
                            isCorrect = isCorrect,
                            userAnswerDisplay = chosenStr,
                            expectedFrench = "${ex.targetNumber} · ${ex.expectedFrench}",
                            ipa = ex.ipa,
                            decompositionRu = ex.decompositionRu,
                            mistakeAnalysis = mistake,
                            responseTimeMs = elapsed
                        )
                    }
                    ListeningVariation.TYPED_DIGITS -> {
                        val input = state.textInput.trim()
                        val isCorrect = AnswerNormalizer.validateNumericDigits(input, ex.targetNumber)
                        val chosenInt = input.toIntOrNull()
                        confusedWith = if (!isCorrect) chosenInt else null
                        val mistake = if (!isCorrect) {
                            if (chosenInt != null) ErrorClassifier.classifyNumericConfusion(ex.targetNumber, chosenInt)
                            else MistakeAnalysis(
                                com.example.domain.MistakeCategory.UNANSWERED,
                                "Прозвучало число ${ex.targetNumber} (${ex.expectedFrench}).",
                                ex.decompositionRu
                            )
                        } else null
                        feedback = ExerciseFeedback(
                            isCorrect = isCorrect,
                            userAnswerDisplay = input.ifBlank { "—" },
                            expectedFrench = "${ex.targetNumber} · ${ex.expectedFrench}",
                            ipa = ex.ipa,
                            decompositionRu = ex.decompositionRu,
                            mistakeAnalysis = mistake,
                            responseTimeMs = elapsed
                        )
                    }
                }
            }

            is Exercise.Telephone -> {
                if (ex.isDictationListening) {
                    val isCorrect = AnswerNormalizer.validatePhoneDigits(state.textInput, ex.pairs)
                    val mistake = if (!isCorrect) ErrorClassifier.classifyPhoneError(state.textInput, ex.pairs) else null
                    feedback = ExerciseFeedback(
                        isCorrect = isCorrect,
                        userAnswerDisplay = state.textInput.ifBlank { "—" },
                        expectedFrench = "${ex.pairs.joinToString(" ")} · ${ex.expectedFrench}",
                        ipa = ex.ipa,
                        decompositionRu = ex.decompositionRu,
                        mistakeAnalysis = mistake,
                        responseTimeMs = elapsed
                    )
                } else {
                    val candidateText = if (state.assembledTokens.isNotEmpty()) {
                        state.assembledTokens.joinToString(" ") { it.text }
                    } else {
                        state.textInput
                    }
                    val validation = if (ex.pairs.size == 1) {
                        AnswerNormalizer.validatePhonePairAnswer(candidateText, ex.pairs.first())
                    } else {
                        AnswerNormalizer.validateFullPhoneWords(candidateText, ex.pairs)
                    }
                    val mistake = if (!validation.isCorrect) ErrorClassifier.classifyPhoneError(candidateText, ex.pairs) else null
                    feedback = ExerciseFeedback(
                        isCorrect = validation.isCorrect,
                        userAnswerDisplay = candidateText.ifBlank { "—" },
                        expectedFrench = ex.expectedFrench,
                        ipa = ex.ipa,
                        decompositionRu = ex.decompositionRu,
                        mistakeAnalysis = mistake,
                        responseTimeMs = elapsed
                    )
                }
            }

            is Exercise.Pronunciation -> return
        }

        finalizeExerciseOutcome(state, feedback, confusedWith)
    }

    private fun finalizeExerciseOutcome(
        state: ActiveSessionUiState,
        feedback: ExerciseFeedback,
        confusedWith: Int?
    ) {
        val ex = state.currentExercise
        val outcome = SessionQuestionOutcome(
            exercise = ex,
            userAnswer = feedback.userAnswerDisplay,
            isCorrect = feedback.isCorrect,
            responseTimeMs = feedback.responseTimeMs,
            mistakeAnalysis = feedback.mistakeAnalysis,
            audioReplays = state.audioReplays,
            speakingOutcomeType = feedback.speakingOutcomeType
        )

        if (!feedback.isCorrect && !ex.isAssessmentItem) {
            progressRepo.scheduler.scheduleSameSessionRetry(ex.targetNumber, ex.learningMode)
        }

        val newStreak = if (feedback.isCorrect) state.sessionStreak + 1 else 0
        _activeSession.update {
            it?.copy(
                feedback = feedback,
                outcomes = it.outcomes + outcome,
                sessionStreak = newStreak
            )
        }

        viewModelScope.launch {
            progressRepo.recordExerciseAttempt(
                number = ex.targetNumber,
                mode = ex.learningMode,
                isCorrect = feedback.isCorrect,
                responseTimeMs = feedback.responseTimeMs,
                confusedWith = confusedWith,
                speakingOutcomeType = feedback.speakingOutcomeType
            )
            prefsRepo.updateStreak(feedback.isCorrect)
        }
    }

    fun nextExerciseOrFinish() {
        val state = _activeSession.value ?: return
        listeningTrainer.stop()
        pronunciationTrainer.clearTempSample()

        val nextIndex = state.currentIndex + 1
        val reachedLimit = state.plannedCount != Int.MAX_VALUE && nextIndex >= state.plannedCount
        val queueExhausted = state.mode in listOf(
            TrainingMode.DIAGNOSTIC,
            TrainingMode.CONFUSION_DRILL,
            TrainingMode.ASSESSMENT,
            TrainingMode.DAILY_TRAINING
        ) && state.prebuiltQueue.isEmpty()

        if (reachedLimit || queueExhausted) {
            finishCurrentSession()
            return
        }

        viewModelScope.launch {
            val prefs = preferences.value
            val snapshot = progressRepo.getAllProgressSnapshot()
            val (nextEx, remainingQueue) = if (state.prebuiltQueue.isNotEmpty()) {
                state.prebuiltQueue.first() to state.prebuiltQueue.drop(1)
            } else {
                buildNextDynamicExercise(
                    mode = state.mode,
                    difficulty = state.difficulty,
                    prefs = prefs,
                    snapshot = snapshot,
                    lastNumber = state.currentExercise.targetNumber
                ) to emptyList()
            }

            // Adaptive speed adjustment: only tighten slightly if accuracy is high; never tighten on low accuracy!
            val updatedTargetMs = if (state.mode == TrainingMode.SPEED_FLUENCY && state.outcomes.size >= 4) {
                val acc = state.outcomes.count { it.isCorrect }.toDouble() / state.outcomes.size
                if (acc >= 0.85) (state.speedTargetMs - 150L).coerceAtLeast(1800L) else state.speedTargetMs
            } else state.speedTargetMs

            _activeSession.update {
                it?.copy(
                    currentIndex = nextIndex,
                    currentExercise = nextEx,
                    prebuiltQueue = remainingQueue,
                    feedback = null,
                    textInput = "",
                    assembledTokens = emptyList(),
                    selectedOption = null,
                    audioReplays = 0,
                    questionTimerStartMs = System.currentTimeMillis(),
                    listeningAudioFinished = nextEx !is Exercise.Listening &&
                        !(nextEx is Exercise.Telephone && nextEx.isDictationListening),
                    asrStatusMessage = null,
                    asrTranscript = null,
                    speedTargetMs = updatedTargetMs
                )
            }
            maybeAutoPlayAudio(nextEx)
        }
    }

    fun finishCurrentSession() {
        val state = _activeSession.value ?: return
        listeningTrainer.stop()
        pronunciationTrainer.release()

        val outcomes = state.outcomes
        if (state.mode == TrainingMode.DIAGNOSTIC) {
            val report = diagnosticEngine.evaluate(outcomes)
            _diagnosticReport.value = report
            _activeSession.value = null
            _currentScreen.value = AppScreen.DIAGNOSTIC_REPORT
            viewModelScope.launch {
                progressRepo.saveDiagnosticReport(report)
                prefsRepo.setDiagnosticDismissed(true)
                prefsRepo.setStageNumber(report.recommendedStage.stageNumber)
            }
            return
        }

        val total = outcomes.size
        val correct = outcomes.count { it.isCorrect }
        val incorrect = total - correct
        val acc = if (total == 0) 0 else ((correct * 100.0) / total).toInt()
        val avgTime = if (total == 0) 0L else outcomes.map { it.responseTimeMs }.average().toLong()
        val difficult = outcomes.filter { !it.isCorrect }

        val summary = SessionSummary(
            mode = state.mode,
            totalAnswered = total,
            correctCount = correct,
            incorrectCount = incorrect,
            accuracyPercent = acc,
            averageResponseMs = avgTime,
            difficultItems = difficult,
            isAssessment = state.mode == TrainingMode.ASSESSMENT
        )
        _sessionSummary.value = summary
        _activeSession.value = null
        _currentScreen.value = AppScreen.SESSION_SUMMARY

        viewModelScope.launch {
            progressRepo.saveCompletedSession(summary)
        }
    }

    override fun onCleared() {
        super.onCleared()
        listeningTrainer.release()
        pronunciationTrainer.release()
    }
}
