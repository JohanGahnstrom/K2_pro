package com.openfilament.cfs

import com.openfilament.cfs.data.Catalogue
import com.openfilament.cfs.data.decodeSpools
import com.openfilament.cfs.data.encodeSpools
import com.openfilament.cfs.domain.TaggedSpool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

/**
 * Guards SettingsStore's JSON spool-history persistence: only product id
 * and colour name are stored, looked back up against Catalogue.products
 * at decode time rather than duplicating the full FilamentProduct/
 * FilamentColor structure.
 */
class SpoolPersistenceTest {

    @Test fun `round trips a single spool`() {
        val product = Catalogue.products.first()
        val color = product.colors.first()
        val spool = TaggedSpool(product, color, 1000, "ABC123", Date(1_700_000_000_000))

        val decoded = decodeSpools(encodeSpools(listOf(spool)))

        assertEquals(1, decoded.size)
        assertEquals(spool, decoded.first())
    }

    @Test fun `round trips multiple spools preserving order`() {
        val a = Catalogue.products[0]
        val b = Catalogue.products[1]
        val spools = listOf(
            TaggedSpool(a, a.colors.first(), 1000, "AAA111", Date(1_000)),
            TaggedSpool(b, b.colors.first(), 500, "BBB222", Date(2_000)),
        )

        val decoded = decodeSpools(encodeSpools(spools))

        assertEquals(spools, decoded)
    }

    @Test fun `empty list round trips to empty list`() {
        assertTrue(decodeSpools(encodeSpools(emptyList())).isEmpty())
    }

    @Test fun `garbage json decodes to empty list rather than throwing`() {
        assertTrue(decodeSpools("not valid json").isEmpty())
    }

    @Test fun `a record referencing an unknown product id is skipped, not crashed on`() {
        val json = """[{"productId":"does-not-exist","colorName":"x","weightG":1000,"serial":"S","taggedAtMillis":1}]"""
        assertTrue(decodeSpools(json).isEmpty())
    }
}
