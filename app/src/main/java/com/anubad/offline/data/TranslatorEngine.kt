package com.anubad.offline.data

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateRemoteModel
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper over Google ML Kit on-device translation.
 * Translation runs entirely on the phone once both language packs are downloaded.
 */
class TranslatorEngine {
    private val modelManager = RemoteModelManager.getInstance()
    private var cachedPair: Pair<String, String>? = null
    private var translator: Translator? = null

    private fun conditions(wifiOnly: Boolean): DownloadConditions =
        if (wifiOnly) DownloadConditions.Builder().requireWifi().build()
        else DownloadConditions.Builder().build()

    private fun model(lang: String) = TranslateRemoteModel.Builder(lang).build()

    private fun clientFor(src: String, tgt: String): Translator {
        val pair = src to tgt
        val existing = translator
        if (existing != null && cachedPair == pair) return existing
        existing?.close()
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(src)
            .setTargetLanguage(tgt)
            .build()
        return Translation.getClient(options).also {
            translator = it
            cachedPair = pair
        }
    }

    suspend fun downloadedLanguages(): Set<String> =
        modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
            .map { it.language }
            .toSet()

    suspend fun download(lang: String, wifiOnly: Boolean) {
        modelManager.download(model(lang), conditions(wifiOnly)).await()
    }

    suspend fun delete(lang: String) {
        resetClient()
        modelManager.deleteDownloadedModel(model(lang)).await()
    }

    suspend fun translate(text: String, src: String, tgt: String, wifiOnly: Boolean): String {
        val client = clientFor(src, tgt)
        client.downloadModelIfNeeded(conditions(wifiOnly)).await()
        return client.translate(text).await()
    }

    fun resetClient() {
        translator?.close()
        translator = null
        cachedPair = null
    }

    fun close() = resetClient()
}
