package app.stova.core.security

import javax.crypto.AEADBadTagException
import javax.crypto.KeyGenerator
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AesGcmBoxTest {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val box = AesGcmBox { key }

    @Test fun roundTrip() = assertArrayEquals("hemmeleg".toByteArray(), box.open(box.seal("hemmeleg".toByteArray())))

    @Test fun sameInputGivesDifferentCiphertext() =
        assertFalse(box.seal(byteArrayOf(1)).contentEquals(box.seal(byteArrayOf(1))))

    @Test(expected = AEADBadTagException::class) fun tamperingIsDetected() {
        val sealed = box.seal("x".toByteArray()); sealed[sealed.lastIndex] = (sealed.last() + 1).toByte(); box.open(sealed)
    }
}
