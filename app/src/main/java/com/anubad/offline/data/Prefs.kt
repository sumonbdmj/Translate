package com.anubad.offline.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var sourceLang: String
        get() = sp.getString("src", "en") ?: "en"
        set(v) = sp.edit().putString("src", v).apply()

    var targetLang: String
        get() = sp.getString("tgt", "bn") ?: "bn"
        set(v) = sp.edit().putString("tgt", v).apply()

    var wifiOnly: Boolean
        get() = sp.getBoolean("wifi_only", false)
        set(v) = sp.edit().putBoolean("wifi_only", v).apply()

    var ttsRate: Float
        get() = sp.getFloat("tts_rate", 1f)
        set(v) = sp.edit().putFloat("tts_rate", v).apply()

    var autoTranslateVoice: Boolean
        get() = sp.getBoolean("auto_voice", true)
        set(v) = sp.edit().putBoolean("auto_voice", v).apply()
}
