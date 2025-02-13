package org.kociumba.kutils.test.utils

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.kociumba.kutils.client.utils.KutilsVersion

class KutilsVersionTest {
    @Test
    fun testToString() {
        assertEquals("1.2.3-p5", KutilsVersion.fromString("1.2.3-p5").toString())
    }

    @Test
    fun testCompareTo() {
        assertTrue(KutilsVersion.fromString("1.2.3") < KutilsVersion.fromString("2.0.0"))
        assertTrue(KutilsVersion.fromString("1.2.3") < KutilsVersion.fromString("1.3.0"))
        assertTrue(KutilsVersion.fromString("1.2.3") < KutilsVersion.fromString("1.2.4"))
        assertTrue(KutilsVersion.fromString("1.2.3-p1") < KutilsVersion.fromString("1.2.3-p2"))
    }

    @Test
    fun testFromStringValid() {
        val v1 = KutilsVersion.fromString("1.0.5")
        assertEquals(1, v1.Major)
        assertEquals(0, v1.Minor)
        assertEquals(5, v1.Revision)
        assertNull(v1.Patch)

        val v2 = KutilsVersion.fromString("0.10.2-p123")
        assertEquals(0, v2.Major)
        assertEquals(10, v2.Minor)
        assertEquals(2, v2.Revision)
        assertEquals(123, v2.Patch)
    }

    @Test
    fun testFromStringInvalid() {
        assertThrows(IllegalArgumentException::class.java) { KutilsVersion.fromString("1.2") }
        assertThrows(IllegalArgumentException::class.java) { KutilsVersion.fromString("1.2.3.4") }
        assertThrows(IllegalArgumentException::class.java) { KutilsVersion.fromString("a.2.3") }
        assertThrows(IllegalArgumentException::class.java) { KutilsVersion.fromString("1.b.3") }
        assertThrows(IllegalArgumentException::class.java) { KutilsVersion.fromString("1.2.c") }
        assertThrows(IllegalArgumentException::class.java) { KutilsVersion.fromString("1.2.3-pa") }
    }
}