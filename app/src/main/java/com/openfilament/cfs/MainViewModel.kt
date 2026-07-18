package com.openfilament.cfs

import android.nfc.Tag
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openfilament.cfs.data.Catalogue
import com.openfilament.cfs.domain.*
import com.openfilament.cfs.nfc.CfsCodec
import com.openfilament.cfs.nfc.DeviceCompatibility
import com.openfilament.cfs.nfc.NfcTagService
import com.openfilament.cfs.nfc.WriteOutcome
import com.openfilament.cfs.printer.MoonrakerClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date

data class AppState(
    val mode: UserMode = UserMode.SIMPLE,
    val selectedProduct: FilamentProduct = Catalogue.products.first(),
    val selectedColor: FilamentColor = Catalogue.products.first().colors.first(),
    val weightG: Int = 1000,
    val printer: PrinterSnapshot = PrinterSnapshot(),
    val printerUrl: String = "http://192.168.1.100:7125",
    val spoolmanSyncUrl: String = "",
    val deviceAssessment: DeviceCompatibility.Assessment =
        DeviceCompatibility.assessDeviceModel(),
    val lastTagAssessment: DeviceCompatibility.Assessment? = null,
    val lastTagSecurityState: TagSecurityState = TagSecurityState.UNKNOWN,
    val awaitingTagScan: Boolean = false,
    val lastWriteOutcome: WriteOutcome? = null,
    val message: String? = null
) {
    val draft get() = SpoolDraft(selectedProduct, selectedColor, weightG)

    /**
     * FUNCTIONAL_DESCRIPTION.md §6/§18: "Simple Mode may not silently use
     * Experimental or Unsupported mappings." Null means the current
     * selection is allowed to write for the current mode; non-null is a
     * user-facing reason it's blocked. Checked both proactively (to disable
     * the write button before a wasted tag scan) and again right before the
     * actual write, since mode/selection could change between the two.
     * Unsupported is blocked in both modes; Experimental only in Simple.
     */
    val writeBlockedReason: String? get() = when {
        selectedProduct.confidence == MappingConfidence.UNSUPPORTED ->
            "${selectedProduct.brand} ${selectedProduct.line} has no safe Creality material-code mapping yet. Writing is blocked."
        mode == UserMode.SIMPLE && selectedProduct.confidence == MappingConfidence.EXPERIMENTAL ->
            "${selectedProduct.brand} ${selectedProduct.line}'s material-code mapping is Experimental. Switch to Expert Mode to write it anyway."
        else -> null
    }
}

/**
 * Builds the 5-char YYMDD date field. The golden vector only proves the
 * format for a single-digit month (date="24120" = 2024-01-20, month="1").
 * How months 10-12 are encoded into ONE character is UNVERIFIED — this uses
 * a hex-like fallback (A/B/C) as a reasonable guess, not a confirmed fact.
 * Confirm against real hardware before trusting tags written in Oct-Dec.
 */
private fun buildDateField(date: Date): String {
    val cal = java.util.Calendar.getInstance().apply { time = date }
    val yy = (cal.get(java.util.Calendar.YEAR) % 100).toString().padStart(2, '0')
    val month = cal.get(java.util.Calendar.MONTH) + 1
    val monthChar = if (month <= 9) ('0' + month) else ('A' + (month - 10))
    val dd = cal.get(java.util.Calendar.DAY_OF_MONTH).toString().padStart(2, '0')
    return "$yy$monthChar$dd"
}

class MainViewModel : ViewModel() {
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    val products = Catalogue.products
    private val moonraker = MoonrakerClient()
    private val nfcTagService = NfcTagService()

    fun setMode(mode: UserMode) { _state.value = _state.value.copy(mode = mode) }
    fun selectProduct(product: FilamentProduct) {
        _state.value = _state.value.copy(selectedProduct = product, selectedColor = product.colors.first(), weightG = product.defaultWeightG)
    }
    fun selectColor(color: FilamentColor) { _state.value = _state.value.copy(selectedColor = color) }
    fun setWeight(weight: Int) { _state.value = _state.value.copy(weightG = weight.coerceIn(50, 5000)) }
    fun setPrinterUrl(url: String) { _state.value = _state.value.copy(printerUrl = url) }
    fun setSpoolmanSyncUrl(url: String) { _state.value = _state.value.copy(spoolmanSyncUrl = url) }
    fun clearMessage() { _state.value = _state.value.copy(message = null) }

    fun beginTagScan() {
        val blockedReason = _state.value.writeBlockedReason
        _state.value = if (blockedReason != null) {
            _state.value.copy(message = blockedReason)
        } else {
            _state.value.copy(awaitingTagScan = true, message = "Hold the tag against your phone…")
        }
    }

    fun cancelTagScan() {
        _state.value = _state.value.copy(awaitingTagScan = false)
    }

    /**
     * Called from MainActivity.onNewIntent whenever the Android NFC dispatch
     * hands over ANY discovered tag — the foreground dispatch is active
     * app-wide (MainActivity.onResume), not just while the Tag screen is
     * open. Without the [AppState.awaitingTagScan] guard below, brushing the
     * phone against an unrelated MIFARE Classic tag on the Home, Spools,
     * Printer, or Settings screen would silently write whatever product/
     * colour/weight happens to be selected — never something the user asked
     * for. Only proceed with a write when the user explicitly started a scan
     * from the Tag screen (vm.beginTagScan()).
     */
    fun onTagDiscovered(tag: Tag) = viewModelScope.launch {
        if (!_state.value.awaitingTagScan) return@launch
        val probe = nfcTagService.probe(tag)
        _state.value = _state.value.copy(
            lastTagAssessment = probe.compatibility,
            lastTagSecurityState = probe.securityState,
            awaitingTagScan = false,
            message = probe.compatibility.message
        )
        if (probe.compatibility.verdict != DeviceCompatibility.Verdict.SUPPORTED) return@launch

        val blockedReason = _state.value.writeBlockedReason
        if (blockedReason != null) {
            _state.value = _state.value.copy(message = blockedReason)
            return@launch
        }
        writeTag(tag, probe.securityState == TagSecurityState.CFS_SECURED)
    }

    private fun writeTag(tag: Tag, tagIsAlreadySecured: Boolean) = viewModelScope.launch {
        val s = _state.value
        val draft = s.draft
        val payload = CfsCodec.Payload(
            batch = "1A5",
            date = buildDateField(Date()),
            supplier = "1B3D",
            material = s.selectedProduct.targetMaterialId.padStart(5, '0').take(5),
            color = s.selectedColor.tagColorField,
            length = draft.tagLengthField,
            serial = draft.serial.take(6).padEnd(6, '0')
        )
        val outcome = nfcTagService.writeVerified(tag, payload, tagIsAlreadySecured)
        _state.value = _state.value.copy(
            lastWriteOutcome = outcome,
            message = when (outcome) {
                is WriteOutcome.Success -> "Tag written and verified."
                is WriteOutcome.DeviceUnsupported -> outcome.reason
                is WriteOutcome.Failed -> "Write failed: ${outcome.reason}"
            }
        )
    }

    fun probePrinter() = viewModelScope.launch {
        _state.value = _state.value.copy(message = "Connecting to printer…")
        moonraker.probe(_state.value.printerUrl)
            .onSuccess { _state.value = _state.value.copy(printer = it, message = "Printer connected") }
            .onFailure { _state.value = _state.value.copy(printer = PrinterSnapshot(), message = it.message ?: "Connection failed") }
    }

    /** Toggles the CFS's own native auto-refill feature — no matching logic lives in this app (§12). */
    fun setAutoRefill(enabled: Boolean) = viewModelScope.launch {
        moonraker.setAutoRefill(_state.value.printerUrl, enabled)
            .onSuccess {
                _state.value = _state.value.copy(printer = _state.value.printer.copy(autoRefillEnabled = enabled))
            }
            .onFailure { _state.value = _state.value.copy(message = it.message ?: "Could not change auto-refill") }
    }
}
