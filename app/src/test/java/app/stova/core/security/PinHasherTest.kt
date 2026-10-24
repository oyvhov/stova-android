package app.stova.core.security

import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test fun validPins() {
        assertTrue(PinHasher.isValid("1234")); assertTrue(PinHasher.isValid("12345678"))
        assertFalse(PinHasher.isValid("123")); assertFalse(PinHasher.isValid("123456789")); assertFalse(PinHasher.isValid("12a4"))
    }

    @Test fun verifyAcceptsRightAndRejectsWrong() {
        val h = PinHasher.hash("2468")
        assertTrue(PinHasher.verify("2468", h)); assertFalse(PinHasher.verify("2469", h))
    }

    @Test fun usesSpoleStrength() {
        val h = PinHasher.hash("2468")
        assertEquals(100_000, h.iterations); assertEquals(16, Base64.getDecoder().decode(h.salt).size)
    }

    @Test fun saltDiffers() = assertNotEquals(PinHasher.hash("2468").hash, PinHasher.hash("2468").hash)
}
