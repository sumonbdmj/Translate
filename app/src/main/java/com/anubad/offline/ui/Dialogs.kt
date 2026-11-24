package com.anubad.offline.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anubad.offline.data.Languages

@Composable
fun DownloadPromptDialog(langs: List<String>, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val names = langs.joinToString(" ও ") { Languages.displayName(it) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ভাষা প্যাক নামাতে হবে") },
        text = {
            Text("অফলাইনে অনুবাদের জন্য $names একবার নামাতে হবে (প্রতিটা প্রায় ৩০ MB)। এরপর ইন্টারনেট ছাড়াই চলবে।")
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("নামাও") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

@Composable
fun OcrResultDialog(
    initial: String,
    warning: String?,
    onUse: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember(initial) { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("স্ক্যান করা লেখা") },
        text = {
            Column {
                if (warning != null) {
                    Text(warning, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.size(8.dp))
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 120.dp, max = 320.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text("দরকার হলে লেখাটা ঠিক করে নিন", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            TextButton(onClick = { onUse(text) }, enabled = text.isNotBlank()) { Text("অনুবাদ করো") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("বাতিল") } },
    )
}
