package com.example.gpgrocery.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {
    @Test
    fun `the right PIN matches and a wrong one does not`() {
        val stored = PinHasher.create("2580")
        assertTrue(PinHasher.matches("2580", stored))
        assertFalse(PinHasher.matches("2581", stored))
        assertFalse(PinHasher.matches("", stored))
    }

    @Test
    fun `the PIN itself is never stored`() {
        val stored = PinHasher.create("2580")
        assertFalse(stored.hash.contains("2580"))
        assertEquals(PinHasher.ITERATIONS, stored.iterations)
    }

    @Test
    fun `the same PIN hashes differently each time`() {
        val first = PinHasher.create("2580")
        val second = PinHasher.create("2580")
        assertNotEquals(first.salt, second.salt)
        assertNotEquals(first.hash, second.hash)
    }

    @Test
    fun `a PIN is exactly four digits`() {
        assertTrue(PinHasher.isValidPin("0000"))
        assertFalse(PinHasher.isValidPin("123"))
        assertFalse(PinHasher.isValidPin("12345"))
        assertFalse(PinHasher.isValidPin("12a4"))
        assertFalse(PinHasher.isValidPin(""))
    }
}
