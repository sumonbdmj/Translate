package com.anubad.offline.ui

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anubad.offline.MainViewModel
import java.util.Locale

@Composable
fun SettingsScreen(vm: MainViewModel) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { vm.exportCsv(it) } }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Section("অনুবাদ")
        SwitchRow(
            title = "শুধু Wi-Fi-তে ভাষা প্যাক নামাও",
            subtitle = "মোবাইল ডেটা বাঁচাতে",
            checked = vm.wifiOnly,
            onChange = vm::updateWifiOnly,
        )
        SwitchRow(
            title = "বলার পর নিজে থেকে অনুবাদ",
            subtitle = "ভয়েস ইনপুট শেষ হলেই অনুবাদ শুরু হবে",
            checked = vm.autoTranslateVoice,
            onChange = vm::updateAutoTranslateVoice,
        )

        Section("শোনা (TTS)")
        ListItem(
            headlineContent = { Text("পড়ার গতি: ${String.format(Locale.US, "%.2f", vm.ttsRate)}x") },
            supportingContent = {
                Slider(
                    value = vm.ttsRate,
                    onValueChange = vm::updateTtsRate,
                    valueRange = 0.5f..2f,
                    steps = 5,
                )
            },
        )
        ActionRow(
            title = "অফলাইন ভয়েস ডেটা ইন্সটল করুন",
            subtitle = "ইন্টারনেট ছাড়া অনুবাদ শুনতে চাইলে এটা লাগবে",
        ) { openTtsInstall(context) }

        Section("হিস্টরি")
        ActionRow(
            title = "CSV হিসেবে এক্সপোর্ট",
            subtitle = "Excel বা Google Sheets-এ খোলা যাবে",
        ) { exportLauncher.launch("anubad-history.csv") }
        ActionRow(
            title = "সব হিস্টরি মুছুন",
            subtitle = null,
            titleColor = MaterialTheme.colorScheme.error,
        ) { confirmClear = true }

        Section("সম্পর্কে")
        ListItem(
            headlineContent = { Text("অনুবাদ: Google ML Kit") },
            supportingContent = {
                Text("Translated by Google. অনুবাদ পুরোপুরি ফোনেই হয়। ভয়েস ইনপুটের জন্য Google-এর স্পিচ সার্ভিস ইন্টারনেট ব্যবহার করে।")
            },
        )
        ListItem(
            headlineContent = { Text("কোনো বিজ্ঞাপন নেই") },
            supportingContent = { Text("কোনো তথ্য সংগ্রহ বা শেয়ার করা হয় না") },
        )
        ListItem(
            headlineContent = { Text("সংস্করণ") },
            supportingContent = { Text(appVersion(context)) },
        )
    }

    if (confirmClear) {
        ConfirmDialog(
            title = "সব হিস্টরি মুছবেন?",
            body = "সব অনুবাদ চিরতরে মুছে যাবে। এটা আর ফেরানো যাবে না।",
            confirmLabel = "মুছে ফেলুন",
            onConfirm = vm::clearHistory,
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun Section(title: String) {
    HorizontalDivider()
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else null,
        trailingContent = { Switch(checked = checked, onCheckedChange = onChange) },
        modifier = Modifier.clickable { onChange(!checked) },
    )
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String?,
    titleColor: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(title, color = titleColor) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle) }
        } else null,
        modifier = Modifier.clickable(onClick = onClick),
    )
}

private fun appVersion(context: Context): String =
    try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
    } catch (e: Exception) {
        "1.0"
    }
