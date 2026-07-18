package com.marewise.openfilament.rfid

import com.marewise.openfilament.domain.SpoolDraft
import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * Adapted from DnG-Crafts/K2-RFID and cross-checked against flamebarke/creality_rfid.
 * DnG source is MIT; see THIRD_PARTY_NOTICES.md.
 */
object CfsCodec {
    private val uidMaster = byteArrayOf(113,51,98,117,94,116,49,110,113,102,90,40,112,102,36,49)
    private val payloadMaster = byteArrayOf(72,64,67,70,107,82,110,122,64,75,65,116,66,74,112,50)

    fun deriveSectorKey(uid: ByteArray): ByteArray {
        require(uid.size >= 4) { "A 4-byte UID is required" }
        val repeated = ByteArray(16) { uid[it % 4] }
        return aes(Cipher.ENCRYPT_MODE, uidMaster, repeated).copyOfRange(0, 6)
    }

    fun encodePayload(draft: SpoolDraft): ByteArray {
        val plain = buildPlainPayload(draft).toByteArray(StandardCharsets.US_ASCII)
        require(plain.size == 48) { "CFS payload must be exactly 48 bytes, got ${plain.size}" }
        return aes(Cipher.ENCRYPT_MODE, payloadMaster, plain)
    }

    fun decodePayload(encrypted: ByteArray): String {
        require(encrypted.size == 48)
        return String(aes(Cipher.DECRYPT_MODE, payloadMaster, encrypted), StandardCharsets.US_ASCII)
    }

    fun buildPlainPayload(draft: SpoolDraft): String {
        val color = draft.colorHex.uppercase().removePrefix("#").padStart(6, '0').takeLast(6)
        val serial = draft.serial.filter(Char::isDigit).padStart(6, '0').takeLast(6)
        return "AB124" + draft.vendorId.padStart(4,'0').takeLast(4) + draft.batch.padEnd(2,'0').take(2) +
            "1" + draft.materialCode.padStart(5,'0').takeLast(5) + "0" + color + draft.size.cfsLengthCode + serial +
            "00000000000000"
    }

    fun buildOptionalExtension(draft: SpoolDraft): ByteArray = draft.printerSuffix.lowercase().padEnd(48, ' ').take(48).toByteArray(StandardCharsets.US_ASCII)

    private fun aes(mode: Int, key: ByteArray, data: ByteArray): ByteArray =
        Cipher.getInstance("AES/ECB/NoPadding").run { init(mode, SecretKeySpec(key, "AES")); doFinal(data) }
}
