package com.marewise.openfilament

import android.app.Application
import android.nfc.Tag
import androidx.lifecycle.AndroidViewModel
import com.marewise.openfilament.domain.*
import com.marewise.openfilament.printer.PrinterFleetManager
import com.marewise.openfilament.printer.PrinterRegistry
import com.marewise.openfilament.rfid.*
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import android.net.Uri
import com.marewise.openfilament.printer.PrintFileManager
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val mode = MutableStateFlow(UserMode.SIMPLE)
    val draft = MutableStateFlow(SpoolDraft())
    val nfc = MutableStateFlow(MifareCapability.device(app))
    val tagMessage = MutableStateFlow("Choose the filament, then tap a blank CFS tag")
    val lastWrite = MutableStateFlow<TagWriteResult.Success?>(null)

    val fleet = PrinterFleetManager(PrinterRegistry(app))
    val selectedPrinterId = MutableStateFlow<String?>(null)
    val printTransfer = MutableStateFlow(PrintTransferState())
    private val printFiles = PrintFileManager(app)

    fun addPrinter(name: String, host: String, location: String = "") {
        val cleanHost = host.trim().removePrefix("http://").removePrefix("https://").substringBefore('/')
        if (name.isBlank() || cleanHost.isBlank()) return
        val printer = ManagedPrinter(name = name.trim(), endpoint = PrinterEndpoint(cleanHost), location = location.trim())
        fleet.add(printer)
        selectedPrinterId.value = printer.id
    }

    fun updatePrinter(printer: ManagedPrinter) = fleet.update(printer)
    fun removePrinter(id: String) {
        fleet.remove(id)
        if (selectedPrinterId.value == id) selectedPrinterId.value = null
    }
    fun selectPrinter(id: String) { selectedPrinterId.value = id }
    fun refreshPrinter(id: String) = fleet.refresh(id)
    fun sendCommand(id: String, command: PrinterCommand) = fleet.command(id, command)


    fun importPrintDocument(uri: Uri) {
        viewModelScope.launch {
            printTransfer.value = PrintTransferState(TransferState.DOWNLOADING, message = "Importing file…", selectedPrinterId = selectedPrinterId.value)
            runCatching { printFiles.importDocument(uri) }
                .onSuccess { printTransfer.value = PrintTransferState(TransferState.READY, 1f, if (it.isPrintable) "Ready to upload" else "Model imported; slicing required before printing", it, selectedPrinterId.value) }
                .onFailure { printTransfer.value = PrintTransferState(TransferState.ERROR, message = it.message ?: "Import failed", selectedPrinterId = selectedPrinterId.value) }
        }
    }

    fun downloadPrintAsset(url: String) {
        viewModelScope.launch {
            printTransfer.value = PrintTransferState(TransferState.DOWNLOADING, message = "Downloading…", selectedPrinterId = selectedPrinterId.value)
            runCatching { printFiles.download(url) { p -> printTransfer.value = printTransfer.value.copy(progress = p) } }
                .onSuccess { printTransfer.value = PrintTransferState(TransferState.READY, 1f, if (it.isPrintable) "Ready to upload" else "Downloaded model; slicing required before printing", it, selectedPrinterId.value) }
                .onFailure { printTransfer.value = PrintTransferState(TransferState.ERROR, message = it.message ?: "Download failed", selectedPrinterId = selectedPrinterId.value) }
        }
    }

    fun uploadSelectedAsset(startPrint: Boolean) {
        val asset = printTransfer.value.asset ?: return
        val id = selectedPrinterId.value ?: run { printTransfer.value = printTransfer.value.copy(state = TransferState.ERROR, message = "Select a printer first"); return }
        val printer = fleet.states.value.firstOrNull { it.printer.id == id }?.printer ?: return
        viewModelScope.launch {
            printTransfer.value = printTransfer.value.copy(state = TransferState.UPLOADING, progress = 0f, message = if (startPrint) "Uploading and starting…" else "Uploading…")
            printFiles.upload(printer.endpoint, asset, startPrint) { p -> printTransfer.value = printTransfer.value.copy(progress = p) }
                .onSuccess { printTransfer.value = printTransfer.value.copy(state = TransferState.COMPLETE, progress = 1f, message = if (startPrint) "Uploaded and print requested on ${printer.name}" else "Uploaded to ${printer.name}") }
                .onFailure { printTransfer.value = printTransfer.value.copy(state = TransferState.ERROR, message = it.message ?: "Upload failed") }
        }
    }

    fun clearPrintAsset() { printTransfer.value = PrintTransferState(selectedPrinterId = selectedPrinterId.value) }

    fun onTag(tag: Tag) {
        nfc.value = MifareCapability.tag(tag)
        if (nfc.value.mifareClassicOnTag != true) {
            tagMessage.value = "This phone exposed the tag without MIFARE Classic write support"
            return
        }
        tagMessage.value = when (val result = CfsTagWriter().write(tag, draft.value)) {
            is TagWriteResult.Success -> {
                lastWrite.value = result
                "Written and verified • UID ${result.uid}"
            }
            is TagWriteResult.Failure -> "Write failed: ${result.reason}"
        }
    }
}
