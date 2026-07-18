package com.openfilament.cfs

import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.MifareClassic
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.openfilament.cfs.ui.OpenFilamentApp
import com.openfilament.cfs.ui.theme.OpenFilamentTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<MainViewModel>()
    private var nfcAdapter: NfcAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent { OpenFilamentTheme { OpenFilamentApp(viewModel) } }
        handleNfcIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        val adapter = nfcAdapter ?: return
        val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else 0
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)
        // ACTION_TAG_DISCOVERED is a required fallback, not an alternative: Android only
        // delivers ACTION_TECH_DISCOVERED when MifareClassic is in the tag's techList, so a
        // non-MIFARE tag (or a MIFARE-incapable phone/tag combo) would otherwise produce no
        // callback at all — silence, not the "clear, specific message" FUNCTIONAL_DESCRIPTION.md
        // §8.5 requires. DeviceCompatibility.assessTag() (called from NfcTagService.probe) does
        // its own techList inspection once ANY tag reaches onTagDiscovered, so it can produce
        // that message for every tag, not just ones already known to be MifareClassic-capable.
        val filters = arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED), IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED))
        val techLists = arrayOf(arrayOf(MifareClassic::class.java.name))
        adapter.enableForegroundDispatch(this, pendingIntent, filters, techLists)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent?) {
        if (intent?.action != NfcAdapter.ACTION_TECH_DISCOVERED && intent?.action != NfcAdapter.ACTION_TAG_DISCOVERED) return
        val tag: Tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
        } ?: return
        viewModel.onTagDiscovered(tag)
    }
}
