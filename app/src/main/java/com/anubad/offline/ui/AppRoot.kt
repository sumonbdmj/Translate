package com.anubad.offline.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.anubad.offline.MainViewModel
import com.anubad.offline.MsgAction

enum class Tab(val label: String, val icon: ImageVector) {
    TRANSLATE("অনুবাদ", Icons.Filled.Translate),
    HISTORY("হিস্টরি", Icons.Filled.History),
    SETTINGS("সেটিংস", Icons.Filled.Settings),
}

enum class Overlay { NONE, MODELS, PICK_SOURCE, PICK_TARGET }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(vm: MainViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.TRANSLATE) }
    var overlay by rememberSaveable { mutableStateOf(Overlay.NONE) }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val msg = vm.message
    LaunchedEffect(msg) {
        if (msg != null) {
            val result = snackbar.showSnackbar(
                message = msg.text,
                actionLabel = if (msg.action == MsgAction.INSTALL_TTS) "ইন্সটল" else null,
                duration = if (msg.action != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && msg.action == MsgAction.INSTALL_TTS) {
                openTtsInstall(context)
            }
            vm.consumeMessage()
        }
    }

    BackHandler(enabled = overlay != Overlay.NONE) { overlay = Overlay.NONE }
    BackHandler(enabled = overlay == Overlay.NONE && tab != Tab.TRANSLATE) { tab = Tab.TRANSLATE }

    val title = when (overlay) {
        Overlay.MODELS -> "অফলাইন ভাষা প্যাক"
        Overlay.PICK_SOURCE -> "কোন ভাষা থেকে"
        Overlay.PICK_TARGET -> "কোন ভাষায়"
        Overlay.NONE -> when (tab) {
            Tab.TRANSLATE -> "অফলাইন অনুবাদ"
            Tab.HISTORY -> "হিস্টরি"
            Tab.SETTINGS -> "সেটিংস"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    if (overlay != Overlay.NONE) {
                        IconButton(onClick = { overlay = Overlay.NONE }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "ফিরে যান")
                        }
                    }
                },
                actions = {
                    if (overlay == Overlay.NONE && tab == Tab.TRANSLATE) {
                        IconButton(onClick = { overlay = Overlay.MODELS }) {
                            Icon(Icons.Filled.Download, contentDescription = "ভাষা প্যাক")
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (overlay == Overlay.NONE) {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            when (overlay) {
                Overlay.MODELS -> ModelsScreen(vm)
                Overlay.PICK_SOURCE -> LanguagePicker(vm.srcLang, vm.downloaded) {
                    vm.setSource(it); overlay = Overlay.NONE
                }
                Overlay.PICK_TARGET -> LanguagePicker(vm.tgtLang, vm.downloaded) {
                    vm.setTarget(it); overlay = Overlay.NONE
                }
                Overlay.NONE -> when (tab) {
                    Tab.TRANSLATE -> TranslateScreen(
                        vm = vm,
                        onPickSource = { overlay = Overlay.PICK_SOURCE },
                        onPickTarget = { overlay = Overlay.PICK_TARGET },
                    )
                    Tab.HISTORY -> HistoryScreen(vm) {
                        vm.openFromHistory(it); tab = Tab.TRANSLATE
                    }
                    Tab.SETTINGS -> SettingsScreen(vm)
                }
            }
        }
    }

    vm.pendingDownload?.let { langs ->
        DownloadPromptDialog(langs, onConfirm = vm::confirmDownload, onDismiss = vm::cancelDownload)
    }
    vm.ocrResult?.let { text ->
        OcrResultDialog(
            initial = text,
            warning = vm.ocrWarning,
            onUse = vm::useOcrText,
            onDismiss = vm::dismissOcr,
        )
    }
}
