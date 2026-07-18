package com.openfilament.cfs.data

/**
 * Creality CFS material-code registry.
 *
 * Ported directly from the community-verified DnG-Crafts/K2-RFID lineage
 * (via flamebarke/creality_rfid). These are the material codes actually
 * observed/documented in that codebase — 41 entries, not a larger
 * speculative list. Treat entries as VERIFIED against the source; treat
 * any *new* code you add here as REASONABLE/EXPERIMENTAL until confirmed
 * on real K2 Pro firmware (FUNCTIONAL_DESCRIPTION.md §7.1, §20).
 */
object MaterialCodes {
    val REGISTRY: Map<String, String> = mapOf(
        "10001" to "HP-TPU",
        "11001" to "CR-Nylon",
        "13001" to "CR-PLACarbon",
        "14001" to "CR-PLAMatte",
        "15001" to "CR-PLAFluo",
        "16001" to "CR-TPU",
        "17001" to "CR-Wood",
        "18001" to "HPUltraPLA",
        "19001" to "HP-ASA",
        "07001" to "CR-ABS",
        "06001" to "CR-PETG",
        "04001" to "CR-PLA",
        "05001" to "CR-Silk",
        "09001" to "EN-PLA+",
        "09002" to "ENDERFASTPLA",
        "08001" to "Ender-PLA",
        "00004" to "GenericABS",
        "00007" to "GenericASA",
        "00010" to "GenericBVOH",
        "00012" to "GenericHIPS",
        "00008" to "GenericPA",
        "00009" to "GenericPA-CF",
        "00015" to "GenericPA6-CF",
        "00016" to "GenericPAHT-CF",
        "00021" to "GenericPC",
        "00020" to "GenericPET",
        "00013" to "GenericPET-CF",
        "00003" to "GenericPETG",
        "00014" to "GenericPETG-CF",
        "00001" to "GenericPLA",
        "00006" to "GenericPLA-CF",
        "00002" to "GenericPLA-Silk",
        "00019" to "GenericPP",
        "00017" to "GenericPPS",
        "00018" to "GenericPPS-CF",
        "00011" to "GenericPVA",
        "00005" to "GenericTPU",
        "03001" to "HyperABS",
        "06002" to "HyperPETG",
        "01001" to "HyperPLA",
        "02001" to "HyperPLA-CF"
    )

    /** Only two length codes are confirmed from the source; others are inferred
     * by linear proportion and must be treated as unverified until bench-tested. */
    val VERIFIED_LENGTH_CODES: Map<String, Int> = mapOf(
        "0330" to 1000, // 1.0 kg
        "0165" to 500   // 0.5 kg
    )

    fun nameFor(code: String): String = REGISTRY[code] ?: "Unknown ($code)"

    /** Best-effort length code for an arbitrary gram weight, linearly scaled from
     * the two verified reference points (330 length-units per 1000 g). Marked
     * inferred, not verified — see FUNCTIONAL_DESCRIPTION.md §7, §20. */
    fun inferredLengthCode(grams: Int): String {
        val units = (grams * 330) / 1000
        return units.toString().padStart(4, '0').takeLast(4)
    }
}
