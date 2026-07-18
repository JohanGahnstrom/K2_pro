package com.openfilament.cfs

import com.openfilament.cfs.nfc.DeviceCompatibility
import com.openfilament.cfs.nfc.DeviceCompatibility.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * H-000 device-list regression test (HOLD_REGISTER.md). Guards the
 * Pixel 8/8 Pro Android-version gate and the bare-"Pixel" substring bug
 * (a naive `contains("PIXEL")` would wrongly mark every future,
 * unverified Pixel model SUPPORTED).
 */
class DeviceCompatibilityTest {

    @Test fun `pixel 8 on android 15 is supported`() =
        assertEquals(Verdict.SUPPORTED, DeviceCompatibility.assessDeviceModel(model = "Pixel 8", sdkInt = 35).verdict)

    @Test fun `pixel 8 pro on android 15 is supported`() =
        assertEquals(Verdict.SUPPORTED, DeviceCompatibility.assessDeviceModel(model = "Pixel 8 Pro", sdkInt = 35).verdict)

    @Test fun `pixel 8 below android 15 is unknown not unsupported`() =
        assertEquals(Verdict.UNKNOWN, DeviceCompatibility.assessDeviceModel(model = "Pixel 8", sdkInt = 34).verdict)

    @Test fun `pixel 8a is supported regardless of android version`() =
        assertEquals(Verdict.SUPPORTED, DeviceCompatibility.assessDeviceModel(model = "Pixel 8a", sdkInt = 30).verdict)

    @Test fun `original pixel is supported`() =
        assertEquals(Verdict.SUPPORTED, DeviceCompatibility.assessDeviceModel(model = "Pixel", sdkInt = 30).verdict)

    @Test fun `an unverified future pixel model is unknown, not supported`() =
        assertEquals(Verdict.UNKNOWN, DeviceCompatibility.assessDeviceModel(model = "Pixel 10", sdkInt = 40).verdict)

    @Test fun `pixel 9 pro is supported`() =
        assertEquals(Verdict.SUPPORTED, DeviceCompatibility.assessDeviceModel(model = "Pixel 9 Pro", sdkInt = 34).verdict)

    @Test fun `galaxy s25 model code is supported`() =
        assertEquals(Verdict.SUPPORTED, DeviceCompatibility.assessDeviceModel(model = "SM-S931B", sdkInt = 34).verdict)

    @Test fun `an unknown model is unknown`() =
        assertEquals(Verdict.UNKNOWN, DeviceCompatibility.assessDeviceModel(model = "SM-G960F", sdkInt = 30).verdict)
}
