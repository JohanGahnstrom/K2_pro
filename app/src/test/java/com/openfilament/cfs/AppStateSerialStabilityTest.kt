package com.openfilament.cfs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Regression test for a real bug: AppState.draft used to be a `get()`
 * property building a fresh SpoolDraft with serial defaulted to
 * UUID.randomUUID(), so every single read of `.draft` produced a
 * DIFFERENT random serial. The serial shown in Expert Mode's UI and the
 * one actually written to the tag came from two independent reads of
 * `.draft` and would practically never agree. Fixed by storing
 * currentSerial as an actual AppState field and threading it explicitly
 * into SpoolDraft.
 */
class AppStateSerialStabilityTest {

    @Test fun `reading draft twice from the same state returns the same serial`() {
        val state = AppState()
        assertEquals(state.draft.serial, state.draft.serial)
    }

    @Test fun `draft serial matches the state's currentSerial`() {
        val state = AppState()
        assertEquals(state.currentSerial, state.draft.serial)
    }

    @Test fun `copying state for an unrelated field preserves the serial`() {
        val state = AppState()
        val copied = state.copy(message = "unrelated update")
        assertEquals(state.currentSerial, copied.currentSerial)
    }

    @Test fun `two separately constructed states get different serials`() {
        // Not a hardcoded constant — each fresh AppState() gets its own random identity.
        assertNotEquals(AppState().currentSerial, AppState().currentSerial)
    }
}
