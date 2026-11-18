package com.example.data.audio

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * Handles optional microphone-based pronunciation practice:
 * 1. Speech Recognition (fr-FR) when system SpeechRecognizer is available.
 * 2. Local temporary audio recording & playback for Shadowing comparison
 *    and fallback when SpeechRecognizer is unavailable.
 *
 * Privacy & Policy compliance:
 * - Never persists raw audio beyond the active temporary practice cache file.
 * - Never penalizes the user for recognition-service or hardware errors.
 */
class PronunciationTrainer(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private val tempAudioFile: File = File(context.cacheDir, "shadow_practice_temp.m4a")

    private val _isListeningAsr = MutableStateFlow(false)
    val isListeningAsr: StateFlow<Boolean> = _isListeningAsr.asStateFlow()

    private val _isRecordingAudio = MutableStateFlow(false)
    val isRecordingAudio: StateFlow<Boolean> = _isRecordingAudio.asStateFlow()

    private val _isPlayingRecording = MutableStateFlow(false)
    val isPlayingRecording: StateFlow<Boolean> = _isPlayingRecording.asStateFlow()

    private val _hasRecordedSample = MutableStateFlow(false)
    val hasRecordedSample: StateFlow<Boolean> = _hasRecordedSample.asStateFlow()

    fun isSpeechRecognitionSupported(): Boolean {
        return runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)
    }

    fun startSpeechRecognition(
        onTranscript: (String) -> Unit,
        onServiceError: (String) -> Unit
    ) {
        if (!isSpeechRecognitionSupported()) {
            onServiceError("Распознавание речи недоступно на устройстве. Используйте запись для самопроверки.")
            return
        }
        stopSpeechRecognition()
        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer = recognizer
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _isListeningAsr.value = true
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    _isListeningAsr.value = false
                }

                override fun onError(error: Int) {
                    _isListeningAsr.value = false
                    val msg = when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Нет разрешения на доступ к микрофону."
                        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                            "Сервис распознавания требует подключения к сети. Это не ошибка ответа."
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                            "Речь не распознана. Попробуйте ещё раз или используйте ручную запись."
                        else -> "Сервис распознавания речи временно недоступен (код $error)."
                    }
                    onServiceError(msg)
                }

                override fun onResults(results: Bundle?) {
                    _isListeningAsr.value = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull().orEmpty()
                    if (best.isNotBlank()) {
                        onTranscript(best)
                    } else {
                        onServiceError("Не удалось распознать фразу. Попробуйте снова.")
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            _isListeningAsr.value = true
            recognizer.startListening(intent)
        } catch (_: SecurityException) {
            _isListeningAsr.value = false
            onServiceError("Нет разрешения на микрофон.")
        } catch (_: Exception) {
            _isListeningAsr.value = false
            onServiceError("Не удалось запустить распознавание речи.")
        }
    }

    fun stopSpeechRecognition() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
        _isListeningAsr.value = false
    }

    fun startRecordingSample(onError: (String) -> Unit) {
        stopPlayback()
        stopRecordingSample()
        try {
            if (tempAudioFile.exists()) tempAudioFile.delete()
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(tempAudioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            _isRecordingAudio.value = true
            _hasRecordedSample.value = false
        } catch (_: SecurityException) {
            _isRecordingAudio.value = false
            onError("Разрешение на микрофон не предоставлено.")
        } catch (_: Exception) {
            _isRecordingAudio.value = false
            onError("Запись аудио недоступна на этом устройстве.")
        }
    }

    fun stopRecordingSample() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            if (tempAudioFile.exists() && tempAudioFile.length() > 0) {
                _hasRecordedSample.value = true
            }
        } catch (_: Exception) {
            _hasRecordedSample.value = false
        } finally {
            mediaRecorder = null
            _isRecordingAudio.value = false
        }
    }

    fun playRecordedSample(onError: (String) -> Unit = {}) {
        if (!tempAudioFile.exists() || tempAudioFile.length() == 0L) {
            onError("Запись отсутствует.")
            return
        }
        stopPlayback()
        try {
            val player = MediaPlayer().apply {
                setDataSource(tempAudioFile.absolutePath)
                setOnCompletionListener {
                    _isPlayingRecording.value = false
                    it.release()
                    mediaPlayer = null
                }
                prepare()
                start()
            }
            mediaPlayer = player
            _isPlayingRecording.value = true
        } catch (_: Exception) {
            _isPlayingRecording.value = false
            onError("Не удалось воспроизвести запись.")
        }
    }

    fun stopPlayback() {
        runCatching {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        }
        mediaPlayer = null
        _isPlayingRecording.value = false
    }

    fun clearTempSample() {
        stopPlayback()
        stopRecordingSample()
        if (tempAudioFile.exists()) tempAudioFile.delete()
        _hasRecordedSample.value = false
    }

    fun release() {
        stopSpeechRecognition()
        clearTempSample()
    }
}
