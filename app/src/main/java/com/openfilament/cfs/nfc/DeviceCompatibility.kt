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
     * Seeded from ikarus23/MifareClassicTool's community-maintained
     * COMPATIBLE_DEVICES.md / INCOMPATIBLE_DEVICES.md (fetched 2026-07-18,
     * see SOURCE_REGISTER.md) plus this project's own bench-adjacent
     * sources (CFSWriter README for the Galaxy S25/S26 family).
     *
     * IMPORTANT — most of MifareClassicTool's list uses marketing names
     * (e.g. "Samsung Galaxy S9"), which do NOT appear in Build.MODEL on
     * non-Pixel hardware (Samsung reports e.g. "SM-G960F", not "Galaxy
     * S9"). Only entries below are ones where Build.MODEL reliably
     * contains the matched string: Google Pixel devices (Build.MODEL is
     * the marketing name) and the Galaxy S25/S26 model codes already
     * bench-confirmed via CFSWriter. The rest of the community lists are
     * kept verbatim in [COMMUNITY_REPORTED_COMPATIBLE_MARKETING_NAMES] /
     * [COMMUNITY_REPORTED_INCOMPATIBLE_MARKETING_NAMES] below for manual
     * cross-reference — do NOT add them here without a real model-code
     * translation first. A false SUPPORTED verdict is worse than an
     * honest UNKNOWN.
     */
    private val KNOWN_COMPATIBLE = setOf(
        "SM-S931", "SM-S936", "SM-S938", // Galaxy S25 family — observed working (CFSWriter README)
        "SM-S951", "SM-S956", "SM-S958", // Galaxy S26 family — observed working (CFSWriter README)
        "PIXEL 2", "PIXEL 3A XL", "PIXEL 3A", "PIXEL 3", "PIXEL 4A", "PIXEL 5A",
        "PIXEL 6 PRO", "PIXEL 6A", "PIXEL 7", "PIXEL 8A", "PIXEL 9 PRO", "PIXEL 9A", "PIXEL 9"
        // Longer strings ("PIXEL 3A XL") are listed before their prefixes ("PIXEL 3A") only for
        // readability — matching uses `any { contains(it) }`, so order has no effect on the result.
        //
        // Deliberately NO bare "PIXEL" entry: `contains("PIXEL")` would also match every future,
        // unverified Pixel model (e.g. a hypothetical "Pixel 10"), which is exactly the false
        // SUPPORTED verdict this class's doc warns against. The original 2016 "Pixel"/"Pixel XL"
        // is handled by exact equality in assessDeviceModel instead.
    )

    // Source lists only "Google Pixel", not "Pixel XL" — matched by exact equality, not
    // substring, so it can't accidentally catch any later/unverified model.
    private const val ORIGINAL_PIXEL_EXACT_MODEL = "PIXEL"

    /** No confirmed-incompatible entry currently has a safe Build.MODEL substring match; see the
     *  class doc above and [COMMUNITY_REPORTED_INCOMPATIBLE_MARKETING_NAMES]. Pixel 8/8 Pro was
     *  previously listed here — see [assessDeviceModel]'s dedicated, Android-version-gated handling. */
    private val KNOWN_INCOMPATIBLE = emptySet<String>()

    /**
     * Google fixed MIFARE Classic on Pixel 8/8 Pro starting Android 15
     * (SDK 35): ikarus23/MifareClassicTool's COMPATIBLE_DEVICES.md lists
     * "Google Pixel 8 / 8 Pro (Android 15+)" explicitly, which reconciles
     * the earlier XDA failure reports cited in SOURCE_REGISTER.md (those
     * predate the fix). Below that OS version, treat as UNKNOWN rather
     * than UNSUPPORTED — the failure was version-gated, not permanent.
     */
    private const val PIXEL_8_MIN_SDK_FOR_SUPPORT = 35 // Android 15

    /**
     * [model] is deliberately nullable: Android's real "for unit tests" stub
     * jar returns null for Build.MODEL (a non-issue on an actual device,
     * which never has a null model string) — a non-null `String` parameter
     * would make Kotlin emit an Intrinsics.checkNotNullParameter that throws
     * NPE the moment this is called with defaults under testDebugUnitTest
     * (confirmed by real CI, not this environment's standalone JVM harness,
     * which used hand-written stubs that didn't reproduce this). A null/
     * blank model safely falls through to UNKNOWN below, which is the
     * correct verdict for "we don't know what this device is" anyway.
     * manufacturer was accepted but never actually used in this function
     * and has been dropped rather than given the same treatment for no
     * purpose.
     */
    fun assessDeviceModel(
        model: String? = Build.MODEL,
        sdkInt: Int = Build.VERSION.SDK_INT,
    ): Assessment {
        val normalized = (model ?: "").uppercase().trim()
        val isPixel8 = normalized.contains("PIXEL 8") && !normalized.contains("PIXEL 8A")
        return when {
            isPixel8 && sdkInt >= PIXEL_8_MIN_SDK_FOR_SUPPORT ->
                Assessment(Verdict.SUPPORTED, "This phone model is known to support MIFARE Classic 1K (fixed on Android 15+).")
            isPixel8 ->
                Assessment(Verdict.UNKNOWN, "Pixel 8/8 Pro support MIFARE Classic starting with Android 15 — this device reports an earlier version. Scan a tag to check before writing.")
            normalized == ORIGINAL_PIXEL_EXACT_MODEL ->
                Assessment(Verdict.SUPPORTED, "This phone model is known to support MIFARE Classic 1K.")
            KNOWN_INCOMPATIBLE.any { normalized.contains(it) } ->
                Assessment(Verdict.UNSUPPORTED, "This phone model has reported MIFARE Classic failures. Tag writing may not work — a tag scan will confirm.")
            KNOWN_COMPATIBLE.any { normalized.contains(it) } ->
                Assessment(Verdict.SUPPORTED, "This phone model is known to support MIFARE Classic 1K.")
            else ->
                Assessment(Verdict.UNKNOWN, "MIFARE Classic support for this phone is unconfirmed. Scan a tag to check before writing.")
        }
    }

    /**
     * Verbatim marketing-name lists from ikarus23/MifareClassicTool
     * (fetched 2026-07-18), kept for manual cross-reference and future
     * model-code translation work (see H-000 in HOLD_REGISTER.md). NOT
     * used by [assessDeviceModel] — see the warning on [KNOWN_COMPATIBLE].
     */
    val COMMUNITY_REPORTED_COMPATIBLE_MARKETING_NAMES = listOf(
        "Asus Pegasus 2 (X550)", "BQ Aquaris X2 Pro", "BQ Aquaris X5 Plus (Android 7.1.1)",
        "Fairphone 4", "Fairphone 5", "Google Nexus 7 (2012)", "Google Nexus 6P", "Google Pixel",
        "Google Pixel 2", "Google Pixel 3", "Google Pixel 3a", "Google Pixel 3a XL",
        "Google Pixel 4a", "Google Pixel 5a", "Google Pixel 6 Pro", "Google Pixel 6a",
        "Google Pixel 7", "Google Pixel 8 / 8 Pro (Android 15+)", "Google Pixel 8a", "Honor 9",
        "Honor 10", "HTC One", "Huawei Ascend 620 (G620S)", "Huawei Ascend Mate7",
        "Huawei Mate 20", "Huawei P7", "Huawei P8 Lite (2016, 2017)", "Huawei P9",
        "Huawei P10 Lite", "Lenovo P2", "LGE LG G3", "LGE LG G4 (H815)", "LGE LG G5",
        "LGE LG G6 (H870)", "Motorola One Vision", "Motorola G5 Plus", "Motorola Photon Q",
        "Nokia 3", "Nokia 5", "Nokia 6.1", "Nokia 6.2", "Nokia 7 Plus", "Nokia 8",
        "Nokia G21 (TA-1418)", "OnePlus One (only with certain versions of ROMs)",
        "OnePlus 3/3T (only with certain versions of ROMs)", "OnePlus 5/5T", "OnePlus 6/6T",
        "OnePlus 7 Pro", "Philips Xenium i908", "Samsung Galaxy A3 (2017, Android 8.0)",
        "Samsung Galaxy A5 (2017, Android 8.0)", "Samsung Galaxy A8 Plus",
        "Samsung Galaxy A40 (SM-A405FN)", "Samsung Galaxy A31 (SM-A315G) (market/region dependent)",
        "Samsung Galaxy A41 (SM-A415F)", "Samsung Galaxy A51",
        "Samsung Galaxy J3 (2016) (Models: J320FN, J320F, J320P)",
        "Samsung Galaxy J7 Pro (2017, Android 8.1)", "Samsung Galaxy M11", "Samsung Galaxy Nexus",
        "Samsung Galaxy Note 2", "Samsung Galaxy Note 8", "Samsung Galaxy Note 10",
        "Samsung Galaxy S3 (i9300)", "Samsung Galaxy S3 Duo (i9300i)",
        "Samsung Galaxy S3 Mini Value Edition (GT-I8200N)", "Samsung Galaxy S5",
        "Samsung Galaxy S7 (Android 8.0)", "Samsung Galaxy S7 Edge (hero2qltechn, Android 7.0)",
        "Samsung Galaxy S8 (Android 8.0)", "Samsung Galaxy S8 Plus (Android 8.0)",
        "Samsung Galaxy S9", "Samsung Galaxy S20 FE", "Samsung Galaxy S22", "Samsung Galaxy S24 FE",
        "Samsung Galaxy Tab Active2", "Sony Xperia M4 Aqua", "Sony Xperia V",
        "Sony Xperia X Compact (Android 8.0)", "Sony Xperia Z2 (D6503, Android 6.0.1 )",
        "Sony Xperia Z3", "Sony Xperia Z5 Compact", "Sony Xperia XZ", "Wiko Wim Lite",
        "Xiaomi Mi 5", "Xiaomi Mi 9 Lite", "Xiaomi Mi 9T", "Xiaomi Mi 10T", "Xiaomi Mi 10T Pro",
        "Xiaomi Mi 11T Pro", "Xiaomi Mi 11 Lite 5G NE", "Xiaomi Mi Mix 2S", "Xiaomi Poco X6 Pro",
        "Xiaomi Redmi K20 Pro", "Xiaomi Redmi K30 5G", "Xiaomi Redmi Note 8T",
        "Xiaomi Redmi Note 9 Pro", "Xiaomi Redmi Note 11 NFC", "Yota Phone (YD201, YD206)",
        "ZTE Axxon 7",
    )

    val COMMUNITY_REPORTED_INCOMPATIBLE_MARKETING_NAMES = listOf(
        "Asus Zenfone 2", "Blackberry Priv", "Blackview BV5500 Pro/Plus", "Blackview BV8000 Pro",
        "Doogee S60", "Fairphone 3", "Foxcon InFocus M320", "Google Nexus 4", "Google Nexus 5",
        "Google Nexus 6", "Google Nexus 7 (2013)", "Google Nexus 9", "Google Nexus 10",
        "HTC One M8", "Jiayu S3", "Lenovo Vibe P1", "Lenovo Vibe P2", "LGE LG F60", "LGE LG G2",
        "LGE LG G2 mini", "LGE LG G3 S", "LGE LG G4 Beat", "LGE LG K10", "LGE LG Optimus L7 II",
        "LGE LG Phoenix 2", "LGE LG V10", "Motorola One Vision Plus", "Motorola Droid Turbo",
        "Motorola Moto Maxx", "Motorola Moto Style", "Motorola Moto X (2014, 2ed gen.)",
        "Motorola Moto X Force", "Motorola Moto X Play", "Motorola Moto X Style", "Nubia Z18",
        "Samsung Galaxy A3 (2015)", "Samsung Galaxy A5 (2016)", "Samsung Galaxy A7 (2016, 2017)",
        "Samsung Galaxy A8 (2018)", "Samsung Galaxy A9 (2016)", "Samsung Galaxy Ace 3",
        "Samsung Galaxy Ace 4", "Samsung Galaxy Alpha", "Samsung Galaxy E7",
        "Samsung Galaxy Express 2", "Samsung Galaxy J3 (2016) (Models: J320G, J320M, J320A, J320V)",
        "Samsung Galaxy J5 (2017)", "Samsung Galaxy J7 (2016, 2017)", "Samsung Galaxy Mega",
        "Samsung Galaxy Note 3", "Samsung Galaxy Note 4", "Samsung Galaxy Note 5",
        "Samsung Galaxy Note 6", "Samsung Galaxy Grand Prime", "Samsung Galaxy S4",
        "Samsung Galaxy S4 Mini", "Samsung Galaxy S5 Mini", "Samsung Galaxy S5 Neo",
        "Samsung Galaxy S6", "Samsung Galaxy S6 Edge", "Samsung Galaxy S6 Edge Plus",
        "Samsung Galaxy S7 Edge (some Versions)", "Samsung Galaxy Xcover 3", "Sharp Aquos Zero 2",
        "Sharp Disney Mobile on DoCoMo SH-02G", "Sony Xperia X Compact",
        "Sony Xperia Z2 (some models)", "Sony Xperia Z3 (SOL26)", "Xiaomi MI 3",
        "ZTE Nubia Z7 Max(NX505J)",
    )

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
