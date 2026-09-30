package com.example.gpgrocery.domain

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * The store PIN is never kept, only a salted PBKDF2 hash of it. A four-digit
 * PIN has ten thousand possibilities, so the hash is deliberately slow and
 * the unlock screen also pauses after repeated misses.
 */
object PinHasher {
    const val ITERATIONS = 120_000
    private const val KEY_BITS = 256

    data class Stored(val hash: String, val salt: String, val iterations: Int)

    fun create(pin: String, random: SecureRandom = SecureRandom()): Stored {
        val salt = ByteArray(16).also(random::nextBytes)
        return Stored(encode(derive(pin, salt, ITERATIONS)), encode(salt), ITERATIONS)
    }

    fun matches(pin: String, stored: Stored): Boolean {
        val expected = decode(stored.hash)
        val actual = derive(pin, decode(stored.salt), stored.iterations)
        return MessageDigest.isEqual(expected, actual)
    }

    fun isValidPin(pin: String): Boolean = pin.length == 4 && pin.all(Char::isDigit)

    private fun derive(pin: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String): ByteArray = Base64.getDecoder().decode(text)
}
