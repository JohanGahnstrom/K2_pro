package com.marewise.openfilament.printer

import com.marewise.openfilament.domain.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class PrinterFleetManager(
    private val registry: PrinterRegistry,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val clients = mutableMapOf<String, CrealityDirectClient>()
    private val _printers = MutableStateFlow(registry.load())
    val printers: StateFlow<List<ManagedPrinter>> = _printers.asStateFlow()
    private val _states = MutableStateFlow<List<ManagedPrinterState>>(emptyList())
    val states: StateFlow<List<ManagedPrinterState>> = _states.asStateFlow()
    val summary: StateFlow<FleetSummary> = states.map { list ->
        FleetSummary(
            total = list.size,
            online = list.count { it.snapshot.connection == ConnectionState.CONNECTED },
            printing = list.count { it.snapshot.isPrinting },
            alarms = list.count { it.snapshot.hasAlarm }
        )
    }.stateIn(scope, SharingStarted.Eagerly, FleetSummary())

    init { syncClients() }

    fun add(printer: ManagedPrinter) = replace(_printers.value + printer)
    fun update(printer: ManagedPrinter) = replace(_printers.value.map { if (it.id == printer.id) printer else it })
    fun remove(id: String) {
        clients.remove(id)?.disconnect()
        replace(_printers.value.filterNot { it.id == id })
    }
    fun connectAll() = clients.values.forEach { it.connect() }
    fun disconnectAll() = clients.values.forEach { it.disconnect() }
    fun refreshAll() = clients.values.forEach { it.refreshViaMoonraker() }
    fun command(id: String, command: PrinterCommand) = clients[id]?.sendCommand(command)
    fun refresh(id: String) = clients[id]?.refreshViaMoonraker()

    private fun replace(next: List<ManagedPrinter>) {
        _printers.value = next
        registry.save(next)
        syncClients()
    }

    private fun syncClients() {
        val activeIds = _printers.value.map { it.id }.toSet()
        clients.keys.filterNot(activeIds::contains).toList().forEach { clients.remove(it)?.disconnect() }
        _printers.value.forEach { printer ->
            val existing = clients[printer.id]
            if (existing == null || existing.endpoint != printer.endpoint) {
                existing?.disconnect()
                val client = CrealityDirectClient(printer.endpoint)
                clients[printer.id] = client
                scope.launch { client.state.collect { rebuildStates() } }
                if (printer.enabled) client.connect()
            }
        }
        rebuildStates()
    }

    private fun rebuildStates() {
        _states.value = _printers.value.map { ManagedPrinterState(it, clients[it.id]?.state?.value ?: PrinterSnapshot()) }
    }
}
