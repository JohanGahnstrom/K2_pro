package com.marewise.openfilament.rfid

import android.nfc.Tag
import android.nfc.tech.MifareClassic
import com.marewise.openfilament.domain.SpoolDraft

sealed interface TagWriteResult { data class Success(val uid: String, val payload: String): TagWriteResult; data class Failure(val reason: String): TagWriteResult }

/** Adapted from the DnG-Crafts blank-tag conversion and sector write sequence. */
class CfsTagWriter {
    fun write(tag: Tag, draft: SpoolDraft): TagWriteResult {
        val mfc = MifareClassic.get(tag) ?: return TagWriteResult.Failure("MIFARE Classic is not supported by this phone/tag")
        if (mfc.type != MifareClassic.TYPE_CLASSIC || mfc.size < MifareClassic.SIZE_1K) return TagWriteResult.Failure("A MIFARE Classic 1K tag is required")
        val uid = tag.id
        val key = CfsCodec.deriveSectorKey(uid)
        val encrypted = CfsCodec.encodePayload(draft)
        return try {
            mfc.connect()
            val alreadySecured = mfc.authenticateSectorWithKeyA(1, key) || mfc.authenticateSectorWithKeyB(1, key)
            val defaultAuth = if (!alreadySecured) mfc.authenticateSectorWithKeyA(1, MifareClassic.KEY_DEFAULT) else false
            if (!alreadySecured && !defaultAuth) return TagWriteResult.Failure("Sector 1 authentication failed")
            for (i in 0..2) mfc.writeBlock(4 + i, encrypted.copyOfRange(i*16, i*16+16))
            if (!alreadySecured) {
                val trailer = mfc.readBlock(7)
                key.copyInto(trailer, 0); key.copyInto(trailer, 10)
                mfc.writeBlock(7, trailer)
            }
            mfc.close(); mfc.connect()
            if (!(mfc.authenticateSectorWithKeyA(1,key) || mfc.authenticateSectorWithKeyB(1,key))) return TagWriteResult.Failure("Read-back authentication failed")
            val readback = ByteArray(48)
            for (i in 0..2) mfc.readBlock(4+i).copyInto(readback, i*16)
            if (!readback.contentEquals(encrypted)) TagWriteResult.Failure("Read-back mismatch")
            else TagWriteResult.Success(uid.joinToString("") { "%02X".format(it) }, CfsCodec.decodePayload(readback))
        } catch (e: Exception) { TagWriteResult.Failure(e.message ?: e::class.simpleName.orEmpty()) }
        finally { runCatching { mfc.close() } }
    }
}
