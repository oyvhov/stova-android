package app.stova.core.security

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlinx.serialization.Serializable

/** Base64 salt and hash. The round count is stored so a later, stronger setting can coexist. */
@Serializable
data class PinHash(val salt: String, val hash: String, val iterations: Int = 100_000)

/** PBKDF2-HMAC-SHA256 with 100 000 rounds and a 16-byte salt, as in Spole. */
object PinHasher {
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private val PIN = Regex("\\d{4,8}")

    fun isValid(pin: String): Boolean = PIN.matches(pin)

    fun hash(pin: String, random: SecureRandom = SecureRandom()): PinHash {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val encoder = Base64.getEncoder()
        return PinHash(encoder.encodeToString(salt), encoder.encodeToString(derive(pin, salt, 100_000)))
    }

    fun verify(pin: String, stored: PinHash): Boolean {
        val decoder = Base64.getDecoder()
        val expected = decoder.decode(stored.hash)
        return MessageDigest.isEqual(expected, derive(pin, decoder.decode(stored.salt), stored.iterations))
    }

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
