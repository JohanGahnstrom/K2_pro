package com.openfilament.cfs.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.openfilament.cfs.domain.TaggedSpool
import com.openfilament.cfs.domain.UserMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

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
        val SPOOLS_JSON = stringPreferencesKey("tagged_spools_json")
        val GUIDE_NUDGE_DISMISSED = booleanPreferencesKey("guide_nudge_dismissed")
    }

    data class Persisted(
        val printerUrl: String?,
        val spoolmanSyncUrl: String?,
        val mode: UserMode?,
        val spools: List<TaggedSpool>,
        val guideNudgeDismissed: Boolean,
    )

    val settings: Flow<Persisted> = context.settingsDataStore.data.map { prefs ->
        Persisted(
            printerUrl = prefs[Keys.PRINTER_URL],
            spoolmanSyncUrl = prefs[Keys.SPOOLMAN_SYNC_URL],
            mode = prefs[Keys.MODE]?.let { name -> runCatching { UserMode.valueOf(name) }.getOrNull() },
            spools = prefs[Keys.SPOOLS_JSON]?.let { decodeSpools(it) } ?: emptyList(),
            guideNudgeDismissed = prefs[Keys.GUIDE_NUDGE_DISMISSED] ?: false,
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

    suspend fun setSpools(spools: List<TaggedSpool>) {
        context.settingsDataStore.edit { it[Keys.SPOOLS_JSON] = encodeSpools(spools) }
    }

    suspend fun setGuideNudgeDismissed() {
        context.settingsDataStore.edit { it[Keys.GUIDE_NUDGE_DISMISSED] = true }
    }
}

/**
 * Only the identifying fields are persisted (product id, colour name,
 * weight, serial, timestamp) — not the full FilamentProduct/FilamentColor
 * structure. Catalogue.products remains the single source of truth for
 * product data, so a stored record just looks its product/colour back up
 * by id/name at load time; if the catalogue ever drops a product a spool
 * was tagged with, that record is silently skipped rather than crashing
 * or showing stale/duplicated product data.
 */
internal fun encodeSpools(spools: List<TaggedSpool>): String {
    val array = JSONArray()
    spools.forEach { spool ->
        array.put(
            JSONObject()
                .put("productId", spool.product.id)
                .put("colorName", spool.color.name)
                .put("weightG", spool.weightG)
                .put("serial", spool.serial)
                .put("taggedAtMillis", spool.taggedAt.time)
        )
    }
    return array.toString()
}

internal fun decodeSpools(json: String): List<TaggedSpool> {
    val array = runCatching { JSONArray(json) }.getOrElse { return emptyList() }
    return (0 until array.length()).mapNotNull { i ->
        runCatching {
            val obj = array.getJSONObject(i)
            val product = com.openfilament.cfs.data.Catalogue.products
                .firstOrNull { it.id == obj.getString("productId") } ?: return@runCatching null
            val color = product.colors.firstOrNull { it.name == obj.getString("colorName") } ?: return@runCatching null
            TaggedSpool(
                product = product,
                color = color,
                weightG = obj.getInt("weightG"),
                serial = obj.getString("serial"),
                taggedAt = Date(obj.getLong("taggedAtMillis")),
            )
        }.getOrNull()
    }
}
