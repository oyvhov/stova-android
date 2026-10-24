package app.stova.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import app.stova.core.storage.JsonFileStore
import java.io.File
import java.security.KeyStore
import java.util.Base64
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/**
 * Secrets sealed with an AES-256 key that lives in the Android Keystore and never leaves it. The
 * file holds only Base64 ciphertext, so a copied `secrets.json` is useless off this device.
 */
class KeystoreSecretStore(context: Context, private val alias: String = "stova.secrets") : SecretStore {
    private val box = AesGcmBox(::key)
    private val store = JsonFileStore(
        File(context.filesDir, "secrets.json"),
        MapSerializer(String.serializer(), String.serializer()),
        emptyMap(),
    )

    override suspend fun read(key: String): String? =
        store.current()[key]?.let { String(box.open(Base64.getDecoder().decode(it)), Charsets.UTF_8) }

    override suspend fun write(key: String, value: String) {
        val sealed = Base64.getEncoder().encodeToString(box.seal(value.toByteArray(Charsets.UTF_8)))
        store.update { it + (key to sealed) }
    }

    override suspend fun delete(key: String) {
        store.update { it - key }
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
    }
}
