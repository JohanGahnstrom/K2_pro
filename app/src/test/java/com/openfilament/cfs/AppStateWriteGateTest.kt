package com.openfilament.cfs

import com.openfilament.cfs.domain.FilamentColor
import com.openfilament.cfs.domain.FilamentProduct
import com.openfilament.cfs.domain.MappingConfidence
import com.openfilament.cfs.domain.UserMode
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FUNCTIONAL_DESCRIPTION.md §6/§18: "Simple Mode may not silently use
 * Experimental or Unsupported mappings." Guards AppState.writeBlockedReason,
 * which gates both the write button (TagScreen) and the actual write
 * (MainViewModel.beginTagScan/onTagDiscovered).
 */
class AppStateWriteGateTest {

    private fun productWith(confidence: MappingConfidence): FilamentProduct {
        val color = FilamentColor("Black", "#111111")
        return FilamentProduct(
            id = "test", brand = "TestBrand", line = "TestLine", material = "PLA",
            density = 1.24, defaultWeightG = 1000, colors = listOf(color),
            targetMaterialId = "00001", confidence = confidence, sourceUrl = "https://example.com",
        )
    }

    private fun stateWith(mode: UserMode, confidence: MappingConfidence): AppState {
        val product = productWith(confidence)
        return AppState(mode = mode, selectedProduct = product, selectedColor = product.colors.first())
    }

    @Test fun `reasonable mapping is never blocked`() {
        assertNull(stateWith(UserMode.SIMPLE, MappingConfidence.REASONABLE).writeBlockedReason)
        assertNull(stateWith(UserMode.EXPERT, MappingConfidence.REASONABLE).writeBlockedReason)
    }

    @Test fun `verified and strong mappings are never blocked`() {
        assertNull(stateWith(UserMode.SIMPLE, MappingConfidence.VERIFIED).writeBlockedReason)
        assertNull(stateWith(UserMode.SIMPLE, MappingConfidence.STRONG).writeBlockedReason)
    }

    @Test fun `experimental mapping is blocked in simple mode`() {
        assertNotNull(stateWith(UserMode.SIMPLE, MappingConfidence.EXPERIMENTAL).writeBlockedReason)
    }

    @Test fun `experimental mapping is allowed in expert mode`() {
        assertNull(stateWith(UserMode.EXPERT, MappingConfidence.EXPERIMENTAL).writeBlockedReason)
    }

    @Test fun `unsupported mapping is blocked in simple mode`() {
        assertNotNull(stateWith(UserMode.SIMPLE, MappingConfidence.UNSUPPORTED).writeBlockedReason)
    }

    @Test fun `unsupported mapping is blocked even in expert mode`() {
        assertNotNull(stateWith(UserMode.EXPERT, MappingConfidence.UNSUPPORTED).writeBlockedReason)
    }

    @Test fun `blocked reason names the product`() {
        val reason = stateWith(UserMode.SIMPLE, MappingConfidence.EXPERIMENTAL).writeBlockedReason
        assertTrue(reason!!.contains("TestBrand"))
        assertTrue(reason.contains("TestLine"))
    }
}
