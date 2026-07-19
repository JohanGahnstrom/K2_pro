package com.openfilament.cfs.domain

import com.openfilament.cfs.data.MaterialCodes
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

enum class UserMode { SIMPLE, EXPERT }
enum class MappingConfidence { VERIFIED, STRONG, REASONABLE, EXPERIMENTAL, UNSUPPORTED }
enum class PrinterTrust { READ_ONLY, STANDARD, FILAMENT, EXPERT }
enum class TagSecurityState { UNKNOWN, FACTORY_DEFAULT, CFS_SECURED, UNSUPPORTED_HARDWARE }

data class FilamentProduct(
    val id: String,
    val brand: String,
    val line: String,
    val material: String,
    val density: Double,
    val defaultWeightG: Int,
    val diameterMm: Double = 1.75,
    val colors: List<FilamentColor>,
    /** Creality material code, e.g. "00003" (GenericPETG) — see MaterialCodes registry. */
    val targetMaterialId: String,
    val confidence: MappingConfidence,
    val sourceUrl: String
) {
    val materialCodeName: String get() = MaterialCodes.nameFor(targetMaterialId)
}

data class FilamentColor(val name: String, val hex: String) {
    /** Creality tag colour field format: 0RRGGBB. */
    val tagColorField: String get() = "0" + hex.removePrefix("#").uppercase().padStart(6, '0')
}

/** Generates a fresh random spool serial. Called once per distinct spool
 * identity (see MainViewModel.currentSerial) — NOT suitable as a default
 * argument value read repeatedly from a `get()` computed property, since
 * each evaluation of a default expression produces a different value. */
fun generateSpoolSerial(): String = UUID.randomUUID().toString().replace("-", "").take(6).uppercase()

data class SpoolDraft(
    val product: FilamentProduct,
    val color: FilamentColor,
    val weightG: Int,
    val serial: String,
    val remainingG: Int = weightG,
) {
    /** Nominal STARTING quantity only — never treat as live remaining material.
     * Live remaining quantity should come from box.remain_len or Moonraker's
     * Spoolman integration when a printer is connected (FUNCTIONAL_DESCRIPTION.md §7). */
    val nominalLengthM: Int get() = FilamentMath.lengthMeters(weightG, product.density, product.diameterMm)

    /** Best-effort length-code for the tag payload; only 1000g/500g are bench-verified (MaterialCodes). */
    val tagLengthField: String get() = MaterialCodes.inferredLengthCode(weightG)
}

/**
 * A local record of a spool this app has actually written a tag for —
 * FUNCTIONAL_DESCRIPTION.md §3.1/§4's "local spool list." Persisted across
 * restarts (data/SettingsStore.setSpools/decodeSpools) as JSON referencing
 * just the product id/colour name, not the full FilamentProduct/
 * FilamentColor structure — Catalogue.products remains the single source
 * of truth for product data. A Room-backed store would be the natural next
 * step if this ever needs querying beyond "the whole list."
 */
data class TaggedSpool(
    val product: FilamentProduct,
    val color: FilamentColor,
    val weightG: Int,
    val serial: String,
    val taggedAt: java.util.Date,
)

/** Per-CFS-unit slot state, mirrored from the community-documented Moonraker
 * `box` object schema (FUNCTIONAL_DESCRIPTION.md §11). Confirmed on K2 Plus;
 * K2 Pro field names are expected but not yet bench-verified (H-004). */
data class CfsSlot(
    val index: Int,
    val materialCode: String? = null,
    val colorHex: String? = null,
    val remainingLengthM: Int? = null,
    val temperatureC: Double? = null,
    val state: String = "Unknown"
) {
    val materialName: String get() = materialCode?.let { MaterialCodes.nameFor(it) } ?: "Empty"
}

data class PrinterSnapshot(
    val online: Boolean = false,
    val hostname: String = "Not connected",
    val state: String = "Offline",
    val progress: Int = 0,
    val nozzleC: Double? = null,
    val bedC: Double? = null,
    val chamberC: Double? = null,
    val autoRefillEnabled: Boolean = false,
    val slots: List<CfsSlot> = emptyList()
)

object FilamentMath {
    /**
     * Nominal starting length only — see SpoolDraft.nominalLengthM doc.
     *
     * BUG FIX (0.2.0): the 0.1.0 baseline included an erroneous extra
     * `* 1000` in the numerator, inflating every result by exactly 1000x
     * (e.g. it would have returned ~327,364 m instead of 327 m for 1 kg
     * PETG). That baseline's Gradle build — and therefore its unit tests —
     * had never actually been executed (see BUILD_STATUS.md history), so
     * the bug was never caught despite a passing-looking assertion in
     * FilamentMathTest. This version was verified by actually compiling
     * and running the formula (mass_g / (density_g_cm3 * pi * (d_mm/2)^2)
     * gives 327 for 1 kg PETG at 1.27 g/cm3, 1.75 mm — matching the
     * documented reference value in FUNCTIONAL_DESCRIPTION.md §7).
     */
    fun lengthMeters(massG: Int, densityGcm3: Double, diameterMm: Double): Int {
        require(massG > 0)
        require(densityGcm3 > 0)
        require(diameterMm > 0)
        val radius = BigDecimal.valueOf(diameterMm).divide(BigDecimal.valueOf(2))
        val area = BigDecimal.valueOf(Math.PI).multiply(radius.pow(2))
        val length = BigDecimal.valueOf(massG.toLong())
            .divide(BigDecimal.valueOf(densityGcm3).multiply(area), 8, RoundingMode.HALF_UP)
        return length.setScale(0, RoundingMode.HALF_UP).toInt()
    }
}
