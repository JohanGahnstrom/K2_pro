package com.openfilament.cfs.ui

import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.openfilament.cfs.AppState
import com.openfilament.cfs.MainViewModel
import com.openfilament.cfs.domain.*
import com.openfilament.cfs.nfc.DeviceCompatibility
import com.openfilament.cfs.nfc.WriteOutcome

private enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Default.Home), TAG("Tag", Icons.Default.Nfc), SPOOLS("Spools", Icons.Default.Inventory2), PRINTER("Printer", Icons.Default.Print), SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun OpenFilamentApp(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var showExpertDialog by remember { mutableStateOf(false) }
    var guideTopic by rememberSaveable { mutableStateOf<GuideTopic?>(null) }
    var showGuideHub by rememberSaveable { mutableStateOf(false) }

    if (showGuideHub || guideTopic != null) {
        GuideFlow(onExit = { showGuideHub = false; guideTopic = null }, startAt = guideTopic)
        return
    }

    // Bottom-nav tabs have no back stack of their own (just local state), so
    // without this, pressing system back from any non-Home tab exits the
    // whole app instead of returning to Home — surprising on a 5-tab root
    // screen. One back press goes to Home; a second exits, matching the
    // usual Android bottom-navigation convention.
    BackHandler(enabled = tab != Tab.HOME) { tab = Tab.HOME }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(tonalElevation = 0.dp) {
                Tab.entries.forEach { item ->
                    NavigationBarItem(selected = tab == item, onClick = { tab = item }, icon = { Icon(item.icon, null) }, label = { Text(item.label) })
                }
            }
        }
    ) { padding ->
        AnimatedContent(tab, label = "tab") { selected ->
            when (selected) {
                Tab.HOME -> HomeScreen(state, { tab = Tab.TAG }, { tab = Tab.PRINTER }, { showGuideHub = true })
                Tab.TAG -> TagScreen(state, viewModel, { guideTopic = GuideTopic.SCAN_WRITE })
                Tab.SPOOLS -> SpoolsScreen(state)
                Tab.PRINTER -> PrinterScreen(state, viewModel)
                Tab.SETTINGS -> SettingsScreen(state, onSimple = { viewModel.setMode(UserMode.SIMPLE) }, onExpert = { showExpertDialog = true })
            }
        }
        state.message?.let { message ->
            // This Snackbar is always-composed (not a SnackbarHostState-driven
            // one), so nothing auto-clears it without this — it would
            // otherwise sit on screen indefinitely until manually dismissed,
            // even for transient status text like "Connecting to printer…".
            LaunchedEffect(message) {
                kotlinx.coroutines.delay(4000)
                viewModel.clearMessage()
            }
            Snackbar(modifier = Modifier.padding(padding).padding(16.dp), action = { TextButton(onClick = viewModel::clearMessage) { Text("Dismiss") } }) { Text(message) }
        }
    }

    if (showExpertDialog) {
        AlertDialog(
            onDismissRequest = { showExpertDialog = false },
            icon = { Icon(Icons.Default.WarningAmber, null) },
            title = { Text("Enable Expert Mode?") },
            text = { Text("Expert Mode exposes protocol fields and printer controls, including M8200 load/unload commands. The stock printer display may not reflect commands issued from here. Incorrect mappings can still fail prints or confuse the CFS.") },
            confirmButton = { Button(onClick = { viewModel.setMode(UserMode.EXPERT); showExpertDialog = false }) { Text("I understand") } },
            dismissButton = { TextButton(onClick = { showExpertDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable private fun Screen(content: @Composable ColumnScope.() -> Unit) {
    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 116.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = { item { Column(verticalArrangement = Arrangement.spacedBy(16.dp), content = content) } })
}

@Composable private fun Header(kicker: String, title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(kicker.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp, fontSize = 12.sp)
        Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun DeviceCompatBanner(assessment: DeviceCompatibility.Assessment) {
    val (bg, icon) = when (assessment.verdict) {
        DeviceCompatibility.Verdict.SUPPORTED -> MaterialTheme.colorScheme.secondaryContainer to Icons.Default.CheckCircle
        DeviceCompatibility.Verdict.UNSUPPORTED -> MaterialTheme.colorScheme.errorContainer to Icons.Default.ErrorOutline
        DeviceCompatibility.Verdict.UNKNOWN -> MaterialTheme.colorScheme.surfaceVariant to Icons.AutoMirrored.Filled.HelpOutline
    }
    Card(colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, null); Spacer(Modifier.width(12.dp))
            Column { Text("Phone NFC compatibility", fontWeight = FontWeight.Black); Text(assessment.message) }
        }
    }
}

@Composable private fun HomeScreen(state: AppState, tag: () -> Unit, printer: () -> Unit, guide: () -> Unit) = Screen {
    Header(if (state.mode == UserMode.SIMPLE) "Simple mode" else "Expert mode", "Filament, finally effortless.", "Identify, tag and manage third-party filament without touching raw RFID data.")
    DeviceCompatBanner(state.deviceAssessment)
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))).padding(24.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = .22f), modifier = Modifier.size(56.dp)) { Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Nfc, null, Modifier.size(30.dp), tint = Color.Black) } }
                Spacer(Modifier.width(16.dp)); Column { Text("Ready to tag", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 24.sp); Text("MIFARE Classic 1K required", color = Color.Black.copy(alpha = .72f)) }
            }
            Button(onClick = tag, colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White), modifier = Modifier.fillMaxWidth().height(56.dp)) { Icon(Icons.Default.AutoAwesome, null); Spacer(Modifier.width(8.dp)); Text("Tag a spool") }
        }
    }
    SectionTitle("At a glance")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetricCard("${state.printer.slots.size}", "CFS slots seen", Icons.Default.Inventory2, Modifier.weight(1f))
        MetricCard(if (state.printer.online) "Online" else "Offline", "printer", Icons.Default.Print, Modifier.weight(1f))
    }
    ActionCard(Icons.Default.Link, "Connect your K2", "View CFS slots, toggle native auto-refill and monitor temperatures.", printer)
    ActionCard(Icons.AutoMirrored.Filled.HelpOutline, "Guides", "Step-by-step: scanning & writing tags, applying a tag to a spool, and reusing or moving one safely.", guide)
}

@Composable private fun TagScreen(state: AppState, vm: MainViewModel, guide: () -> Unit) = Screen {
    Header("Guided workflow", "Tag a spool", if (state.mode == UserMode.SIMPLE) "Three choices. No protocol fields." else "Full mapping visibility with the real, verified CFS codec.")
    TextButton(onClick = guide) { Icon(Icons.AutoMirrored.Filled.HelpOutline, null); Spacer(Modifier.width(6.dp)); Text("How do I actually do this?") }
    SectionTitle("1 · Filament")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(vm.products) { product -> ProductChip(product, state.selectedProduct.id == product.id) { vm.selectProduct(product) } }
    }
    DetailCard(state.selectedProduct)
    SectionTitle("2 · Colour")
    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(state.selectedProduct.colors) { color -> ColorChoice(color, state.selectedColor == color) { vm.selectColor(color) } }
    }
    SectionTitle("3 · Spool size")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(250, 500, 750, 1000, 2500).forEach { w -> FilterChip(selected = state.weightG == w, onClick = { vm.setWeight(w) }, label = { Text(if (w >= 1000) "${w / 1000.0} kg" else "$w g") }) }
    }
    SpoolPreview(state)
    if (state.mode == UserMode.EXPERT) ExpertFields(state)

    if (state.awaitingTagScan) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Nfc, null, Modifier.size(32.dp)); Spacer(Modifier.width(14.dp))
                Text("Hold a MIFARE Classic 1K tag against the back of your phone…", fontWeight = FontWeight.Bold)
            }
        }
        OutlinedButton(onClick = vm::cancelTagScan, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
    } else {
        val blockedReason = state.writeBlockedReason
        Button(onClick = vm::beginTagScan, enabled = blockedReason == null, modifier = Modifier.fillMaxWidth().height(60.dp)) { Icon(Icons.Default.Nfc, null); Spacer(Modifier.width(10.dp)); Text("Tap a tag to write") }
        if (blockedReason != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) { Icon(Icons.Default.ErrorOutline, null); Spacer(Modifier.width(12.dp)); Text(blockedReason) }
            }
        } else {
            Text("The write path uses a codec verified against a published golden vector (FUNCTIONAL_DESCRIPTION.md §8.4). Whether your specific phone's NFC hardware can perform the write is confirmed only once a tag is scanned — see the compatibility banner on Home.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    state.lastWriteOutcome?.let { outcome ->
        val (bg, text) = when (outcome) {
            is WriteOutcome.Success -> MaterialTheme.colorScheme.secondaryContainer to "Write verified: read-back matched exactly what was requested."
            is WriteOutcome.DeviceUnsupported -> MaterialTheme.colorScheme.errorContainer to outcome.reason
            is WriteOutcome.Failed -> MaterialTheme.colorScheme.errorContainer to "Write failed: ${outcome.reason}"
        }
        Card(colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(22.dp)) { Text(text, Modifier.padding(16.dp)) }
    }
}

@Composable private fun ProductChip(product: FilamentProduct, selected: Boolean, select: () -> Unit) {
    Surface(onClick = select, shape = RoundedCornerShape(22.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.width(170.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(product.brand, fontWeight = FontWeight.Black); Text(product.line, maxLines = 2, overflow = TextOverflow.Ellipsis); Text(product.material, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable private fun DetailCard(product: FilamentProduct) = Card(shape = RoundedCornerShape(24.dp)) {
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column { Text(product.brand, fontWeight = FontWeight.Bold); Text(product.line, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black) }; ConfidenceBadge(product.confidence) }
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { KeyValue("Material", product.material); KeyValue("Density", "${product.density} g/cm³"); KeyValue("Diameter", "${product.diameterMm} mm") }
        Text("Mapped to Creality code ${product.targetMaterialId} (${product.materialCodeName})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable private fun ColorChoice(color: FilamentColor, selected: Boolean, select: () -> Unit) {
    val parsed = remember(color.hex) { runCatching { Color(AndroidColor.parseColor(color.hex)) }.getOrDefault(Color.Gray) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(74.dp).clickable(onClick = select)) {
        Box(Modifier.size(if (selected) 58.dp else 50.dp).clip(CircleShape).background(parsed))
        Spacer(Modifier.height(7.dp)); Text(color.name, maxLines = 2, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable private fun SpoolPreview(state: AppState) {
    val target = remember(state.selectedColor.hex) { runCatching { Color(AndroidColor.parseColor(state.selectedColor.hex)) }.getOrDefault(Color.Gray) }
    val color by animateColorAsState(target, label = "spool")
    Card(shape = RoundedCornerShape(28.dp)) {
        Row(Modifier.fillMaxWidth().padding(22.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(92.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) { Box(Modifier.size(42.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)) }
            Spacer(Modifier.width(18.dp)); Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(state.selectedProduct.line, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text(state.selectedColor.name)
                Text("${state.weightG} g starting  •  ~${state.draft.nominalLengthM} m nominal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Mapped as ${state.selectedProduct.material}  •  live remaining will come from the printer, not this estimate", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable private fun ExpertFields(state: AppState) = Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), shape = RoundedCornerShape(24.dp)) {
    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Science, null); Spacer(Modifier.width(8.dp)); Text("Expert mapping", fontWeight = FontWeight.Black) }
        Text("Target material code: ${state.selectedProduct.targetMaterialId} (${state.selectedProduct.materialCodeName})")
        Text("Tag colour field: ${state.selectedColor.tagColorField}")
        Text("Nominal length: ${state.draft.nominalLengthM} m  •  tag length field: ${state.draft.tagLengthField}")
        Text("Generated serial: ${state.draft.serial}")
        Text("Codec: AES-128-ECB, verified against published golden vector", fontWeight = FontWeight.Bold)
        state.lastTagSecurityState.takeIf { it != TagSecurityState.UNKNOWN }?.let {
            Text("Last tag security state: ${it.name}")
        }
    }
}

@Composable private fun SpoolsScreen(state: AppState) = Screen {
    Header("Local inventory", "My spools", "Spool identity, remaining material and tag history stay local by default.")
    if (state.spools.isEmpty()) {
        EmptyState(Icons.Default.Inventory2, "No tagged spools yet", "Complete a verified tag write and the spool will appear here.")
    } else {
        state.spools.asReversed().forEach { spool -> TaggedSpoolCard(spool) }
    }
}

@Composable private fun TaggedSpoolCard(spool: TaggedSpool) {
    val formatted = remember(spool.taggedAt) {
        java.text.SimpleDateFormat("d MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(spool.taggedAt)
    }
    Card(shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(runCatching { Color(AndroidColor.parseColor(spool.color.hex)) }.getOrDefault(Color.Gray)))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("${spool.product.brand} ${spool.product.line}", fontWeight = FontWeight.Black)
                Text("${spool.color.name} · ${spool.weightG} g · serial ${spool.serial}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text("Tagged $formatted", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable private fun PrinterScreen(state: AppState, vm: MainViewModel) {
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    Screen {
        Header("Thin client", "Printer status", "Basic status and the native auto-refill toggle live here. Slot view, consumption and low-stock alerts are better handled by SpoolmanSync — see below.")
        OutlinedTextField(value = state.printerUrl, onValueChange = vm::setPrinterUrl, modifier = Modifier.fillMaxWidth(), label = { Text("Moonraker base URL") }, leadingIcon = { Icon(Icons.Default.Router, null) }, singleLine = true)
        Button(onClick = vm::probePrinter, modifier = Modifier.fillMaxWidth().height(54.dp)) { Icon(Icons.Default.WifiFind, null); Spacer(Modifier.width(8.dp)); Text("Probe printer") }
        Card(shape = RoundedCornerShape(28.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(12.dp).clip(CircleShape).background(if (state.printer.online) Color(0xFF36D67C) else Color(0xFFFF6B68))); Spacer(Modifier.width(10.dp)); Text(state.printer.state, fontWeight = FontWeight.Black, fontSize = 20.sp) }
                Text(state.printer.hostname, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.printer.state.equals("Printing", ignoreCase = true)) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        LinearProgressIndicator(progress = { state.printer.progress / 100f }, modifier = Modifier.fillMaxWidth())
                        Text("${state.printer.progress}% complete", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { TempTile("Nozzle", state.printer.nozzleC); TempTile("Bed", state.printer.bedC); TempTile("Chamber", state.printer.chamberC) }
            }
        }
        Card(shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.padding(18.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) { Text("Native auto-refill", fontWeight = FontWeight.Black); Text("Toggles the CFS's own same-material/colour relay. No matching logic runs in this app.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }
                Switch(checked = state.printer.autoRefillEnabled, onCheckedChange = vm::setAutoRefill)
            }
        }

        SectionTitle("Slots, consumption & alerts")
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("This app deliberately does not build a slot dashboard, consumption tracker or low-stock UI — SpoolmanSync already does this against real K1/K2/K2 Plus/Hi/Ender-3-V3-CFS hardware, is open source, and deploys in one command.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = state.spoolmanSyncUrl, onValueChange = vm::setSpoolmanSyncUrl, modifier = Modifier.fillMaxWidth(), label = { Text("SpoolmanSync URL") }, placeholder = { Text("http://192.168.1.100:3000") }, leadingIcon = { Icon(Icons.Default.Link, null) }, singleLine = true)
                Button(onClick = { runCatching { uriHandler.openUri(state.spoolmanSyncUrl) } }, enabled = state.spoolmanSyncUrl.isNotBlank(), modifier = Modifier.fillMaxWidth().height(50.dp)) { Icon(Icons.AutoMirrored.Filled.OpenInNew, null); Spacer(Modifier.width(8.dp)); Text("Open SpoolmanSync") }
            }
        }

        if (state.mode == UserMode.EXPERT && state.printer.slots.isNotEmpty()) {
            SectionTitle("Raw CFS slots (Expert diagnostics)")
            Text("For confirming K2 Pro's box-object field names against documentation sourced from K2 Plus (H-004) — not a substitute for SpoolmanSync.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            state.printer.slots.forEach { slot -> CfsSlotCard(slot) }
        }

        ActionCard(Icons.Default.CameraAlt, "Live camera", "Served over WebRTC:8000 on the printer once confirmed reachable — not hardcoded.", {})
    }
}

@Composable private fun CfsSlotCard(slot: CfsSlot) = Card(shape = RoundedCornerShape(20.dp)) {
    Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        slot.colorHex?.let { hex -> Box(Modifier.size(28.dp).clip(CircleShape).background(runCatching { Color(AndroidColor.parseColor(hex)) }.getOrDefault(Color.Gray))) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Slot T${slot.index} · ${slot.materialName}", fontWeight = FontWeight.Bold)
            Text("${slot.remainingLengthM?.let { "$it m remaining" } ?: "unknown remaining"} · ${slot.state}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

@Composable private fun SettingsScreen(state: AppState, onSimple: () -> Unit, onExpert: () -> Unit) = Screen {
    Header("Experience", "Settings", "Simple is plug-and-play. Expert exposes technical detail behind explicit warnings.")
    ModeCard("Simple Mode", "Automatic mappings, minimal choices and no raw printer commands.", Icons.Default.AutoAwesome, state.mode == UserMode.SIMPLE, onSimple)
    ModeCard("Expert Mode", "Protocol visibility, advanced mappings and capability-gated printer controls.", Icons.Default.Science, state.mode == UserMode.EXPERT, onExpert)
    SectionTitle("Privacy")
    ListItem(headlineContent = { Text("Local-first") }, supportingContent = { Text("No account, advertising or analytics in this source release.") }, leadingContent = { Icon(Icons.Default.Shield, null) })
    SectionTitle("About")
    Text("OpenFilament CFS 0.2.1-alpha01\nIndependent open-source project. Not affiliated with Creality.\nCFS codec ported and verified against a published golden vector — see THIRD_PARTY.yml.\nSlot/consumption tracking intentionally deferred to SpoolmanSync (see docs/FUNCTIONAL_DESCRIPTION.md §11.1).", color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable private fun ModeCard(title: String, text: String, icon: ImageVector, selected: Boolean, click: () -> Unit) = Card(onClick = click, shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(32.dp)); Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Black); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (selected) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary) }
}

@Composable private fun MetricCard(value: String, label: String, icon: ImageVector, modifier: Modifier = Modifier) = Card(modifier, shape = RoundedCornerShape(22.dp)) { Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(value, fontWeight = FontWeight.Black, fontSize = 22.sp); Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } }
@Composable private fun ActionCard(icon: ImageVector, title: String, text: String, click: () -> Unit) = Card(onClick = click, shape = RoundedCornerShape(24.dp)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(48.dp)) { Box(contentAlignment = Alignment.Center) { Icon(icon, null) } }; Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.Black); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Icon(Icons.Default.ChevronRight, null) } }
@Composable private fun EmptyState(icon: ImageVector, title: String, text: String) = Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) { Icon(icon, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary); Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp); Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
@Composable private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
@Composable private fun KeyValue(key: String, value: String) = Column { Text(key, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.Bold) }
@Composable private fun TempTile(label: String, value: Double?) = Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) { Column(Modifier.padding(12.dp)) { Text(label, fontSize = 11.sp); Text(value?.let { "${it.toInt()}°" } ?: "—", fontWeight = FontWeight.Black, fontSize = 20.sp) } }
@Composable private fun ConfidenceBadge(confidence: MappingConfidence) { val color = when(confidence) { MappingConfidence.VERIFIED -> Color(0xFF1B8F5A); MappingConfidence.STRONG -> Color(0xFF2C7BD9); MappingConfidence.REASONABLE -> Color(0xFFB66B00); else -> MaterialTheme.colorScheme.error }; Surface(shape = CircleShape, color = color.copy(alpha = .15f)) { Text(confidence.name.lowercase().replaceFirstChar { it.uppercase() }, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) } }
