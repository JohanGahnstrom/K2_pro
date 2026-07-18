package com.openfilament.cfs

import com.openfilament.cfs.domain.FilamentMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * NOTE: this formula computes a NOMINAL STARTING quantity only (the spool's
 * declared weight at tagging time), never live remaining material. Live
 * remaining quantity comes from box.remain_len or Moonraker's Spoolman
 * integration when a printer is connected — see FUNCTIONAL_DESCRIPTION.md §7.
 */
class FilamentMathTest {
    @Test fun oneKgPetgIsAbout327m() = assertEquals(327, FilamentMath.lengthMeters(1000, 1.27, 1.75))
    @Test fun oneKgPlaIsAbout335m() = assertEquals(335, FilamentMath.lengthMeters(1000, 1.24, 1.75))
    @Test fun rejectsZeroDensity() { assertThrows(IllegalArgumentException::class.java) { FilamentMath.lengthMeters(1000, 0.0, 1.75) } }
}
