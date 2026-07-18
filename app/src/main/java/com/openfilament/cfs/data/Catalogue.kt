package com.openfilament.cfs.data

import com.openfilament.cfs.domain.*

/**
 * Demonstrator catalogue. Filament facts (density/diameter) draw on
 * SpoolmanDB conventions; target material codes are now REAL, verified
 * Creality codes from MaterialCodes (ported from the community-verified
 * codec source) rather than placeholders — see FUNCTIONAL_DESCRIPTION.md §5-6.
 * Confidence reflects generic-family fit, not code validity: the codes
 * themselves are Verified; brand-specific formulation match remains
 * Reasonable until confirmed against real hardware.
 */
object Catalogue {
    val products = listOf(
        FilamentProduct("sunlu-hs-petg", "SUNLU", "High Speed PETG", "PETG", 1.27, 1000,
            colors = listOf(FilamentColor("Midnight Black", "#111217"), FilamentColor("Safety Orange", "#FF5A18"), FilamentColor("Ocean Blue", "#176BFF")),
            targetMaterialId = "00003", confidence = MappingConfidence.REASONABLE, // GenericPETG
            sourceUrl = "https://donkie.github.io/SpoolmanDB/filaments.json"),
        FilamentProduct("esun-pla-plus", "eSUN", "PLA+", "PLA+", 1.24, 1000,
            colors = listOf(FilamentColor("Cold White", "#F1F3F5"), FilamentColor("Fire Engine Red", "#D8292F"), FilamentColor("Pine Green", "#166B49")),
            targetMaterialId = "00001", confidence = MappingConfidence.REASONABLE, // GenericPLA
            sourceUrl = "https://donkie.github.io/SpoolmanDB/filaments.json"),
        FilamentProduct("polymaker-asa", "Polymaker", "PolyLite ASA", "ASA", 1.07, 1000,
            colors = listOf(FilamentColor("Jet Black", "#151515"), FilamentColor("Army Beige", "#C3AE82"), FilamentColor("Teal", "#008E8A")),
            targetMaterialId = "00007", confidence = MappingConfidence.REASONABLE, // GenericASA
            sourceUrl = "https://donkie.github.io/SpoolmanDB/filaments.json"),
        FilamentProduct("overture-tpu", "OVERTURE", "High Speed TPU", "TPU", 1.21, 1000,
            colors = listOf(FilamentColor("Black", "#0C0C0D"), FilamentColor("Digital Blue", "#1C70FF")),
            targetMaterialId = "00005", confidence = MappingConfidence.REASONABLE, // GenericTPU
            sourceUrl = "https://donkie.github.io/SpoolmanDB/filaments.json")
    )
}
