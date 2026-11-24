package com.anubad.offline.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.anubad.offline.MainViewModel
import com.anubad.offline.data.HistoryItem
import com.anubad.offline.data.formatPickedDay
import com.anubad.offline.data.formatTime
import com.anubad.offline.data.utcDayKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(vm: MainViewModel, onOpen: (HistoryItem) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.loadHistory() }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = vm.historySearch,
            onValueChange = vm::searchHistory,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (vm.historySearch.isNotEmpty()) {
                    IconButton(onClick = { vm.searchHistory("") }) {
                        Icon(Icons.Filled.Close, contentDescription = "মুছুন")
                    }
                }
            },
            placeholder = { Text("হিস্টরিতে খুঁজুন") },
        )
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChip(
                selected = vm.historyStarredOnly,
                onClick = vm::toggleStarredOnly,
                label = { Text("তারকা দেওয়া") },
                leadingIcon = { Icon(Icons.Filled.Star, contentDescription = null, Modifier.size(18.dp)) },
            )
            val day = vm.historyDay
            FilterChip(
                selected = day != null,
                onClick = { if (day != null) vm.setHistoryDayFilter(null) else showDatePicker = true },
                label = { Text(day?.let { formatPickedDay(it) } ?: "তারিখ") },
                leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, Modifier.size(18.dp)) },
                trailingIcon = if (day != null) {
                    { Icon(Icons.Filled.Close, contentDescription = "তারিখ বাদ দিন", Modifier.size(18.dp)) }
                } else null,
            )
        }

        val filtering = vm.historySearch.isNotBlank() || vm.historyStarredOnly || vm.historyDay != null
        if (vm.history.isEmpty()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (filtering) "কিছু মেলেনি" else "এখনো কোনো অনুবাদ নেই।\nযা অনুবাদ করবেন, এখানে জমা থাকবে।",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(vm.history, key = { it.id }) { item ->
                    HistoryRow(
                        item = item,
                        onClick = { onOpen(item) },
                        onStar = { vm.toggleStar(item) },
                        onDelete = { vm.deleteItem(item) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showDatePicker) {
        val days = vm.historyDays
        val selectable = remember(days) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    utcDayKey(utcTimeMillis) in days
            }
        }
        val state = rememberDatePickerState(selectableDates = selectable)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.setHistoryDayFilter(state.selectedDateMillis)
                        showDatePicker = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) { Text("দেখাও") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("বাতিল") }
            },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        overlineContent = {
            Text(
                "${item.sourceLang.uppercase()} → ${item.targetLang.uppercase()} · ${formatTime(item.createdAt)}",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        headlineContent = {
            Text(item.sourceText, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
        supportingContent = {
            Text(
                item.targetText,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.primary,
            )
        },
        trailingContent = {
            Row {
                IconButton(onClick = onStar) {
                    Icon(
                        if (item.starred) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = "তারকা",
                        tint = if (item.starred) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = "মুছুন")
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
