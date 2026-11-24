package com.anubad.offline.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anubad.offline.MainViewModel
import com.anubad.offline.data.Languages

@Composable
fun TranslateScreen(
    vm: MainViewModel,
    onPickSource: () -> Unit,
    onPickTarget: () -> Unit,
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val focus = LocalFocusManager.current
    var cameraUri by rememberSaveable { mutableStateOf<Uri?>(null) }

    val voiceLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == Activity.RESULT_OK) {
            res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let { vm.onVoiceResult(it) }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { ok -> if (ok) cameraUri?.let { vm.runOcr(it) } }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { vm.runOcr(it) } }

    fun startVoice() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Languages.speechTag(vm.srcLang))
            putExtra(RecognizerIntent.EXTRA_PROMPT, "${Languages.displayName(vm.srcLang)} বলুন…")
        }
        try {
            voiceLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            vm.showMessage("এই ফোনে ভয়েস ইনপুট নেই (Google অ্যাপ লাগবে)")
        }
    }

    fun startCamera() {
        val uri = newImageUri(context)
        cameraUri = uri
        try {
            cameraLauncher.launch(uri)
        } catch (e: ActivityNotFoundException) {
            vm.showMessage("ক্যামেরা অ্যাপ পাওয়া যায়নি")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ---- Language pair ----
        Row(verticalAlignment = Alignment.CenterVertically) {
            LangButton(
                name = Languages.displayName(vm.srcLang),
                ready = vm.srcLang in vm.downloaded,
                modifier = Modifier.weight(1f),
                onClick = onPickSource,
            )
            IconButton(onClick = vm::swap) {
                Icon(Icons.Filled.SwapHoriz, contentDescription = "ভাষা অদলবদল")
            }
            LangButton(
                name = Languages.displayName(vm.tgtLang),
                ready = vm.tgtLang in vm.downloaded,
                modifier = Modifier.weight(1f),
                onClick = onPickTarget,
            )
        }

        // ---- Source input ----
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                OutlinedTextField(
                    value = vm.sourceText,
                    onValueChange = vm::onSourceChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 140.dp),
                    placeholder = { Text("লিখুন, বলুন বা ক্যামেরায় স্ক্যান করুন…") },
                    trailingIcon = {
                        if (vm.sourceText.isNotEmpty()) {
                            IconButton(onClick = vm::clearSource) {
                                Icon(Icons.Filled.Close, contentDescription = "মুছুন")
                            }
                        }
                    },
                )
                Spacer(Modifier.size(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { startVoice() }) {
                        Icon(Icons.Filled.Mic, contentDescription = "বলে লিখুন")
                    }
                    IconButton(onClick = { startCamera() }) {
                        Icon(Icons.Filled.CameraAlt, contentDescription = "ক্যামেরায় স্ক্যান")
                    }
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = "ছবি থেকে স্ক্যান")
                    }
                    IconButton(
                        onClick = { vm.speak(vm.sourceText, vm.srcLang) },
                        enabled = vm.sourceText.isNotBlank(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "মূল লেখা শুনুন")
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            focus.clearFocus()
                            vm.translate()
                        },
                        enabled = vm.sourceText.isNotBlank() && !vm.translating,
                    ) {
                        if (vm.translating) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("অনুবাদ")
                        }
                    }
                }
            }
        }

        // ---- Status ----
        if (vm.downloading.isNotEmpty()) {
            StatusRow("ভাষা প্যাক নামানো হচ্ছে: " + vm.downloading.joinToString { Languages.displayName(it) })
        }
        if (vm.ocrBusy) StatusRow("ছবি থেকে লেখা পড়া হচ্ছে…")

        // ---- Output ----
        if (vm.targetText.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        Languages.displayName(vm.tgtLang),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.size(6.dp))
                    SelectionContainer {
                        Text(
                            vm.targetText,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.size(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { vm.speak(vm.targetText, vm.tgtLang) }) {
                            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "অনুবাদ শুনুন")
                        }
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(vm.targetText))
                            vm.showMessage("কপি হয়েছে")
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "কপি")
                        }
                        IconButton(onClick = { shareText(context, vm.targetText) }) {
                            Icon(Icons.Filled.Share, contentDescription = "শেয়ার")
                        }
                        if (vm.currentId != null) {
                            IconButton(onClick = vm::toggleCurrentStar) {
                                Icon(
                                    if (vm.currentStarred) Icons.Filled.Star else Icons.Filled.StarBorder,
                                    contentDescription = "তারকা দিন",
                                )
                            }
                        }
                    }
                    Text(
                        "Translated by Google · ML Kit, ফোনেই অনুবাদ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LangButton(name: String, ready: Boolean, modifier: Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier) {
        Text(name, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
        if (!ready) {
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Outlined.CloudDownload,
                contentDescription = "এখনো নামানো হয়নি",
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun StatusRow(text: String) {
    Column(Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.size(4.dp))
        LinearProgressIndicator(Modifier.fillMaxWidth())
    }
}
