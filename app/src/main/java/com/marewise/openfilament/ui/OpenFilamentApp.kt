package com.marewise.openfilament.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.marewise.openfilament.AppViewModel
import com.marewise.openfilament.domain.*
import com.marewise.openfilament.rfid.CfsCodec

@Composable
fun OpenFilamentApp(vm: AppViewModel) {
    MaterialTheme {
        var tab by remember { mutableIntStateOf(0) }
        Scaffold(bottomBar = {
            NavigationBar {
                listOf("Fleet" to Icons.Outlined.Dashboard, "Printers" to Icons.Outlined.Print, "Models" to Icons.Outlined.CloudDownload, "Write tag" to Icons.Outlined.Nfc, "Settings" to Icons.Outlined.Settings).forEachIndexed { i, item ->
                    NavigationBarItem(tab == i, { tab = i }, { Icon(item.second, null) }, label = { Text(item.first) })
                }
            }
        }) { pad ->
            Box(Modifier.padding(pad).fillMaxSize().background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant)))) {
                when(tab) { 0 -> FleetScreen(vm); 1 -> PrintersScreen(vm); 2 -> ModelsScreen(vm); 3 -> TagScreen(vm); else -> SettingsScreen(vm) }
            }
        }
    }
}

@Composable private fun Header(title:String, subtitle:String)=Column(Modifier.padding(20.dp)){Text(title,style=MaterialTheme.typography.headlineMedium);Text(subtitle,color=MaterialTheme.colorScheme.onSurfaceVariant)}
@Composable private fun AppCard(content:@Composable ColumnScope.()->Unit)=Card(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),shape=RoundedCornerShape(24.dp)){Column(Modifier.padding(18.dp),content=content)}

@Composable
private fun FleetScreen(vm:AppViewModel){
    val states by vm.fleet.states.collectAsStateWithLifecycle(); val sum by vm.fleet.summary.collectAsStateWithLifecycle()
    Column(Modifier.verticalScroll(rememberScrollState())){
        Header("K2 Pro fleet","Direct management of multiple printers on the local network")
        AppCard { Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween){Metric("Printers",sum.total);Metric("Online",sum.online);Metric("Printing",sum.printing);Metric("Alarms",sum.alarms)} }
        if(states.isEmpty()) AppCard { Text("No printers configured"); Text("Add the first K2 Pro in Printers.") }
        states.forEach { item ->
            AppCard {
                Row(verticalAlignment=Alignment.CenterVertically){
                    Icon(if(item.snapshot.hasAlarm) Icons.Outlined.Warning else if(item.snapshot.connection==ConnectionState.CONNECTED) Icons.Outlined.CheckCircle else Icons.Outlined.CloudOff,null)
                    Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)){Text(item.printer.name,style=MaterialTheme.typography.titleLarge);Text("${item.printer.endpoint.host} • ${item.snapshot.status}")}
                    Text("${(item.snapshot.progress*100).toInt()}%")
                }
                LinearProgressIndicator(progress={item.snapshot.progress.toFloat().coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().padding(vertical=10.dp))
                Text("Nozzle ${item.snapshot.nozzle.actual.toInt()}° / ${item.snapshot.nozzle.target.toInt()}°   Bed ${item.snapshot.bed.actual.toInt()}° / ${item.snapshot.bed.target.toInt()}°")
                Text("CFS slots ${item.snapshot.slots.size} • Auto-refill ${item.snapshot.autoRefill?.toString() ?: "unknown"}")
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.padding(top=10.dp)){
                    OutlinedButton({vm.selectPrinter(item.printer.id)}){Text("Open")}
                    OutlinedButton({vm.refreshPrinter(item.printer.id)}){Text("Refresh")}
                }
            }
        }
    }
}

@Composable private fun Metric(label:String,value:Int)=Column(horizontalAlignment=Alignment.CenterHorizontally){Text(value.toString(),style=MaterialTheme.typography.headlineSmall);Text(label,style=MaterialTheme.typography.labelMedium)}

@Composable
private fun PrintersScreen(vm:AppViewModel){
    val states by vm.fleet.states.collectAsStateWithLifecycle(); val selected by vm.selectedPrinterId.collectAsStateWithLifecycle()
    var add by remember{ mutableStateOf(false)}
    Column(Modifier.verticalScroll(rememberScrollState())){
        Header("Printers","Add, inspect and control each K2 Pro independently")
        Button({add=true},Modifier.padding(horizontal=16.dp)){Icon(Icons.Outlined.Add,null);Spacer(Modifier.width(6.dp));Text("Add K2 Pro")}
        val chosen=states.firstOrNull{it.printer.id==selected}
        if(chosen!=null) PrinterDetail(chosen,vm)
        states.forEach{item->
            AppCard { Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(item.printer.name,style=MaterialTheme.typography.titleMedium);Text(item.printer.endpoint.host)};OutlinedButton({vm.selectPrinter(item.printer.id)}){Text("Manage")};IconButton({vm.removePrinter(item.printer.id)}){Icon(Icons.Outlined.Delete,null)}} }
        }
    }
    if(add) AddPrinterDialog({add=false}){n,h,l->vm.addPrinter(n,h,l);add=false}
}

@Composable
private fun PrinterDetail(item:ManagedPrinterState,vm:AppViewModel){
    val s=item.snapshot
    AppCard {
        Text(item.printer.name,style=MaterialTheme.typography.headlineSmall)
        Text("${s.model ?: "K2 Pro"} • ${s.connection} • ${s.status}")
        s.fileName?.let{Text(it,modifier=Modifier.padding(top=6.dp))}
        Row(Modifier.padding(top=12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            Button({vm.sendCommand(item.printer.id,PrinterCommand.PAUSE)},enabled=s.isPrinting){Text("Pause")}
            Button({vm.sendCommand(item.printer.id,PrinterCommand.RESUME)},enabled=s.status.equals("paused",true)){Text("Resume")}
            OutlinedButton({vm.sendCommand(item.printer.id,PrinterCommand.CANCEL)},enabled=s.isPrinting||s.status.equals("paused",true)){Text("Cancel")}
        }
        HorizontalDivider(Modifier.padding(vertical=12.dp))
        Text("Temperatures",style=MaterialTheme.typography.titleMedium)
        Text("Nozzle ${s.nozzle.actual.toInt()} / ${s.nozzle.target.toInt()} °C")
        Text("Bed ${s.bed.actual.toInt()} / ${s.bed.target.toInt()} °C")
        Text("Chamber ${s.chamber.actual.toInt()} / ${s.chamber.target.toInt()} °C")
        if(s.slots.isNotEmpty()){
            HorizontalDivider(Modifier.padding(vertical=12.dp));Text("CFS",style=MaterialTheme.typography.titleMedium)
            s.slots.forEach{slot->Text("Box ${slot.box} slot ${slot.slot}: ${slot.materialCode ?: "—"}, ${slot.remainMeters?.toInt() ?: 0} m${if(slot.active) " • active" else ""}")}
        }
        s.lastError?.let{Text(it,color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(top=8.dp))}
    }
}

@Composable
private fun AddPrinterDialog(close:()->Unit,add:(String,String,String)->Unit){var name by remember{mutableStateOf("K2 Pro")};var host by remember{mutableStateOf("")};var loc by remember{mutableStateOf("")};AlertDialog(close,{Button({add(name,host,loc)},enabled=host.isNotBlank()){Text("Add")}},dismissButton={TextButton(close){Text("Cancel")}},title={Text("Add K2 Pro")},text={Column{OutlinedTextField(name,{name=it},label={Text("Name")});OutlinedTextField(host,{host=it},label={Text("IP or hostname")});OutlinedTextField(loc,{loc=it},label={Text("Location")})}})}


@Composable
private fun ModelsScreen(vm: AppViewModel) {
    val transfer by vm.printTransfer.collectAsStateWithLifecycle()
    val printers by vm.fleet.states.collectAsStateWithLifecycle()
    val selected by vm.selectedPrinterId.collectAsStateWithLifecycle()
    var url by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(vm::importPrintDocument)
    }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Header("Models & print files", "Import or download, select a K2 Pro, then upload or start")
        AppCard {
            Text("What the printer can run", style = MaterialTheme.typography.titleLarge)
            Text("K2 Pro runs sliced G-code. STL, OBJ, 3MF and GLB can be stored and inspected, but must be sliced before printing.")
        }
        AppCard {
            Text("Add a file", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                Button({ picker.launch(arrayOf("application/octet-stream", "model/stl", "model/gltf-binary", "*/*")) }) {
                    Icon(Icons.Outlined.FolderOpen, null); Spacer(Modifier.width(6.dp)); Text("Choose file")
                }
            }
            OutlinedTextField(url, { url = it }, label = { Text("Direct model or G-code URL") }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp))
            Button({ vm.downloadPrintAsset(url.trim()) }, enabled = url.startsWith("http://") || url.startsWith("https://"), modifier = Modifier.padding(top = 8.dp)) {
                Icon(Icons.Outlined.Download, null); Spacer(Modifier.width(6.dp)); Text("Download")
            }
        }
        AppCard {
            Text("Destination printer", style = MaterialTheme.typography.titleLarge)
            if (printers.isEmpty()) Text("Add a K2 Pro first")
            else printers.forEach { item ->
                FilterChip(
                    selected = selected == item.printer.id,
                    onClick = { vm.selectPrinter(item.printer.id) },
                    label = { Text(item.printer.name) },
                    modifier = Modifier.padding(end = 8.dp, top = 8.dp)
                )
            }
        }
        AppCard {
            Text(transfer.message, style = MaterialTheme.typography.titleMedium)
            transfer.asset?.let { a ->
                Text(a.displayName, modifier = Modifier.padding(top = 6.dp))
                Text("${a.kind} • ${formatBytes(a.sizeBytes)}")
                Text("SHA-256 ${a.sha256?.take(16)}…", style = MaterialTheme.typography.bodySmall)
                if (a.isPreviewable) {
                    Text("Interactive GLB preview", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                    FilamentModelPreview(a.localPath)
                }
                if (a.kind == PrintAssetKind.GLTF) {
                    Text("Standalone .gltf may reference external buffers and textures. Package it as GLB for reliable mobile preview.", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.padding(top = 8.dp))
                }
                a.gcode?.let { meta ->
                    HorizontalDivider(Modifier.padding(vertical = 10.dp))
                    Text("G-code analysis", style = MaterialTheme.typography.titleSmall)
                    meta.slicer?.let { Text("Slicer: $it") }
                    meta.estimatedSeconds?.let { Text("Estimated time: ${formatDuration(it)}") }
                    meta.filamentGrams?.let { Text("Filament: %.1f g".format(it)) }
                    meta.filamentMillimetres?.let { Text("Filament length: %.1f m".format(it / 1000.0)) }
                    meta.layerHeightMm?.let { Text("Layer height: $it mm") }
                    meta.nozzleDiameterMm?.let { Text("Nozzle: $it mm") }
                    meta.material?.let { Text("Material/profile: $it") }
                    meta.printerModel?.let { Text("Printer profile: $it") }
                    if (meta.thumbnailAvailable) Text("Embedded thumbnail detected")
                }
                if (a.needsSlicing) Text("This is source geometry, not printer-ready G-code.", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
            if (transfer.state == TransferState.DOWNLOADING || transfer.state == TransferState.UPLOADING) {
                LinearProgressIndicator(progress = { transfer.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                Button({ vm.uploadSelectedAsset(false) }, enabled = transfer.asset?.isPrintable == true && selected != null && transfer.state !in setOf(TransferState.DOWNLOADING, TransferState.UPLOADING)) { Text("Upload") }
                Button({ vm.uploadSelectedAsset(true) }, enabled = transfer.asset?.isPrintable == true && selected != null && transfer.state !in setOf(TransferState.DOWNLOADING, TransferState.UPLOADING)) { Text("Upload & print") }
                TextButton(vm::clearPrintAsset, enabled = transfer.asset != null) { Text("Clear") }
            }
        }
        AppCard {
            Text("Slicing", style = MaterialTheme.typography.titleLarge)
            Text("Slice STL/3MF/OBJ in OrcaSlicer or PrusaSlicer using a validated K2 Pro profile, then import the G-code. The codebase now has a vendor-neutral remote-slicer interface for a future local OrcaSlicer/PrusaSlicer service; no desktop slicing engine is falsely embedded in Android.")
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000_000 -> "%.2f GB".format(bytes / 1_000_000_000.0)
    bytes >= 1_000_000 -> "%.2f MB".format(bytes / 1_000_000.0)
    bytes >= 1_000 -> "%.1f kB".format(bytes / 1_000.0)
    else -> "$bytes B"
}

@Composable
private fun TagScreen(vm:AppViewModel){
    val d by vm.draft.collectAsStateWithLifecycle();val n by vm.nfc.collectAsStateWithLifecycle();val msg by vm.tagMessage.collectAsStateWithLifecycle();val mode by vm.mode.collectAsStateWithLifecycle()
    Column(Modifier.verticalScroll(rememberScrollState())){Header("Write CFS tag","Create and verify a spool identity for any managed printer")
        AppCard { Text(if(n.enabled)"Phone ready" else "NFC unavailable",style=MaterialTheme.typography.titleMedium);Text(n.message) }
        AppCard { OutlinedTextField(d.materialCode,{vm.draft.value=d.copy(materialCode=it.filter(Char::isDigit).take(5))},label={Text("Material code")},modifier=Modifier.fillMaxWidth());OutlinedTextField(d.colorHex,{vm.draft.value=d.copy(colorHex=it.filter(Char::isLetterOrDigit).uppercase().take(6))},label={Text("Colour")},modifier=Modifier.fillMaxWidth());OutlinedTextField(d.serial,{vm.draft.value=d.copy(serial=it.filter(Char::isDigit).take(6))},label={Text("Spool identity")},modifier=Modifier.fillMaxWidth());FlowRow{SpoolSize.entries.forEach{z->FilterChip(d.size==z,{vm.draft.value=d.copy(size=z)},{Text("${z.grams} g")})}} }
        AppCard { Text(msg);if(mode==UserMode.EXPERT){HorizontalDivider(Modifier.padding(vertical=8.dp));Text(CfsCodec.buildPlainPayload(d),style=MaterialTheme.typography.bodySmall)} }
    }
}

@Composable
private fun SettingsScreen(vm:AppViewModel){val mode by vm.mode.collectAsStateWithLifecycle();Column(Modifier.verticalScroll(rememberScrollState())){Header("Settings","Fleet behaviour and expert access");AppCard{Text("Mode",style=MaterialTheme.typography.titleLarge);Row{FilterChip(mode==UserMode.SIMPLE,{vm.mode.value=UserMode.SIMPLE},{Text("Simple")});Spacer(Modifier.width(8.dp));FilterChip(mode==UserMode.EXPERT,{vm.mode.value=UserMode.EXPERT},{Text("Expert")})}};AppCard{Text("Safety boundary",style=MaterialTheme.typography.titleLarge);Text("Pause, resume and cancel use documented Moonraker APIs. CFS load/unload, heater changes and raw G-code remain disabled until K2 Pro capability verification. Emergency stop is intentionally not placed in the normal UI.")};AppCard{Text("Inventory boundary",style=MaterialTheme.typography.titleLarge);Text("Spoolman or SpoolmanSync remains authoritative for inventory and deductions. This app manages printers and writes tags without duplicating their accounting engines.")}}
}
