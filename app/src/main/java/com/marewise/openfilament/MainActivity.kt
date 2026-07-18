package com.marewise.openfilament

import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.marewise.openfilament.ui.OpenFilamentApp
import com.google.android.filament.Filament

class MainActivity:ComponentActivity(),NfcAdapter.ReaderCallback {
    private val vm:AppViewModel by viewModels()
    private var adapter:NfcAdapter?=null
    override fun onCreate(savedInstanceState:Bundle?){ super.onCreate(savedInstanceState); Filament.init(); enableEdgeToEdge(); adapter=NfcAdapter.getDefaultAdapter(this); setContent{ OpenFilamentApp(vm) } }
    override fun onResume(){ super.onResume(); adapter?.enableReaderMode(this,this,NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,Bundle().apply{putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY,250)}) }
    override fun onPause(){ adapter?.disableReaderMode(this); super.onPause() }
    override fun onTagDiscovered(tag:Tag){ runOnUiThread{vm.onTag(tag)} }
}
