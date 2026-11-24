package com.anubad.offline.data

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Android's built-in text-to-speech. Offline only if the phone has that voice installed. */
class Speaker(context: Context) : TextToSpeech.OnInitListener {

    enum class Result { OK, NOT_READY, MISSING_VOICE }

    private var ready = false
    private val tts = TextToSpeech(context.applicationContext, this)

    override fun onInit(status: Int) {
        ready = status == TextToSpeech.SUCCESS
    }

    fun speak(text: String, lang: String, rate: Float): Result {
        if (!ready) return Result.NOT_READY
        val r = tts.setLanguage(Locale.forLanguageTag(lang))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            return Result.MISSING_VOICE
        }
        tts.setSpeechRate(rate)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "anubad")
        return Result.OK
    }

    fun stop() {
        tts.stop()
    }

    fun shutdown() {
        tts.shutdown()
    }
}
