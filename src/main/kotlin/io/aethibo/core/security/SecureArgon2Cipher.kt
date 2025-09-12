package io.aethibo.core.security

import de.mkammerer.argon2.Argon2Factory
import java.security.SecureRandom
import java.util.*

data object SecureArgon2Cipher {
    private val argon2 = Argon2Factory.create(Argon2Factory.Argon2Types.ARGON2id)

    private const val TIME_COST = 3      // iterations
    private const val MEMORY_COST = 65536 // 64 MB
    private const val PARALLELISM = 2    // threads

    fun hashPassword(password: String): String {
        return argon2.hash(TIME_COST, MEMORY_COST, PARALLELISM, password.toCharArray())
    }

    fun verifyPassword(password: String, hashedPassword: String): Boolean {
        return try {
            argon2.verify(hashedPassword, password.toCharArray())
        } catch (e: Exception) {
            false
        } finally {
            // Clear password from memory for security
            password.toCharArray().fill('\u0000')
        }
    }

    fun generateSecureSecret(length: Int = 64): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(length)
        secureRandom.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }
}
