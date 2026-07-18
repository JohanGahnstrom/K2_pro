package com.openfilament.cfs.nfc

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import com.openfilament.cfs.domain.TagSecurityState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NfcProbe(
    val compatibility: DeviceCompatibility.Assessment,
    val securityState: TagSecurityState,
    val uidHex: String?
)

data class RawTagSnapshot(val uidHex: String, val payload: CfsCodec.Payload)

sealed class WriteOutcome {
    data class Success(val payload: CfsCodec.Payload) : WriteOutcome()
    data class DeviceUnsupported(val reason: String) : WriteOutcome()
    data class Failed(val reason: String) : WriteOutcome()
}

/**
 * Real Android MIFARE Classic transport wired to the verified [CfsCodec].
 * Every write is followed by a mandatory read-back comparison
 * (FUNCTIONAL_DESCRIPTION.md §9) — success is never inferred from writeBlock()
 * returning without exception.
 */
class NfcTagService {

    /** Step 1-2 of the tag transaction: hardware gate + tag-security detection. */
    suspend fun probe(tag: Tag): NfcProbe = withContext(Dispatchers.IO) {
        val compatibility = DeviceCompatibility.assessTag(tag)
        if (compatibility.verdict != DeviceCompatibility.Verdict.SUPPORTED) {
            return@withContext NfcProbe(compatibility, TagSecurityState.UNSUPPORTED_HARDWARE, null)
        }
        val uidHex = tag.id.joinToString("") { "%02X".format(it) }
        val derivedKey = CfsCodec.deriveSectorKey(uidHex)
        val mifare = MifareClassic.get(tag) ?: return@withContext NfcProbe(compatibility, TagSecurityState.UNKNOWN, uidHex)
        val state = try {
            mifare.connect()
            when {
                mifare.authenticateSectorWithKeyA(1, derivedKey) -> TagSecurityState.CFS_SECURED
                mifare.authenticateSectorWithKeyA(1, CfsCodec.FACTORY_DEFAULT_KEY) -> TagSecurityState.FACTORY_DEFAULT
                else -> TagSecurityState.UNKNOWN
            }
        } catch (e: Exception) {
            TagSecurityState.UNKNOWN
        } finally {
            runCatching { mifare.close() }
        }
        NfcProbe(compatibility, state, uidHex)
    }

    /** Reads and decodes an already CFS-secured tag (derived key authenticates). */
    suspend fun readSecuredTag(tag: Tag): Result<RawTagSnapshot> = withContext(Dispatchers.IO) {
        runCatching {
            val uidHex = tag.id.joinToString("") { "%02X".format(it) }
            val key = CfsCodec.deriveSectorKey(uidHex)
            val mifare = requireNotNull(MifareClassic.get(tag)) { "MIFARE Classic unsupported on this phone" }
            try {
                mifare.connect()
                require(mifare.size == MifareClassic.SIZE_1K) { "A MIFARE Classic 1K tag is required" }
                require(mifare.authenticateSectorWithKeyA(1, key)) { "Sector 1 authentication failed with derived key" }
                val b4 = mifare.readBlock(4); val b5 = mifare.readBlock(5); val b6 = mifare.readBlock(6)
                val ascii = CfsCodec.decryptBlocks(b4, b5, b6)
                RawTagSnapshot(uidHex, CfsCodec.parse(ascii))
            } finally { runCatching { mifare.close() } }
        }
    }

    /**
     * Writes a [CfsCodec.Payload] to Blocks 4-6, verifying every block by
     * read-back, per the transaction sequence in FUNCTIONAL_DESCRIPTION.md §9.
     * [tagIsAlreadySecured] selects whether to authenticate with the derived
     * key (re-tagging a factory CFS tag) or the factory-default key followed
     * by installing a fresh sector trailer (a genuinely blank tag) — see §8.6.
     * The blank-tag trailer-install path is UNVERIFIED against real hardware
     * (H-011/§20) and should be treated as higher-risk than re-tagging.
     */
    suspend fun writeVerified(tag: Tag, payload: CfsCodec.Payload, tagIsAlreadySecured: Boolean): WriteOutcome =
        withContext(Dispatchers.IO) {
            val compatibility = DeviceCompatibility.assessTag(tag)
            if (compatibility.verdict != DeviceCompatibility.Verdict.SUPPORTED) {
                return@withContext WriteOutcome.DeviceUnsupported(compatibility.message)
            }
            runCatching {
                val uidHex = tag.id.joinToString("") { "%02X".format(it) }
                val key = CfsCodec.deriveSectorKey(uidHex)
                val blocks = CfsCodec.encryptPayload(payload)
                val mifare = requireNotNull(MifareClassic.get(tag)) { "MIFARE Classic unsupported" }
                try {
                    mifare.connect()
                    val authKey = if (tagIsAlreadySecured) key else CfsCodec.FACTORY_DEFAULT_KEY
                    require(mifare.authenticateSectorWithKeyA(1, authKey)) { "Sector 1 authentication failed" }

                    listOf(4, 5, 6).forEachIndexed { i, blockIndex ->
                        mifare.writeBlock(blockIndex, blocks[i])
                        require(mifare.readBlock(blockIndex).contentEquals(blocks[i])) {
                            "Read-back verification failed at block $blockIndex"
                        }
                    }

                    if (!tagIsAlreadySecured) {
                        val trailer = CfsCodec.buildSectorTrailer(key)
                        mifare.writeBlock(7, trailer)
                        // Re-authenticate with the newly-installed key to confirm the trailer took effect.
                        require(mifare.authenticateSectorWithKeyA(1, key)) {
                            "Sector trailer write did not take effect — re-authentication with the new key failed"
                        }
                    }

                    val decoded = CfsCodec.decryptBlocks(
                        mifare.readBlock(4), mifare.readBlock(5), mifare.readBlock(6)
                    )
                    require(CfsCodec.parse(decoded) == payload) { "Semantic verification failed after write" }
                    WriteOutcome.Success(payload) as WriteOutcome
                } finally { runCatching { mifare.close() } }
            }.getOrElse { e -> WriteOutcome.Failed(e.message ?: "Unknown write failure") }
        }
}
