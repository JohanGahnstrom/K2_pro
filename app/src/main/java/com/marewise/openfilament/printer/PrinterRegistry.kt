package com.marewise.openfilament.printer

import android.content.Context
import com.marewise.openfilament.domain.ManagedPrinter
import com.marewise.openfilament.domain.PrinterEndpoint
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PrinterRegistry(context: Context) {
    private val prefs = context.getSharedPreferences("managed_printers", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun load(): List<ManagedPrinter> = runCatching {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        json.decodeFromString<List<StoredPrinter>>(raw).map { it.toDomain() }
    }.getOrDefault(emptyList())

    fun save(printers: List<ManagedPrinter>) {
        prefs.edit().putString(KEY, json.encodeToString(printers.map { StoredPrinter.fromDomain(it) })).apply()
    }

    @Serializable
    private data class StoredPrinter(
        val id: String,
        val name: String,
        val host: String,
        val moonrakerPort: Int,
        val crealityWsPort: Int,
        val enabled: Boolean,
        val location: String,
        val notes: String
    ) {
        fun toDomain() = ManagedPrinter(id, name, PrinterEndpoint(host, moonrakerPort, crealityWsPort), enabled, location, notes)
        companion object {
            fun fromDomain(p: ManagedPrinter) = StoredPrinter(p.id, p.name, p.endpoint.host, p.endpoint.moonrakerPort, p.endpoint.crealityWsPort, p.enabled, p.location, p.notes)
        }
    }

    private companion object { const val KEY = "printers_v1" }
}
