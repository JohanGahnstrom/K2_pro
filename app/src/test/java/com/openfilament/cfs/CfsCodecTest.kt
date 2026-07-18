package com.openfilament.cfs

import com.openfilament.cfs.nfc.CfsCodec
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Mandatory golden-vector regression test (FUNCTIONAL_DESCRIPTION.md §8.4).
 * A codec change that fails this test must NOT be wired to the write UI.
 *
 * Vector source: flamebarke/creality_rfid README (a Python3 port of
 * DnG-Crafts/K2-RFID), reproduced verbatim. See THIRD_PARTY.yml.
 */
class CfsCodecTest {

    private fun ByteArray.toHex() = joinToString("") { "%02X".format(it) }

    @Test
    fun `derives the published golden-vector key from UID`() {
        val key = CfsCodec.deriveSectorKey("35B94A19")
        assertEquals("239E7FE23653", key.toHex())
    }

    @Test
    fun `builds the exact 48-char ASCII payload from the golden vector fields`() {
        val payload = CfsCodec.Payload(
            batch = "1A5", date = "24120", supplier = "1B3D",
            material = "01001", color = "0000000", length = "0330", serial = "000001"
        )
        assertEquals("1A5241201B3D010010000000033000000100000000000000", payload.ascii)
    }

    @Test
    fun `encrypts to the exact published golden-vector ciphertext blocks`() {
        val payload = CfsCodec.Payload(
            batch = "1A5", date = "24120", supplier = "1B3D",
            material = "01001", color = "0000000", length = "0330", serial = "000001"
        )
        val blocks = CfsCodec.encryptPayload(payload)
        assertEquals("07881A468B7D754A76A07C9EBB452B63", blocks[0].toHex())
        assertEquals("E07623E57AA4DFC8F23BB22F645DC64B", blocks[1].toHex())
        assertEquals("FAC8F07509292DF943D4CDF64CBA06A1", blocks[2].toHex())
    }

    @Test
    fun `decrypts the golden-vector blocks back to the exact ASCII payload`() {
        fun hex(s: String) = ByteArray(s.length / 2) { i ->
            ((Character.digit(s[i * 2], 16) shl 4) + Character.digit(s[i * 2 + 1], 16)).toByte()
        }
        val ascii = CfsCodec.decryptBlocks(
            hex("07881A468B7D754A76A07C9EBB452B63"),
            hex("E07623E57AA4DFC8F23BB22F645DC64B"),
            hex("FAC8F07509292DF943D4CDF64CBA06A1")
        )
        assertEquals("1A5241201B3D010010000000033000000100000000000000", ascii)
    }

    @Test
    fun `full roundtrip parses back to the golden-vector fields`() {
        val original = CfsCodec.Payload(
            batch = "1A5", date = "24120", supplier = "1B3D",
            material = "01001", color = "0000000", length = "0330", serial = "000001"
        )
        val blocks = CfsCodec.encryptPayload(original)
        val decoded = CfsCodec.decryptBlocks(blocks[0], blocks[1], blocks[2])
        val parsed = CfsCodec.parse(decoded)
        assertEquals("01001", parsed.material)
        assertEquals("0330", parsed.length)
        assertEquals("000001", parsed.serial)
        assertEquals("0000000", parsed.color)
    }

    @Test
    fun `sector trailer installs the same derived key as both Key A and Key B`() {
        val key = CfsCodec.deriveSectorKey("35B94A19")
        val trailer = CfsCodec.buildSectorTrailer(key)
        assertEquals(16, trailer.size)
        assertEquals(key.toHex(), trailer.copyOfRange(0, 6).toHex())
        assertEquals("FF078069", trailer.copyOfRange(6, 10).toHex())
        assertEquals(key.toHex(), trailer.copyOfRange(10, 16).toHex())
    }
}
