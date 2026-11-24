package com.anubad.offline.data

import com.google.mlkit.nl.translate.TranslateLanguage
import java.util.Locale

/** Which ML Kit text-recognition model can read a language's script. */
enum class OcrScript { LATIN, CHINESE, DEVANAGARI, JAPANESE, KOREAN, UNSUPPORTED }

object Languages {

    /** All languages ML Kit can translate offline, sorted by display name. */
    val all: List<String> by lazy {
        TranslateLanguage.getAllLanguages().sortedBy { displayName(it).lowercase() }
    }

    /** e.g. "Bengali (বাংলা)" on an English phone, "বাংলা" on a Bengali phone. */
    fun displayName(code: String): String {
        val loc = Locale.forLanguageTag(code)
        val device = Locale.getDefault()
        val local = loc.getDisplayLanguage(device).cap(device)
        val native = loc.getDisplayLanguage(loc).cap(loc)
        return if (local.equals(native, ignoreCase = true)) local else "$local ($native)"
    }

    private fun String.cap(l: Locale) =
        replaceFirstChar { if (it.isLowerCase()) it.titlecase(l) else it.toString() }

    /** Locale tag for Google voice input. Regional defaults that suit Bangladesh users. */
    private val speechTags = mapOf(
        "bn" to "bn-BD",
        "en" to "en-US",
        "zh" to "zh-CN",
        "pt" to "pt-BR",
        "hi" to "hi-IN",
        "ur" to "ur-PK",
        "ar" to "ar-SA",
    )

    fun speechTag(code: String): String = speechTags[code] ?: code

    /** Scripts ML Kit text recognition cannot read yet (Bengali, Arabic, Cyrillic, ...). */
    private val unsupportedOcr = setOf(
        "ar", "be", "bg", "bn", "el", "fa", "gu", "he", "ka",
        "kn", "mk", "ru", "ta", "te", "th", "uk", "ur",
    )

    fun ocrScript(code: String): OcrScript = when (code) {
        "zh" -> OcrScript.CHINESE
        "hi", "mr" -> OcrScript.DEVANAGARI
        "ja" -> OcrScript.JAPANESE
        "ko" -> OcrScript.KOREAN
        in unsupportedOcr -> OcrScript.UNSUPPORTED
        else -> OcrScript.LATIN
    }
}
