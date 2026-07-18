package com.openfilament.cfs.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build

/**
 * H-000 device-compatibility gate (FUNCTIONAL_DESCRIPTION.md §8.5).
 *
 * `android.nfc.tech.MifareClassic` is an OPTIONAL Android API, implemented
 * only where the phone's NFC controller is NXP-compatible. Devices with
 * Broadcom or certain other controllers can read only the tag UID (NfcA)
 * and will never enumerate MifareClassic at all. This must be checked
 * BEFORE entering any write flow, never discovered mid-transaction.
 */
object DeviceCompatibility {

    enum class Verdict { SUPPORTED, UNSUPPORTED, UNKNOWN }

    data class Assessment(val verdict: Verdict, val message: String)

    /**
     * Best-effort assessment from Build.MODEL/MANUFACTURER alone, usable at
     * app launch before any tag has been scanned (for the Home-screen
     * banner). This is NOT a substitute for [assessTag] — a device not on
     * either list is UNKNOWN, not assumed safe.
     *
     * Seed this list from ikarus23/MifareClassicTool's community-maintained
     * COMPATIBLE_DEVICES.md / INCOMPATIBLE_DEVICES.md (see SOURCE_REGISTER.md)
     * and this project's own bench results. Keep it small and honest rather
     * than large and unverified.
     */
    private val KNOWN_COMPATIBLE = setOf(
        "SM-S931", "SM-S936", "SM-S938", // Galaxy S25 family — observed working (CFSWriter README)
        "SM-S951", "SM-S956", "SM-S958", // Galaxy S26 family — observed working (CFSWriter README)
        "PIXEL 9", "PIXEL 9 PRO", "PIXEL 9A"
    )

    private val KNOWN_INCOMPATIBLE = setOf(
        "PIXEL 8", "PIXEL 8 PRO" // community-reported MIFARE Classic failures with non-default keys
    )

    fun assessDeviceModel(model: String = Build.MODEL, manufacturer: String = Build.MANUFACTURER): Assessment {
        val normalized = model.uppercase()
        return when {
            KNOWN_INCOMPATIBLE.any { normalized.contains(it) } ->
                Assessment(Verdict.UNSUPPORTED, "This phone model has reported MIFARE Classic failures. Tag writing may not work — a tag scan will confirm.")
            KNOWN_COMPATIBLE.any { normalized.contains(it) } ->
                Assessment(Verdict.SUPPORTED, "This phone model is known to support MIFARE Classic 1K.")
            else ->
                Assessment(Verdict.UNKNOWN, "MIFARE Classic support for this phone is unconfirmed. Scan a tag to check before writing.")
        }
    }

    /**
     * Definitive, tag-present check. Call this on every tag scan before
     * authenticating or writing — never rely on [assessDeviceModel] alone
     * to permit a write.
     */
    fun assessTag(tag: Tag): Assessment {
        val techList = tag.techList.toSet()
        if (!techList.contains(MifareClassic::class.java.name)) {
            return Assessment(
                Verdict.UNSUPPORTED,
                "This phone's NFC hardware does not support MIFARE Classic tags — only the tag ID was readable. " +
                    "Tag writing is not possible on this device. See FUNCTIONAL_DESCRIPTION.md §8.5."
            )
        }
        val mifare = MifareClassic.get(tag)
            ?: return Assessment(Verdict.UNSUPPORTED, "MIFARE Classic technology handle unavailable for this tag/phone combination.")
        return try {
            mifare.connect()
            val ok = mifare.type == MifareClassic.TYPE_CLASSIC && mifare.size == MifareClassic.SIZE_1K
            if (ok) Assessment(Verdict.SUPPORTED, "MIFARE Classic 1K confirmed on this phone and tag.")
            else Assessment(Verdict.UNSUPPORTED, "Tag responded but is not a MIFARE Classic 1K (type=${mifare.type}, size=${mifare.size}).")
        } catch (e: Exception) {
            Assessment(Verdict.UNSUPPORTED, "MIFARE Classic connection failed: ${e.message ?: "unknown error"}.")
        } finally {
            runCatching { mifare.close() }
        }
    }
}
