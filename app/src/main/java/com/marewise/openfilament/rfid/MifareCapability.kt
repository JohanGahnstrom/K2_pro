package com.marewise.openfilament.rfid

import android.content.Context
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic

data class NfcCapability(val hasNfc: Boolean, val enabled: Boolean, val mifareClassicOnTag: Boolean? = null, val message: String)

object MifareCapability {
    fun device(context: Context): NfcCapability {
        val has = context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC)
        val adapter = NfcAdapter.getDefaultAdapter(context)
        return when {
            !has || adapter == null -> NfcCapability(false, false, null, "This phone has no NFC adapter")
            !adapter.isEnabled -> NfcCapability(true, false, null, "Enable NFC before scanning a spool tag")
            else -> NfcCapability(true, true, null, "Tap a MIFARE Classic 1K tag to verify controller support")
        }
    }

    fun tag(tag: Tag): NfcCapability {
        val mfc = MifareClassic.get(tag)
        return if (mfc == null) NfcCapability(true, true, false, "Tag was exposed only as NFC-A. This phone cannot authenticate MIFARE Classic tags.")
        else NfcCapability(true, true, true, "MIFARE Classic supported (${mfc.size} bytes)")
    }
}
