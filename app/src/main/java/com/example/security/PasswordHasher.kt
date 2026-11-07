package com.example.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordDigest(val hash: String, val salt: String)

object PasswordHasher {
    private const val ITERATIONS = 210_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    fun create(password: String): PasswordDigest {
        val saltBytes = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hashBytes = derive(password, saltBytes)
        return PasswordDigest(
            hash = Base64.encodeToString(hashBytes, Base64.NO_WRAP),
            salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        )
    }

    fun verify(password: String, salt: String, expectedHash: String): Boolean {
        if (password.isEmpty() || salt.isBlank() || expectedHash.isBlank()) return false
        return runCatching {
            val saltBytes = Base64.decode(salt, Base64.NO_WRAP)
            val expected = Base64.decode(expectedHash, Base64.NO_WRAP)
            MessageDigest.isEqual(derive(password, saltBytes), expected)
        }.getOrDefault(false)
    }

    private fun derive(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
