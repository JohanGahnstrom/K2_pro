package com.openfilament.cfs.nfc

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * CFS RFID codec.
 *
 * Ported and verified from the community-verified DnG-Crafts/K2-RFID lineage
 * (directly from flamebarke/creality_rfid, a Python3 port of the original
 * project — see THIRD_PARTY.yml). This is NOT a novel derivation: the master
 * keys and algorithm below are publicly documented in that source and are
 * reproduced here verbatim, ported to Kotlin/JVM crypto APIs.
 *
 * Verified against the golden vector published in flamebarke/creality_rfid's
 * README: UID 35B94A19 -> Key B 239E7FE23653 -> decrypts to
 * "1A5241201B3D010010000000033000000100000000000000"
 * (HyperPLA / code 01001 / length 0330 = 1.0 kg / serial 000001 / black).
 * See CfsCodecTest for the executable regression test — this is a MANDATORY
 * gate before the write path is enabled (FUNCTIONAL_DESCRIPTION.md §8.4).
 *
 * Block cipher mode: AES-128-ECB for both key derivation and payload
 * encryption. This resolves the ECB-vs-CBC open question in
 * FUNCTIONAL_DESCRIPTION.md §8.2 in favour of ECB, empirically, via this
 * source.
 */
object CfsCodec {

    /** Factory-default MIFARE Classic key present on a genuinely blank/unwritten tag. */
    val FACTORY_DEFAULT_KEY: ByteArray = hexToBytes("FFFFFFFFFFFF")

    // Master key used to derive the per-tag sector key from the tag UID.
    private val KEY_GEN = hexToBytes("713362755E74316E71665A2870662431")

    // Master key used to encrypt/decrypt the 48-byte identity payload.
    private val KEY_CIPHER = hexToBytes("484043466B526E7A404B4174424A7032")

    // Access-bits + GPB byte used when installing a fresh sector trailer on a
    // previously-blank tag: Key A(6) + FF 07 80 69 + Key B(6). Key A and Key B
    // are set to the SAME derived value in this scheme — confirm this holds
    // for real factory-programmed CFS tags on the bench (FUNCTIONAL_DESCRIPTION.md §8.6, §20).
    private val ACCESS_BITS_AND_GPB = hexToBytes("FF078069")

    /**
     * Derives the MIFARE sector-1 key (used as both Key A and Key B on a
     * CFS-secured tag) from the tag's UID. This is a deterministic transform,
     * not a secret to recover — no Crypto1 attack, dictionary, or brute force
     * is involved (FUNCTIONAL_DESCRIPTION.md §8.1).
     *
     * [uidHex] is the tag UID as hex, 8 chars (4 bytes) or 14 chars (7 bytes) —
     * e.g. from `Tag.id.joinToString("") { "%02X".format(it) }`.
     */
    fun deriveSectorKey(uidHex: String): ByteArray {
        val clean = uidHex.replace(" ", "").replace(":", "").uppercase()
        require(clean.length == 8 || clean.length == 14) {
            "UID must be 8 or 14 hex characters (4 or 7 bytes), got ${clean.length}"
        }
        val repeated = (clean + clean + clean + clean).substring(0, 32)
        val uidBlock = hexToBytes(repeated)
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(KEY_GEN, "AES"))
        return cipher.doFinal(uidBlock).copyOfRange(0, 6)
    }

    /** Builds the sector trailer (Block 7) needed to secure a previously-blank tag. */
    fun buildSectorTrailer(sectorKey: ByteArray): ByteArray {
        require(sectorKey.size == 6) { "Sector key must be 6 bytes" }
        return sectorKey + ACCESS_BITS_AND_GPB + sectorKey
    }

    /**
     * The 48-character ASCII identity payload written across Blocks 4-6:
     * batch(3) + date(5, YYMDD) + supplier(4) + material(5) + color(7, 0RRGGBB)
     * + length(4) + serial(6) + reserve(14).
     */
    data class Payload(
        val batch: String,
        val date: String,
        val supplier: String,
        val material: String,
        val color: String,
        val length: String,
        val serial: String,
        val reserve: String = "00000000000000"
    ) {
        init {
            require(batch.length == 3) { "batch must be 3 chars, got '$batch'" }
            require(date.length == 5) { "date must be 5 chars (YYMDD), got '$date'" }
            require(supplier.length == 4) { "supplier must be 4 chars, got '$supplier'" }
            require(material.length == 5) { "material must be 5 chars, got '$material'" }
            require(color.length == 7) { "color must be 7 chars (0RRGGBB), got '$color'" }
            require(length.length == 4) { "length must be 4 chars, got '$length'" }
            require(serial.length == 6) { "serial must be 6 chars, got '$serial'" }
            require(reserve.length == 14) { "reserve must be 14 chars, got '$reserve'" }
        }

        val ascii: String get() = batch + date + supplier + material + color + length + serial + reserve
    }

    /** Encrypts a [Payload] into the three 16-byte blocks to write at Blocks 4, 5, 6. */
    fun encryptPayload(payload: Payload): List<ByteArray> {
        val ascii = payload.ascii
        require(ascii.length == 48) { "Payload must encode to exactly 48 ASCII bytes, got ${ascii.length}" }
        val data = ascii.toByteArray(StandardCharsets.US_ASCII)
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(KEY_CIPHER, "AES"))
        val encrypted = cipher.doFinal(data)
        return listOf(
            encrypted.copyOfRange(0, 16),
            encrypted.copyOfRange(16, 32),
            encrypted.copyOfRange(32, 48)
        )
    }

    /** Decrypts Blocks 4-6 back into the 48-char ASCII payload string. */
    fun decryptBlocks(block4: ByteArray, block5: ByteArray, block6: ByteArray): String {
        require(block4.size == 16 && block5.size == 16 && block6.size == 16) {
            "Each block must be exactly 16 bytes"
        }
        val cipher = Cipher.getInstance("AES/ECB/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(KEY_CIPHER, "AES"))
        val decrypted = cipher.doFinal(block4 + block5 + block6)
        return String(decrypted, StandardCharsets.US_ASCII)
    }

    /** Parses a decrypted 48-char ASCII payload into structured fields. */
    fun parse(ascii: String): Payload {
        require(ascii.length >= 48) { "Decoded data too short: ${ascii.length} chars" }
        return Payload(
            batch = ascii.substring(0, 3),
            date = ascii.substring(3, 8),
            supplier = ascii.substring(8, 12),
            material = ascii.substring(12, 17),
            color = ascii.substring(17, 24),
            length = ascii.substring(24, 28),
            serial = ascii.substring(28, 34),
            reserve = ascii.substring(34, 48)
        )
    }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i ->
            ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte()
        }
}
