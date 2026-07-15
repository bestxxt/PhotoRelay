package com.example.photorelay.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.photorelay.domain.WorkScheduler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val autoSyncEnabled by viewModel.autoSyncEnabled.collectAsStateWithLifecycle()
    val syncIntervalHours by viewModel.syncIntervalHours.collectAsStateWithLifecycle()
    val syncStartDate by viewModel.syncStartDate.collectAsStateWithLifecycle()
    val saveLocation by viewModel.saveLocation.collectAsStateWithLifecycle()
    val keepDays by viewModel.keepDays.collectAsStateWithLifecycle()
    val appLogs by viewModel.appLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Auto Sync Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Auto Sync", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Run background sync on Wi-Fi",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoSyncEnabled,
                    onCheckedChange = { 
                        viewModel.setAutoSyncEnabled(it)
                        WorkScheduler.updateSyncWork(context, it, syncIntervalHours)
                    }
                )
            }

            // Sync Interval
            var intervalText by remember(syncIntervalHours) { mutableStateOf(syncIntervalHours.toString()) }
            OutlinedTextField(
                value = intervalText,
                onValueChange = { 
                    intervalText = it.filter { char -> char.isDigit() }
                    intervalText.toIntOrNull()?.let { hours ->
                        viewModel.setSyncIntervalHours(hours)
                        if (autoSyncEnabled) {
                            WorkScheduler.updateSyncWork(context, true, hours)
                        }
                    }
                },
                label = { Text("Sync Interval (Hours)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Sync Start Date
            var startDateText by remember(syncStartDate) { mutableStateOf(syncStartDate) }
            OutlinedTextField(
                value = startDateText,
                onValueChange = {},
                label = { Text("Sync Start Date (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Text("📅")
                    }
                }
            )

            // Keep Days
            var keepDaysText by remember(keepDays) { mutableStateOf(keepDays.toString()) }
            OutlinedTextField(
                value = keepDaysText,
                onValueChange = { 
                    keepDaysText = it.filter { char -> char.isDigit() }
                    keepDaysText.toIntOrNull()?.let { days ->
                        viewModel.setKeepDays(days)
                    }
                },
                label = { Text("Keep Locally (Days)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Save Location
            var locationText by remember(saveLocation) { mutableStateOf(saveLocation) }
            OutlinedTextField(
                value = locationText,
                onValueChange = { 
                    locationText = it
                    viewModel.setSaveLocation(it)
                },
                label = { Text("Save Location (Pictures/...)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.weight(1f))

            var showClearConfirm by remember { mutableStateOf(false) }
            var showLogsDialog by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showLogsDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("View Sync Logs")
            }

            Button(
                onClick = { showClearConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Clear Sync Records")
            }

            if (showClearConfirm) {
                AlertDialog(
                    onDismissRequest = { showClearConfirm = false },
                    title = { Text("Clear Sync Records?") },
                    text = { Text("This will delete all local sync records, meaning previously synced photos may be synced again. Are you sure?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.clearSyncRecords()
                                showClearConfirm = false
                            }
                        ) {
                            Text("Clear")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showClearConfirm = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            if (showLogsDialog) {
                AlertDialog(
                    onDismissRequest = { showLogsDialog = false },
                    title = { Text("Background Sync Logs") },
                    text = {
                        androidx.compose.foundation.lazy.LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 400.dp)
                        ) {
                            if (appLogs.isEmpty()) {
                                item { Text("No logs available yet.") }
                            } else {
                                items(appLogs) { log ->
                                    Text(
                                        text = log,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLogsDialog = false }) {
                            Text("Close")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { viewModel.clearAppLogs() }
                        ) {
                            Text("Clear Logs", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
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
                            viewModel.setSyncStartDate(formattedDate)
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
