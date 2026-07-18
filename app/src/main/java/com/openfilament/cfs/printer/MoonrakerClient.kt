package com.openfilament.cfs.printer

import com.openfilament.cfs.domain.CfsSlot
import com.openfilament.cfs.domain.PrinterSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Read-mostly Moonraker client. Per FUNCTIONAL_DESCRIPTION.md §11, this app
 * treats the printer/CFS as authoritative and itself as a thin client:
 * relay-on-runout, filament consumption and thermal watchdogs are configured
 * here, never implemented as app-side control loops (§12-§14).
 *
 * The `box` object schema and M8200 command family are drawn from
 * community-documented K2-series reverse engineering, CONFIRMED ON K2 PLUS —
 * K2 Pro is expected but not yet bench-verified (H-004). BOX_LOAD_MATERIAL
 * is deliberately never used here — see §11.
 */
class MoonrakerClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()
) {
    private val jsonMedia = "application/json".toMediaType()

    suspend fun probe(baseUrl: String): Result<PrinterSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = baseUrl.trimEnd('/')
            val request = Request.Builder()
                .url("$clean/printer/objects/query?print_stats&extruder&heater_bed&display_status&box")
                .get().build()
            client.newCall(request).execute().use { response ->
                require(response.isSuccessful) { "Moonraker returned HTTP ${response.code}" }
                val json = JSONObject(requireNotNull(response.body).string())
                val status = json.optJSONObject("result")?.optJSONObject("status") ?: JSONObject()
                val stats = status.optJSONObject("print_stats") ?: JSONObject()
                val extruder = status.optJSONObject("extruder") ?: JSONObject()
                val bed = status.optJSONObject("heater_bed") ?: JSONObject()
                val displayStatus = status.optJSONObject("display_status") ?: JSONObject()
                val box = status.optJSONObject("box")
                PrinterSnapshot(
                    online = true,
                    hostname = clean,
                    state = stats.optString("state", "Ready").replaceFirstChar { it.uppercase() },
                    // display_status.progress is a 0.0-1.0 fraction per Moonraker's documented API.
                    progress = (displayStatus.optDouble("progress", 0.0) * 100).toInt().coerceIn(0, 100),
                    nozzleC = extruder.optDouble("temperature").takeUnless { it.isNaN() },
                    bedC = bed.optDouble("temperature").takeUnless { it.isNaN() },
                    autoRefillEnabled = box?.optBoolean("auto_refill", false) ?: false,
                    slots = box?.let { parseSlots(it) } ?: emptyList()
                )
            }
        }
    }

    /** Parses the community-documented `box` object into per-slot [CfsSlot] records. */
    private fun parseSlots(box: JSONObject): List<CfsSlot> {
        val slots = mutableListOf<CfsSlot>()
        for (i in 1..4) {
            val key = "T$i"
            val unit = box.optJSONObject(key) ?: continue
            val rawColor = unit.optString("color_value", "").removePrefix("0").ifBlank { null }
            val rawMaterial = unit.optString("material_type", "").removePrefix("1").ifBlank { null }
            slots += CfsSlot(
                index = i,
                materialCode = rawMaterial,
                colorHex = rawColor?.let { "#$it" },
                remainingLengthM = unit.optInt("remain_len", -1).takeIf { it >= 0 },
                temperatureC = unit.optDouble("temperature").takeUnless { it.isNaN() },
                state = unit.optString("state", "Unknown")
            )
        }
        return slots
    }

    /**
     * Toggles the CFS's own native auto-refill (relay-on-runout) feature.
     * This does NOT implement matching logic in the app — it flips the
     * printer's own `BOX_ENABLE_AUTO_REFILL` behaviour (§12).
     */
    suspend fun setAutoRefill(baseUrl: String, enabled: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        runGcodeScript(baseUrl, "BOX_ENABLE_AUTO_REFILL ENABLE=${if (enabled) 1 else 0}")
    }

    /**
     * Issues a load/unload command via the documented M8200 macro family.
     * `BOX_LOAD_MATERIAL` is intentionally never sent — it omits a required
     * pre-operation step on K2 firmware (FUNCTIONAL_DESCRIPTION.md §11).
     */
    suspend fun sendBoxCommand(baseUrl: String, m8200Args: String): Result<Unit> = withContext(Dispatchers.IO) {
        runGcodeScript(baseUrl, "M8200 $m8200Args")
    }

    /** Sets chamber target temperature via the documented M141 command. S0 = off. */
    suspend fun setChamberTarget(baseUrl: String, celsius: Int): Result<Unit> = withContext(Dispatchers.IO) {
        runGcodeScript(baseUrl, "M141 S$celsius")
    }

    private suspend fun runGcodeScript(baseUrl: String, script: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val clean = baseUrl.trimEnd('/')
            val body = JSONObject().put("script", script).toString().toRequestBody(jsonMedia)
            val request = Request.Builder().url("$clean/printer/gcode/script").post(body).build()
            client.newCall(request).execute().use { response ->
                require(response.isSuccessful) { "Moonraker returned HTTP ${response.code} for: $script" }
            }
        }
    }
}
