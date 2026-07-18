package com.marewise.openfilament.printer

import com.marewise.openfilament.domain.GCodeMetadata
import java.io.File
import kotlin.math.roundToLong

/** Best-effort parser for common OrcaSlicer, PrusaSlicer and Cura comments. */
object GCodeMetadataParser {
    fun parse(file: File): GCodeMetadata {
        var slicer: String? = null
        var estimatedSeconds: Long? = null
        var filamentMm: Double? = null
        var filamentGrams: Double? = null
        var layerHeight: Double? = null
        var nozzle: Double? = null
        var material: String? = null
        var printer: String? = null
        var thumbnail = false

        file.bufferedReader().useLines { lines ->
            lines.take(20_000).forEach { raw ->
                val line = raw.trim()
                if (slicer == null) slicer = detectSlicer(line)
                if (line.startsWith("; estimated printing time", true) || line.startsWith(";TIME:", true)) {
                    estimatedSeconds = parseTime(line)
                }
                when {
                    line.startsWith("; filament used [mm]", true) -> filamentMm = valueAfterEquals(line)
                    line.startsWith("; filament used [g]", true) -> filamentGrams = valueAfterEquals(line)
                    line.startsWith(";Filament used:", true) -> filamentMm = parseLengthToMm(line.substringAfter(':'))
                    line.startsWith("; layer_height", true) || line.startsWith(";LAYER_HEIGHT:", true) -> layerHeight = number(line)
                    line.startsWith("; nozzle_diameter", true) -> nozzle = number(line)
                    line.startsWith("; filament_type", true) || line.startsWith("; filament_settings_id", true) -> material = textValue(line)
                    line.startsWith("; printer_model", true) || line.startsWith("; printer_settings_id", true) -> printer = textValue(line)
                    line.startsWith("; thumbnail begin", true) || line.startsWith("; thumbnail_JPG begin", true) -> thumbnail = true
                }
            }
        }
        return GCodeMetadata(slicer, estimatedSeconds, filamentMm, filamentGrams, layerHeight, nozzle, material, printer, thumbnail)
    }

    private fun detectSlicer(line: String): String? = when {
        line.contains("OrcaSlicer", true) -> line.removePrefix(";").trim()
        line.contains("PrusaSlicer", true) -> line.removePrefix(";").trim()
        line.contains("Cura_SteamEngine", true) || line.contains("Cura", true) -> "Cura"
        else -> null
    }

    private fun parseTime(line: String): Long? {
        if (line.startsWith(";TIME:", true)) return line.substringAfter(':').trim().toDoubleOrNull()?.roundToLong()
        val t = line.substringAfter('=', line.substringAfter(':')).trim()
        var total = 0L
        Regex("(\\d+)d").find(t)?.groupValues?.get(1)?.toLongOrNull()?.let { total += it * 86400 }
        Regex("(\\d+)h").find(t)?.groupValues?.get(1)?.toLongOrNull()?.let { total += it * 3600 }
        Regex("(\\d+)m").find(t)?.groupValues?.get(1)?.toLongOrNull()?.let { total += it * 60 }
        Regex("(\\d+)s").find(t)?.groupValues?.get(1)?.toLongOrNull()?.let { total += it }
        return total.takeIf { it > 0 }
    }

    private fun valueAfterEquals(line: String) = line.substringAfter('=').substringBefore(',').trim().toDoubleOrNull()
    private fun number(line: String) = line.substringAfter('=', line.substringAfter(':')).trim().substringBefore(',').toDoubleOrNull()
    private fun textValue(line: String) = line.substringAfter('=', line.substringAfter(':')).trim().trim('"').takeIf { it.isNotBlank() }
    private fun parseLengthToMm(value: String): Double? {
        val n = Regex("[-+]?\\d*\\.?\\d+").find(value)?.value?.toDoubleOrNull() ?: return null
        return when {
            value.contains(" m", true) -> n * 1000.0
            value.contains("cm", true) -> n * 10.0
            else -> n
        }
    }
}
