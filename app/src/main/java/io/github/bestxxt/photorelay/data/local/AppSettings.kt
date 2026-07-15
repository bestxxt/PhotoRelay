package io.github.bestxxt.photorelay.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppSettings(private val context: Context) {

    companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val SYNC_RECORDS = stringPreferencesKey("sync_records_json")
        val KEEP_DAYS = androidx.datastore.preferences.core.intPreferencesKey("keep_days")
        
        val AUTO_SYNC_ENABLED = androidx.datastore.preferences.core.booleanPreferencesKey("auto_sync_enabled")
        val SYNC_INTERVAL_HOURS = androidx.datastore.preferences.core.intPreferencesKey("sync_interval_hours")
        val SYNC_START_DATE = stringPreferencesKey("sync_start_date")
        val SAVE_LOCATION = stringPreferencesKey("save_location")
        val APP_LOGS = stringPreferencesKey("app_logs")
        val LAST_EMAIL = stringPreferencesKey("last_email")
        
        val HOME_DATE_SELECTION = stringPreferencesKey("home_date_selection")
        val HOME_SIZE_SELECTION = androidx.datastore.preferences.core.intPreferencesKey("home_size_selection")
    }
    
    private val gson = Gson()

    val serverUrl: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[SERVER_URL]
    }

    val accessToken: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[ACCESS_TOKEN]
    }
    
    val lastEmail: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[LAST_EMAIL]
    }

    val syncRecords: Flow<Map<String, SyncRecord>> = context.dataStore.data.map { preferences ->
        val json = preferences[SYNC_RECORDS]
        if (json.isNullOrEmpty()) {
            emptyMap()
        } else {
            val type = object : TypeToken<Map<String, SyncRecord>>() {}.type
            try {
                gson.fromJson(json, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
    
    val keepDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[KEEP_DAYS] ?: 30
    }
    
    val autoSyncEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_SYNC_ENABLED] ?: false
    }
    
    val syncIntervalHours: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SYNC_INTERVAL_HOURS] ?: 24
    }
    
    val syncStartDate: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SYNC_START_DATE] ?: "2024-01-01"
    }
    
    val saveLocation: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SAVE_LOCATION] ?: "PhotoRelay"
    }

    val homeDateSelection: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[HOME_DATE_SELECTION] ?: "7d"
    }
    
    val homeSizeSelection: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[HOME_SIZE_SELECTION] ?: 10000
    }

    val appLogs: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val json = preferences[APP_LOGS]
        if (json.isNullOrEmpty()) {
            emptyList()
        } else {
            val type = object : TypeToken<List<String>>() {}.type
            try {
                gson.fromJson(json, type) ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    suspend fun saveServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL] = url
        }
    }

    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[ACCESS_TOKEN] = token
        }
    }
    
    suspend fun saveLastEmail(email: String) {
        context.dataStore.edit { preferences ->
            preferences[LAST_EMAIL] = email
        }
    }

    suspend fun saveKeepDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_DAYS] = days
        }
    }
    
    suspend fun saveAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_SYNC_ENABLED] = enabled
        }
    }
    
    suspend fun saveSyncIntervalHours(hours: Int) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_INTERVAL_HOURS] = hours
        }
    }
    
    suspend fun saveSyncStartDate(date: String) {
        context.dataStore.edit { preferences ->
            preferences[SYNC_START_DATE] = date
        }
    }
    
    suspend fun saveLocation(location: String) {
        context.dataStore.edit { preferences ->
            preferences[SAVE_LOCATION] = location
        }
    }

    suspend fun saveHomeDateSelection(selection: String) {
        context.dataStore.edit { preferences ->
            preferences[HOME_DATE_SELECTION] = selection
        }
    }
    
    suspend fun saveHomeSizeSelection(size: Int) {
        context.dataStore.edit { preferences ->
            preferences[HOME_SIZE_SELECTION] = size
        }
    }

    suspend fun saveSyncRecord(record: SyncRecord) {
        context.dataStore.edit { preferences ->
            val json = preferences[SYNC_RECORDS]
            val type = object : TypeToken<Map<String, SyncRecord>>() {}.type
            val currentMap: MutableMap<String, SyncRecord> = try {
                if (json != null) gson.fromJson(json, type) ?: mutableMapOf() else mutableMapOf()
            } catch (e: Exception) {
                mutableMapOf()
            }
            currentMap[record.assetId] = record
            preferences[SYNC_RECORDS] = gson.toJson(currentMap)
        }
    }
    
    suspend fun markAsCleaned(assetIds: List<String>) {
        context.dataStore.edit { preferences ->
            val json = preferences[SYNC_RECORDS]
            if (json != null) {
                val type = object : TypeToken<Map<String, SyncRecord>>() {}.type
                val currentMap: MutableMap<String, SyncRecord> = try {
                    gson.fromJson(json, type) ?: mutableMapOf()
                } catch (e: Exception) {
                    mutableMapOf()
                }
                
                var changed = false
                for (id in assetIds) {
                    currentMap[id]?.let { record ->
                        currentMap[id] = record.copy(isCleaned = true)
                        changed = true
                    }
                }
                
                if (changed) {
                    preferences[SYNC_RECORDS] = gson.toJson(currentMap)
                }
            }
        }
    }
    
    suspend fun clearAuthData() {
        context.dataStore.edit { preferences ->
            preferences.remove(ACCESS_TOKEN)
        }
    }

    suspend fun clearSyncRecords() {
        context.dataStore.edit { preferences ->
            preferences.remove(SYNC_RECORDS)
        }
    }

    suspend fun addAppLog(message: String) {
        context.dataStore.edit { preferences ->
            val json = preferences[APP_LOGS]
            val type = object : TypeToken<List<String>>() {}.type
            val currentList: MutableList<String> = try {
                if (json != null) gson.fromJson(json, type) ?: mutableListOf() else mutableListOf()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            // Add new message to the top
            currentList.add(0, message)
            
            // Keep only the latest 50 logs
            if (currentList.size > 50) {
                currentList.subList(50, currentList.size).clear()
            }
            
            preferences[APP_LOGS] = gson.toJson(currentList)
        }
    }

    suspend fun clearAppLogs() {
        context.dataStore.edit { preferences ->
            preferences.remove(APP_LOGS)
        }
    }
}
