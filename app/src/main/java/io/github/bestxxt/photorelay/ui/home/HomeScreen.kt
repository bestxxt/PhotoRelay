package io.github.bestxxt.photorelay.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SyncTab {
    UNSYNCED, SYNCED, CLEANED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel, onNavigateToSettings: () -> Unit, onLogout: () -> Unit) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncRecords by viewModel.syncRecords.collectAsStateWithLifecycle()
    val savedKeepDays by viewModel.keepDays.collectAsStateWithLifecycle()
    val homeDateSelection by viewModel.homeDateSelection.collectAsStateWithLifecycle()
    val homeSizeSelection by viewModel.homeSizeSelection.collectAsStateWithLifecycle()
    
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(SyncTab.UNSYNCED) }
    
    var currentTakenAfter by remember { mutableStateOf<String?>(null) }
    var currentSize by remember { mutableStateOf(10000) }

    LaunchedEffect(uiState, homeDateSelection, homeSizeSelection) {
        val state = uiState
        if (state is HomeUiState.Idle) {
            val size = homeSizeSelection
            var targetDate: String? = null
            
            if (homeDateSelection.startsWith("date:")) {
                targetDate = homeDateSelection.removePrefix("date:")
            } else {
                val days = homeDateSelection.removeSuffix("d").toIntOrNull() ?: 7
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val today = System.currentTimeMillis()
                val dayMillis = 24L * 60 * 60 * 1000
                targetDate = sdf.format(Date(today - days * dayMillis))
            }
            viewModel.fetchAssets(targetDate, size)
        } else if (state is HomeUiState.Success) {
            currentTakenAfter = state.takenAfter
            currentSize = state.fetchSize
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PhotoRelay Sync",
                style = MaterialTheme.typography.headlineMedium
            )
            Row {
                IconButton(onClick = { 
                    viewModel.fetchAssets(currentTakenAfter, currentSize) 
                }) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = onNavigateToSettings) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = System.currentTimeMillis()
            val dayMillis = 24L * 60 * 60 * 1000

            val presetDays = listOf(7, 30, 60)
            
            presetDays.forEachIndexed { index, days ->
                val targetDate = sdf.format(Date(today - days * dayMillis))
                val isSelected = homeDateSelection == "${days}d"
                SegmentedButton(
                    selected = isSelected,
                    onClick = { 
                        viewModel.saveHomeSelections("${days}d", currentSize)
                        viewModel.fetchAssets(targetDate, currentSize) 
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = presetDays.size + 1)
                ) {
                    Text("${days}d")
                }
            }
            
            // "Select Date" Segment
            val isCustomDate = homeDateSelection.startsWith("date:")
            SegmentedButton(
                selected = isCustomDate,
                onClick = { showDatePicker = true },
                shape = SegmentedButtonDefaults.itemShape(index = presetDays.size, count = presetDays.size + 1)
            ) {
                val customDate = if (isCustomDate) homeDateSelection.removePrefix("date:").takeLast(5) else "Date"
                Text(customDate) 
            }
        }

        // Size selection row
        val sizes = listOf(50, 100, 500, 10000)
        
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            sizes.forEachIndexed { index, size ->
                SegmentedButton(
                    selected = homeSizeSelection == size,
                    onClick = { 
                        viewModel.saveHomeSelections(homeDateSelection, size)
                        viewModel.fetchAssets(currentTakenAfter, size) 
                    },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = sizes.size)
                ) {
                    Text(if (size == 10000) "Max" else size.toString())
                }
            }
        }
        
        if (uiState is HomeUiState.Success) {
            val state = uiState as HomeUiState.Success
        }

        when (val state = uiState) {
            is HomeUiState.Idle -> {
                Text("Please select a start date and fetch assets.")
            }
            is HomeUiState.Loading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            is HomeUiState.Error -> {
                Text("Error: ${state.message}", color = MaterialTheme.colorScheme.error)
            }
            is HomeUiState.Success -> {
                val unsyncedAssets = state.assets.filter { !syncRecords.containsKey(it.id) }
                val syncedAssets = state.assets.filter { syncRecords[it.id]?.isCleaned == false }
                val cleanedAssets = state.assets.filter { syncRecords[it.id]?.isCleaned == true }
                
                // Tabs
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    SegmentedButton(
                        selected = selectedTab == SyncTab.UNSYNCED,
                        onClick = { selectedTab = SyncTab.UNSYNCED },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3)
                    ) {
                        Text("未同步 (${unsyncedAssets.size})")
                    }
                    SegmentedButton(
                        selected = selectedTab == SyncTab.SYNCED,
                        onClick = { selectedTab = SyncTab.SYNCED },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3)
                    ) {
                        Text("已同步 (${syncedAssets.size})")
                    }
                    SegmentedButton(
                        selected = selectedTab == SyncTab.CLEANED,
                        onClick = { selectedTab = SyncTab.CLEANED },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3)
                    ) {
                        Text("已清理 (${cleanedAssets.size})")
                    }
                }
                
                val currentList = when (selectedTab) {
                    SyncTab.UNSYNCED -> unsyncedAssets
                    SyncTab.SYNCED -> syncedAssets
                    SyncTab.CLEANED -> cleanedAssets
                }

                Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (currentList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Empty",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "暂无照片",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            items(currentList) { asset ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val baseUrl = if (state.serverUrl.endsWith("/")) state.serverUrl.dropLast(1) else state.serverUrl
                                        val thumbnailUrl = "$baseUrl/api/assets/${asset.id}/thumbnail?size=thumbnail"

                                        AsyncImage(
                                            model = thumbnailUrl,
                                            contentDescription = "Thumbnail",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = asset.originalFileName,
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                            Text(
                                                text = "Created: ${asset.fileCreatedAt.take(10)}",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                text = "ID: ${asset.id.take(8)}...",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (selectedTab == SyncTab.UNSYNCED && unsyncedAssets.isNotEmpty()) {
                        SyncProgressButton(
                            isSyncing = state.isSyncing,
                            syncProgress = state.syncProgress,
                            onSyncAll = { viewModel.syncAll() }
                        )
                    }
                }
            }
        }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val dateMillis = datePickerState.selectedDateMillis
                    if (dateMillis != null) {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val formattedDate = sdf.format(Date(dateMillis))
                        val currentSize = (uiState as? HomeUiState.Success)?.fetchSize ?: 50
                        viewModel.saveHomeSelections("date:$formattedDate", currentSize)
                        viewModel.fetchAssets(formattedDate, currentSize)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    }
}

@Composable
fun SyncProgressButton(
    isSyncing: Boolean,
    syncProgress: String,
    onSyncAll: () -> Unit
) {
    var fraction by remember { mutableFloatStateOf(0f) }
    var progressText by remember { mutableStateOf("Sync All Un-synced") }

    LaunchedEffect(isSyncing, syncProgress) {
        if (isSyncing) {
            val parts = syncProgress.split("/")
            if (parts.size == 2) {
                val current = parts[0].replace(Regex("[^0-9]"), "").toFloatOrNull() ?: 0f
                val total = parts[1].replace(Regex("[^0-9]"), "").toFloatOrNull() ?: 1f
                fraction = if (total > 0f) current / total else 0f
                progressText = "${current.toInt()}/${total.toInt()}"
            } else {
                progressText = syncProgress
            }
        } else {
            fraction = 0f
            progressText = "Sync All Un-synced"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = !isSyncing, onClick = onSyncAll),
        contentAlignment = Alignment.CenterStart
    ) {
        if (isSyncing) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
        Text(
            text = progressText,
            modifier = Modifier.align(Alignment.Center),
            color = if (isSyncing) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
