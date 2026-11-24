package com.anubad.offline.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.anubad.offline.MainViewModel
import com.anubad.offline.data.Languages

/** S02: pick a language, with search. */
@Composable
fun LanguagePicker(selected: String, downloaded: Set<String>, onPick: (String) -> Unit) {
    var query by remember { mutableStateOf("") }
    val list = remember(query) {
        val q = query.trim()
        Languages.all.filter {
            q.isEmpty() || Languages.displayName(it).contains(q, ignoreCase = true) ||
                it.equals(q, ignoreCase = true)
        }
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            placeholder = { Text("ভাষা খুঁজুন") },
        )
        if (list.isEmpty()) {
            Text("কোনো ভাষা মেলেনি", modifier = Modifier.padding(16.dp))
        }
        LazyColumn {
            items(list, key = { it }) { code ->
                ListItem(
                    headlineContent = { Text(Languages.displayName(code)) },
                    supportingContent = if (code in downloaded) {
                        { Text("অফলাইনে প্রস্তুত", color = MaterialTheme.colorScheme.primary) }
                    } else null,
                    trailingContent = if (code == selected) {
                        { Icon(Icons.Filled.Check, contentDescription = "বাছাই করা") }
                    } else null,
                    modifier = Modifier.clickable { onPick(code) },
                )
            }
        }
    }
}

/** S03: download or delete offline language packs. */
@Composable
fun ModelsScreen(vm: MainViewModel) {
    LaunchedEffect(Unit) { vm.refreshDownloaded() }
    val sorted = remember(vm.downloaded) {
        Languages.all.sortedBy { if (it in vm.downloaded) 0 else 1 }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp)) {
                Text(
                    "যে ভাষা নামানো থাকবে, সেটা ইন্টারনেট ছাড়াই অনুবাদ হবে। প্রতিটা প্রায় ৩০ MB।",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (vm.wifiOnly) {
                    Text(
                        "শুধু Wi-Fi-তে নামানো হবে (সেটিংস থেকে বদলাতে পারবেন)।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            HorizontalDivider()
        }
        items(sorted, key = { it }) { code ->
            val isDownloaded = code in vm.downloaded
            val isDownloading = code in vm.downloading
            ListItem(
                headlineContent = { Text(Languages.displayName(code)) },
                supportingContent = {
                    Text(
                        when {
                            isDownloading -> "নামানো হচ্ছে…"
                            isDownloaded -> "অফলাইনে প্রস্তুত"
                            else -> "নামানো হয়নি"
                        }
                    )
                },
                leadingContent = if (isDownloaded) {
                    { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
                } else null,
                trailingContent = {
                    when {
                        isDownloading -> CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        isDownloaded -> IconButton(onClick = { vm.deleteLang(code) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "মুছুন")
                        }
                        else -> IconButton(onClick = { vm.downloadLang(code) }) {
                            Icon(Icons.Filled.Download, contentDescription = "নামান")
                        }
                    }
                },
            )
        }
    }
}
