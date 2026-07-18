package com.openfilament.cfs.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.openfilament.cfs.domain.UserMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "openfilament_settings")

/**
 * Persists non-secret app settings (printer/SpoolmanSync URLs, Simple/Expert
 * mode) across restarts — FUNCTIONAL_DESCRIPTION.md §16 calls for "local
 * printer URL storage," which previously lived only in the in-memory
 * MainViewModel StateFlow and reset on every app launch. Nothing stored here
 * is a credential; if the app ever gains an auth token field, that belongs
 * in EncryptedSharedPreferences (see the pre-existing "printer_credentials"
 * backup-exclusion rules), not this DataStore.
 */
class SettingsStore(private val context: Context) {
    private object Keys {
        val PRINTER_URL = stringPreferencesKey("printer_url")
        val SPOOLMAN_SYNC_URL = stringPreferencesKey("spoolman_sync_url")
        val MODE = stringPreferencesKey("mode")
    }

    data class Persisted(val printerUrl: String?, val spoolmanSyncUrl: String?, val mode: UserMode?)

    val settings: Flow<Persisted> = context.settingsDataStore.data.map { prefs ->
        Persisted(
            printerUrl = prefs[Keys.PRINTER_URL],
            spoolmanSyncUrl = prefs[Keys.SPOOLMAN_SYNC_URL],
            mode = prefs[Keys.MODE]?.let { name -> runCatching { UserMode.valueOf(name) }.getOrNull() }
        )
    }

    suspend fun setPrinterUrl(url: String) {
        context.settingsDataStore.edit { it[Keys.PRINTER_URL] = url }
    }

    suspend fun setSpoolmanSyncUrl(url: String) {
        context.settingsDataStore.edit { it[Keys.SPOOLMAN_SYNC_URL] = url }
    }

    suspend fun setMode(mode: UserMode) {
        context.settingsDataStore.edit { it[Keys.MODE] = mode.name }
    }
}
