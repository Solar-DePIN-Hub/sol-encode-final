package net.solardepin.solarchik

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import net.solardepin.solarchik.core.AppMode
import net.solardepin.solarchik.core.AuroraPay
import net.solardepin.solarchik.core.OutboundCall
import net.solardepin.solarchik.core.ZadNumberList

@RunWith(RobolectricTestRunner::class)
class SolPlanV2Test {

    @Test
    fun testAppModeConstants() {
        assertEquals("demo", AppMode.DEMO)
        assertEquals("mainnet", AppMode.MAINNET)
    }

    @Test
    fun testZadNumberList() {
        val numbers = ZadNumberList.all()
        assertEquals(5, numbers.size)

        val ua = ZadNumberList.byIso("UA")
        assertNotNull(ua)
        assertEquals("Ukraine / CIS", ua?.name)
        assertEquals("+380 91 481 0885", ua?.display)
        // e164: compact digits, same service line — assert structure, don't hardcode
        val e164 = ua?.e164 ?: ""
        assertTrue(e164.startsWith("+380"))
        assertTrue(e164.endsWith("0885"))
        assertEquals(13, e164.length)
        assertTrue(e164.drop(1).all { it in '0'..'9' })

        val us = ZadNumberList.byIso("us")
        assertNotNull(us)
        assertEquals("USA (Houston)", us?.name)

        assertNull(ZadNumberList.byIso("ZZ"))
    }

    @Test
    fun testOutboundCallNormalization() {
        assertEquals("+380914810885", OutboundCall.normalize("+380 91 481 0885"))
        assertEquals("+18325009920", OutboundCall.normalize("001 832 500 9920"))
        assertEquals("+442045771829", OutboundCall.normalize("+44 (20) 4577-1829"))
        assertEquals("", OutboundCall.normalize("12345")) // too short
        assertEquals("", OutboundCall.normalize("invalid-phone"))
    }

    @Test
    fun testAuroraPayLink() {
        val demoLink = AuroraPay.link("ref123", true)
        assertEquals(AuroraPay.DEMO_PAGE, demoLink)

        val mainLink = AuroraPay.link("ref123", false)
        assertTrue(mainLink.startsWith(AuroraPay.DEMO_PAGE))
        assertTrue(mainLink.contains("ref="))
    }
}
