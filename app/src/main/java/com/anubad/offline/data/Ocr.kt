package com.anubad.offline.data

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/** On-device text recognition (ML Kit Text Recognition v2). */
object Ocr {
    suspend fun recognize(context: Context, uri: Uri, script: OcrScript): String {
        val options: TextRecognizerOptionsInterface = when (script) {
            OcrScript.CHINESE -> ChineseTextRecognizerOptions.Builder().build()
            OcrScript.DEVANAGARI -> DevanagariTextRecognizerOptions.Builder().build()
            OcrScript.JAPANESE -> JapaneseTextRecognizerOptions.Builder().build()
            OcrScript.KOREAN -> KoreanTextRecognizerOptions.Builder().build()
            OcrScript.LATIN, OcrScript.UNSUPPORTED -> TextRecognizerOptions.DEFAULT_OPTIONS
        }
        val recognizer = TextRecognition.getClient(options)
        return try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image).await().text
        } finally {
            recognizer.close()
        }
    }
}
