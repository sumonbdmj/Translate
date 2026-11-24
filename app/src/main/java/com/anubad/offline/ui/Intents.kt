package com.anubad.offline.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.core.content.FileProvider
import java.io.File

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(send, null))
}

/** Opens the system screen that downloads offline text-to-speech voices. */
fun openTtsInstall(context: Context) {
    val install = Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(install)
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(
                Intent("com.android.settings.TTS_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (ignored: ActivityNotFoundException) {
        }
    }
}

/** A fresh file the camera app can write the photo into. */
fun newImageUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "scan_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}
