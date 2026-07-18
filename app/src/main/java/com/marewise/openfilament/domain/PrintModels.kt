package com.marewise.openfilament.domain

enum class PrintAssetKind { GCODE, STL, OBJ, THREE_MF, GLB, GLTF, UNKNOWN }
enum class TransferState { IDLE, DOWNLOADING, ANALYSING, READY, UPLOADING, COMPLETE, ERROR }

data class GCodeMetadata(
    val slicer: String? = null,
    val estimatedSeconds: Long? = null,
    val filamentMillimetres: Double? = null,
    val filamentGrams: Double? = null,
    val layerHeightMm: Double? = null,
    val nozzleDiameterMm: Double? = null,
    val material: String? = null,
    val printerModel: String? = null,
    val thumbnailAvailable: Boolean = false
)

data class PrintAsset(
    val displayName: String,
    val localPath: String,
    val kind: PrintAssetKind,
    val sizeBytes: Long,
    val source: String? = null,
    val sha256: String? = null,
    val gcode: GCodeMetadata? = null
) {
    val isPrintable: Boolean get() = kind == PrintAssetKind.GCODE
    val isPreviewable: Boolean get() = kind == PrintAssetKind.GLB
    val needsSlicing: Boolean get() = kind in setOf(PrintAssetKind.STL, PrintAssetKind.OBJ, PrintAssetKind.THREE_MF, PrintAssetKind.GLB, PrintAssetKind.GLTF)
}

data class PrintTransferState(
    val state: TransferState = TransferState.IDLE,
    val progress: Float = 0f,
    val message: String = "Choose a model or G-code file",
    val asset: PrintAsset? = null,
    val selectedPrinterId: String? = null
)

data class SlicerServiceConfig(
    val enabled: Boolean = false,
    val baseUrl: String = "",
    val apiToken: String = "",
    val profileId: String = ""
)

fun assetKind(name: String): PrintAssetKind = when (name.substringAfterLast('.', "").lowercase()) {
    "gcode", "gco", "gc" -> PrintAssetKind.GCODE
    "stl" -> PrintAssetKind.STL
    "obj" -> PrintAssetKind.OBJ
    "3mf" -> PrintAssetKind.THREE_MF
    "glb" -> PrintAssetKind.GLB
    "gltf" -> PrintAssetKind.GLTF
    else -> PrintAssetKind.UNKNOWN
}
