package com.anubad.offline

import android.app.Application
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anubad.offline.data.HistoryDb
import com.anubad.offline.data.HistoryItem
import com.anubad.offline.data.Languages
import com.anubad.offline.data.Ocr
import com.anubad.offline.data.OcrScript
import com.anubad.offline.data.Prefs
import com.anubad.offline.data.Speaker
import com.anubad.offline.data.TranslatorEngine
import com.anubad.offline.data.formatCsvTime
import com.anubad.offline.data.localDayKey
import com.anubad.offline.data.localDayRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class MsgAction { INSTALL_TTS }

data class UiMessage(
    val text: String,
    val action: MsgAction? = null,
    val id: Long = System.nanoTime(),
)

class MainViewModel(private val app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val db = HistoryDb(app)
    private val engine = TranslatorEngine()
    private val speaker = Speaker(app)

    // ---------- Translate screen ----------
    var sourceText by mutableStateOf("")
        private set
    var targetText by mutableStateOf("")
        private set
    var srcLang by mutableStateOf(prefs.sourceLang)
        private set
    var tgtLang by mutableStateOf(prefs.targetLang)
        private set
    var translating by mutableStateOf(false)
        private set
    var currentId by mutableStateOf<Long?>(null)
        private set
    var currentStarred by mutableStateOf(false)
        private set

    // ---------- Language packs ----------
    var downloaded by mutableStateOf<Set<String>>(emptySet())
        private set
    var downloading by mutableStateOf<Set<String>>(emptySet())
        private set
    var pendingDownload by mutableStateOf<List<String>?>(null)
        private set

    // ---------- Camera scan ----------
    var ocrBusy by mutableStateOf(false)
        private set
    var ocrResult by mutableStateOf<String?>(null)
        private set
    var ocrWarning by mutableStateOf<String?>(null)
        private set

    // ---------- History ----------
    var historySearch by mutableStateOf("")
        private set
    var historyStarredOnly by mutableStateOf(false)
        private set
    var historyDay by mutableStateOf<Long?>(null) // UTC millis from the date picker
        private set
    var history by mutableStateOf<List<HistoryItem>>(emptyList())
        private set
    var historyDays by mutableStateOf<Set<String>>(emptySet())
        private set

    // ---------- Settings ----------
    var wifiOnly by mutableStateOf(prefs.wifiOnly)
        private set
    var ttsRate by mutableStateOf(prefs.ttsRate)
        private set
    var autoTranslateVoice by mutableStateOf(prefs.autoTranslateVoice)
        private set

    // ---------- Messages (snackbar) ----------
    var message by mutableStateOf<UiMessage?>(null)
        private set

    private var inputMethod = "typed"
    private var lastSavedKey: String? = null

    init {
        refreshDownloaded()
        loadHistory()
    }

    fun showMessage(text: String) {
        message = UiMessage(text)
    }

    fun consumeMessage() {
        message = null
    }

    // =====================================================================
    // Translate
    // =====================================================================

    fun onSourceChange(text: String) {
        sourceText = text
        if (text.isEmpty()) {
            targetText = ""
            inputMethod = "typed"
            currentId = null
        }
    }

    fun clearSource() = onSourceChange("")

    fun setSource(code: String) {
        if (code == tgtLang) {
            swap(); return
        }
        srcLang = code
        prefs.sourceLang = code
        targetText = ""
        currentId = null
    }

    fun setTarget(code: String) {
        if (code == srcLang) {
            swap(); return
        }
        tgtLang = code
        prefs.targetLang = code
        targetText = ""
        currentId = null
    }

    fun swap() {
        val s = srcLang
        srcLang = tgtLang
        tgtLang = s
        prefs.sourceLang = srcLang
        prefs.targetLang = tgtLang
        if (targetText.isNotBlank()) {
            val t = targetText
            targetText = sourceText
            sourceText = t
        } else {
            targetText = ""
        }
        currentId = null
    }

    fun translate() {
        val text = sourceText.trim()
        if (text.isEmpty() || translating) return
        if (srcLang == tgtLang) {
            targetText = sourceText; return
        }
        viewModelScope.launch {
            try {
                downloaded = engine.downloadedLanguages()
            } catch (ignored: Exception) {
            }
            val missing = listOf(srcLang, tgtLang).distinct().filter { it !in downloaded }
            if (missing.isNotEmpty()) pendingDownload = missing else runTranslate(text)
        }
    }

    fun cancelDownload() {
        pendingDownload = null
    }

    fun confirmDownload() {
        val langs = pendingDownload ?: return
        pendingDownload = null
        if (wifiOnly) showMessage("শুধু Wi-Fi চালু আছে: Wi-Fi পেলে নামানো শুরু হবে")
        viewModelScope.launch {
            downloading = downloading + langs
            try {
                for (l in langs) engine.download(l, wifiOnly)
                downloaded = engine.downloadedLanguages()
                runTranslate(sourceText.trim())
            } catch (e: Exception) {
                showMessage(friendlyError(e))
            } finally {
                downloading = downloading - langs.toSet()
            }
        }
    }

    private suspend fun runTranslate(text: String) {
        if (text.isEmpty()) return
        translating = true
        try {
            val result = engine.translate(text, srcLang, tgtLang, wifiOnly)
            targetText = result
            saveToHistory(text, result)
        } catch (e: Exception) {
            showMessage(friendlyError(e))
        } finally {
            translating = false
        }
    }

    private suspend fun saveToHistory(source: String, target: String) {
        val key = "$srcLang|$tgtLang|$source"
        if (key == lastSavedKey && currentId != null) return
        val item = HistoryItem(
            sourceText = source,
            targetText = target,
            sourceLang = srcLang,
            targetLang = tgtLang,
            inputMethod = inputMethod,
        )
        val id = withContext(Dispatchers.IO) { db.insert(item) }
        currentId = id
        currentStarred = false
        lastSavedKey = key
        loadHistory()
    }

    fun onVoiceResult(text: String) {
        sourceText = text
        inputMethod = "voice"
        if (autoTranslateVoice) translate()
    }

    fun receiveSharedText(text: String) {
        sourceText = text
        inputMethod = "typed"
        translate()
    }

    fun toggleCurrentStar() {
        val id = currentId ?: return
        val newValue = !currentStarred
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.setStarred(id, newValue) }
            currentStarred = newValue
            loadHistory()
        }
    }

    // =====================================================================
    // Speech output
    // =====================================================================

    fun speak(text: String, lang: String) {
        if (text.isBlank()) return
        when (speaker.speak(text, lang, ttsRate)) {
            Speaker.Result.OK -> Unit
            Speaker.Result.NOT_READY -> showMessage("শোনার ইঞ্জিন চালু হচ্ছে, একটু পরে আবার চাপুন")
            Speaker.Result.MISSING_VOICE -> message = UiMessage(
                "${Languages.displayName(lang)} ভয়েস এই ফোনে ইন্সটল নেই",
                MsgAction.INSTALL_TTS,
            )
        }
    }

    // =====================================================================
    // Camera scan
    // =====================================================================

    fun runOcr(uri: Uri) {
        viewModelScope.launch {
            ocrBusy = true
            val script = Languages.ocrScript(srcLang)
            try {
                val text = withContext(Dispatchers.IO) { Ocr.recognize(app, uri, script) }
                when {
                    text.isBlank() && script == OcrScript.UNSUPPORTED ->
                        showMessage("${Languages.displayName(srcLang)} লেখা ক্যামেরা এখনো পড়তে পারে না")
                    text.isBlank() -> showMessage("ছবিতে কোনো লেখা পাওয়া যায়নি")
                    else -> {
                        ocrWarning = if (script == OcrScript.UNSUPPORTED) {
                            "${Languages.displayName(srcLang)} লেখা ক্যামেরা পড়তে পারে না, তাই শুধু ইংরেজি (ল্যাটিন) অক্ষর ধরা পড়েছে।"
                        } else null
                        ocrResult = text
                    }
                }
            } catch (e: Exception) {
                showMessage("ছবিটা পড়া গেল না")
            } finally {
                ocrBusy = false
            }
        }
    }

    fun useOcrText(text: String) {
        ocrResult = null
        ocrWarning = null
        sourceText = text
        inputMethod = "camera"
        translate()
    }

    fun dismissOcr() {
        ocrResult = null
        ocrWarning = null
    }

    // =====================================================================
    // Language packs
    // =====================================================================

    fun refreshDownloaded() {
        viewModelScope.launch {
            try {
                downloaded = engine.downloadedLanguages()
            } catch (ignored: Exception) {
            }
        }
    }

    fun downloadLang(code: String) {
        if (code in downloading) return
        if (wifiOnly) showMessage("শুধু Wi-Fi চালু আছে: Wi-Fi পেলে নামানো শুরু হবে")
        viewModelScope.launch {
            downloading = downloading + code
            try {
                engine.download(code, wifiOnly)
                downloaded = engine.downloadedLanguages()
                showMessage("${Languages.displayName(code)} এখন অফলাইনে চলবে")
            } catch (e: Exception) {
                showMessage(friendlyError(e))
            } finally {
                downloading = downloading - code
            }
        }
    }

    fun deleteLang(code: String) {
        viewModelScope.launch {
            try {
                engine.delete(code)
                downloaded = engine.downloadedLanguages()
                showMessage("${Languages.displayName(code)} মুছে ফেলা হয়েছে")
            } catch (e: Exception) {
                showMessage(friendlyError(e))
            }
        }
    }

    // =====================================================================
    // History
    // =====================================================================

    fun loadHistory() {
        viewModelScope.launch {
            val range = historyDay?.let { localDayRange(it) }
            val search = historySearch
            val starred = historyStarredOnly
            val (items, days) = withContext(Dispatchers.IO) {
                db.query(search, starred, range?.first, range?.second) to
                    db.allTimestamps().map { localDayKey(it) }.toSet()
            }
            history = items
            historyDays = days
        }
    }

    fun searchHistory(q: String) {
        historySearch = q
        loadHistory()
    }

    fun toggleStarredOnly() {
        historyStarredOnly = !historyStarredOnly
        loadHistory()
    }

    fun setHistoryDayFilter(utcMillis: Long?) {
        historyDay = utcMillis
        loadHistory()
    }

    fun toggleStar(item: HistoryItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.setStarred(item.id, !item.starred) }
            if (item.id == currentId) currentStarred = !item.starred
            loadHistory()
        }
    }

    fun deleteItem(item: HistoryItem) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.delete(item.id) }
            if (item.id == currentId) currentId = null
            loadHistory()
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { db.clear() }
            currentId = null
            lastSavedKey = null
            loadHistory()
            showMessage("সব হিস্টরি মুছে ফেলা হয়েছে")
        }
    }

    fun openFromHistory(item: HistoryItem) {
        srcLang = item.sourceLang
        tgtLang = item.targetLang
        prefs.sourceLang = srcLang
        prefs.targetLang = tgtLang
        sourceText = item.sourceText
        targetText = item.targetText
        currentId = item.id
        currentStarred = item.starred
        lastSavedKey = "${item.sourceLang}|${item.targetLang}|${item.sourceText}"
    }

    fun exportCsv(uri: Uri) {
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    val items = db.query()
                    app.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { w ->
                        w.write("﻿") // BOM so Excel shows Bengali correctly
                        w.write("date,source_lang,target_lang,source_text,target_text,starred,input\n")
                        for (item in items) {
                            w.write(
                                listOf(
                                    formatCsvTime(item.createdAt), item.sourceLang, item.targetLang,
                                    item.sourceText, item.targetText, if (item.starred) "yes" else "no",
                                    item.inputMethod,
                                ).joinToString(",") { v -> csv(v) }
                            )
                            w.write("\n")
                        }
                    }
                    items.size
                }
                showMessage("$count টি অনুবাদ এক্সপোর্ট হয়েছে")
            } catch (e: Exception) {
                showMessage("এক্সপোর্ট করা গেল না")
            }
        }
    }

    private fun csv(s: String) = "\"" + s.replace("\"", "\"\"") + "\""

    // =====================================================================
    // Settings
    // =====================================================================

    fun updateWifiOnly(v: Boolean) {
        wifiOnly = v
        prefs.wifiOnly = v
    }

    fun updateTtsRate(v: Float) {
        ttsRate = v
        prefs.ttsRate = v
    }

    fun updateAutoTranslateVoice(v: Boolean) {
        autoTranslateVoice = v
        prefs.autoTranslateVoice = v
    }

    // =====================================================================

    private fun isOnline(): Boolean {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun friendlyError(e: Exception): String =
        if (!isOnline()) "ইন্টারনেট নেই। ভাষা প্যাক একবার নামাতে ইন্টারনেট লাগবে।"
        else "সমস্যা হয়েছে: ${e.localizedMessage ?: "অজানা ত্রুটি"}"

    override fun onCleared() {
        engine.close()
        speaker.shutdown()
        db.close()
    }
}
