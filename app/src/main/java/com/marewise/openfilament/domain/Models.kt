package com.marewise.openfilament.domain

import java.util.UUID

enum class UserMode { SIMPLE, EXPERT }
enum class ConnectionState { DISCONNECTED, CONNECTING, CONNECTED, ERROR }
enum class CameraKind { NONE, MJPEG, WEBRTC }
enum class PrinterCommand { PAUSE, RESUME, CANCEL, HOME, EMERGENCY_STOP }

data class PrinterEndpoint(
    val host: String,
    val moonrakerPort: Int = 7125,
    val crealityWsPort: Int = 9999
)

data class ManagedPrinter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val endpoint: PrinterEndpoint,
    val enabled: Boolean = true,
    val location: String = "",
    val notes: String = ""
)

data class Temperature(val actual: Double = 0.0, val target: Double = 0.0)

data class CfsSlot(
    val box: Int,
    val slot: Int,
    val materialCode: String?,
    val colorHex: String?,
    val remainMeters: Double?,
    val temperature: Double?,
    val humidity: Double?,
    val state: String?,
    val active: Boolean = false
)

data class PrinterCapabilities(
    val moonraker: Boolean = false,
    val boxObject: Boolean = false,
    val pauseResume: Boolean = false,
    val cancel: Boolean = false,
    val camera: CameraKind = CameraKind.NONE,
    val chamberTelemetry: Boolean = false,
    val cfsControlVerified: Boolean = false
)

data class PrinterSnapshot(
    val connection: ConnectionState = ConnectionState.DISCONNECTED,
    val model: String? = null,
    val status: String = "offline",
    val progress: Double = 0.0,
    val nozzle: Temperature = Temperature(),
    val bed: Temperature = Temperature(),
    val chamber: Temperature = Temperature(),
    val fileName: String? = null,
    val printDurationSeconds: Double? = null,
    val filamentUsedMm: Double? = null,
    val autoRefill: Boolean? = null,
    val slots: List<CfsSlot> = emptyList(),
    val cameraKind: CameraKind = CameraKind.NONE,
    val capabilities: PrinterCapabilities = PrinterCapabilities(),
    val lastUpdatedEpochMs: Long? = null,
    val lastError: String? = null
) {
    val isPrinting: Boolean get() = status.equals("printing", true)
    val hasAlarm: Boolean get() = connection == ConnectionState.ERROR || status.contains("error", true) || status.contains("shutdown", true)
}

data class ManagedPrinterState(
    val printer: ManagedPrinter,
    val snapshot: PrinterSnapshot = PrinterSnapshot()
)

data class FleetSummary(
    val total: Int = 0,
    val online: Int = 0,
    val printing: Int = 0,
    val alarms: Int = 0
)

data class SpoolDraft(
    val vendorId: String = "0276",
    val batch: String = "A2",
    val materialCode: String = "01001",
    val colorHex: String = "000000",
    val size: SpoolSize = SpoolSize.KG_1,
    val serial: String = "000001",
    val printerSuffix: String = "k2"
)

enum class SpoolSize(val grams: Int, val cfsLengthCode: String) {
    G_250(250, "0082"), G_500(500, "0165"), G_600(600, "0198"), G_750(750, "0247"), KG_1(1000, "0330")
}
