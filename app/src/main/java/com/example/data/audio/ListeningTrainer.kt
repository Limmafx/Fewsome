package com.example.data.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

/**
 * Manages metropolitan French (fr-FR) Text-to-Speech playback with:
 * - Normal (1.0f) and slow (0.72f) rates
 * - Automatic discovery of multiple fr-FR voices and gradual variability
 * - Utterance completion callback so listening response time starts AFTER audio finishes
 * - Graceful non-blocking degradation when TTS is unavailable
 */
class ListeningTrainer(context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tts: TextToSpeech? = null
    private var frenchVoices: List<Voice> = emptyList()
    private var voiceRotationIndex = 0
    private var completionCallback: (() -> Unit)? = null

    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _voiceNames = MutableStateFlow<List<String>>(emptyList())
    val voiceNames: StateFlow<List<String>> = _voiceNames.asStateFlow()

    private val _statusMessageRu = MutableStateFlow<String?>(null)
    val statusMessageRu: StateFlow<String?> = _statusMessageRu.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: return@TextToSpeech
                    val langResult = engine.setLanguage(Locale.FRANCE)
                    if (langResult == TextToSpeech.LANG_MISSING_DATA ||
                        langResult == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        _isReady.value = false
                        _statusMessageRu.value = "Французский голос (fr-FR) не установлен в системе TTS."
                    } else {
                        frenchVoices = engine.voices
                            ?.filter {
                                it.locale.language.equals("fr", ignoreCase = true) &&
                                    !it.isNetworkConnectionRequired
                            }
                            ?.sortedBy { it.name }
                            ?: emptyList()
                        if (frenchVoices.isEmpty()) {
                            frenchVoices = engine.voices
                                ?.filter { it.locale.language.equals("fr", ignoreCase = true) }
                                ?.sortedBy { it.name }
                                ?: emptyList()
                        }
                        _voiceNames.value = frenchVoices.map { it.name }
                        _isReady.value = true
                        _statusMessageRu.value = null
                    }
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            mainHandler.post { _isSpeaking.value = true }
                        }

                        override fun onDone(utteranceId: String?) {
                            mainHandler.post {
                                _isSpeaking.value = false
                                completionCallback?.invoke()
                                completionCallback = null
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            mainHandler.post {
                                _isSpeaking.value = false
                                completionCallback?.invoke()
                                completionCallback = null
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            mainHandler.post {
                                _isSpeaking.value = false
                                completionCallback?.invoke()
                                completionCallback = null
                            }
                        }
                    })
                } else {
                    _isReady.value = false
                    _statusMessageRu.value = "Синтезатор речи недоступен на этом устройстве."
                }
            }
        } catch (_: Exception) {
            _isReady.value = false
            _statusMessageRu.value = "Не удалось инициализировать озвучку."
        }
    }

    fun speak(
        frenchText: String,
        speechRate: Float = 1.0f,
        varyVoice: Boolean = true,
        preferredVoiceIndex: Int? = null,
        onFinished: () -> Unit = {}
    ) {
        val engine = tts
        if (!_isReady.value || engine == null) {
            _isSpeaking.value = false
            onFinished()
            return
        }
        try {
            if (frenchVoices.isNotEmpty()) {
                val targetVoice = when {
                    preferredVoiceIndex != null && preferredVoiceIndex in frenchVoices.indices ->
                        frenchVoices[preferredVoiceIndex]
                    varyVoice && frenchVoices.size > 1 -> {
                        val v = frenchVoices[voiceRotationIndex % frenchVoices.size]
                        voiceRotationIndex++
                        v
                    }
                    else -> frenchVoices.first()
                }
                engine.voice = targetVoice
            }
            engine.setSpeechRate(speechRate.coerceIn(0.5f, 1.5f))
            completionCallback = onFinished
            val utteranceId = UUID.randomUUID().toString()
            val result = engine.speak(frenchText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result != TextToSpeech.SUCCESS) {
                _isSpeaking.value = false
                completionCallback = null
                onFinished()
            }
        } catch (_: Exception) {
            _isSpeaking.value = false
            completionCallback = null
            onFinished()
        }
    }

    fun stop() {
        runCatching { tts?.stop() }
        _isSpeaking.value = false
        completionCallback = null
    }

    fun release() {
        runCatching {
            tts?.stop()
            tts?.shutdown()
        }
        tts = null
        _isReady.value = false
    }
}
