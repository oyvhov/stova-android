package app.stova.core.security

import java.util.concurrent.ConcurrentHashMap

/** Tokens and other secrets. Never logged, never written in clear text. */
interface SecretStore {
    suspend fun read(key: String): String?
    suspend fun write(key: String, value: String)
    suspend fun delete(key: String)
}

/** For tests in every plan: the same contract, held in memory. */
class InMemorySecretStore : SecretStore {
    private val values = ConcurrentHashMap<String, String>()
    override suspend fun read(key: String): String? = values[key]
    override suspend fun write(key: String, value: String) { values[key] = value }
    override suspend fun delete(key: String) { values.remove(key) }
}
