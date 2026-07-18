package com.marewise.openfilament.slicer

import com.marewise.openfilament.domain.PrintAsset
import com.marewise.openfilament.domain.SlicerServiceConfig

/**
 * Deliberately vendor-neutral slicing boundary.
 * A server adapter may wrap OrcaSlicer/PrusaSlicer CLI, but the Android app does not
 * claim to run their desktop C++ engines in-process.
 */
interface RemoteSlicerClient {
    suspend fun slice(
        source: PrintAsset,
        config: SlicerServiceConfig,
        onProgress: (Float, String) -> Unit
    ): Result<PrintAsset>
}

class DisabledRemoteSlicerClient : RemoteSlicerClient {
    override suspend fun slice(source: PrintAsset, config: SlicerServiceConfig, onProgress: (Float, String) -> Unit): Result<PrintAsset> =
        Result.failure(IllegalStateException("No slicing service is configured. Slice with OrcaSlicer/PrusaSlicer, or configure a compatible local service."))
}
