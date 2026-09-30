package com.example.gpgrocery

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTest {
    private val fiveMinutes = 5 * 60 * 1000L

    @Test
    fun `starts locked`() {
        assertFalse(Session().unlocked.value)
    }

    @Test
    fun `a quick look away keeps the store open`() {
        val session = Session().apply { unlock() }
        session.wentAway(at = 1_000)
        session.cameBack(at = 1_000 + fiveMinutes, lockAfterMillis = fiveMinutes)
        assertTrue(session.unlocked.value)
    }

    @Test
    fun `more than five minutes away locks it`() {
        val session = Session().apply { unlock() }
        session.wentAway(at = 1_000)
        session.cameBack(at = 1_001 + fiveMinutes, lockAfterMillis = fiveMinutes)
        assertFalse(session.unlocked.value)
    }

    @Test
    fun `each absence is timed on its own`() {
        val session = Session().apply { unlock() }
        session.wentAway(at = 1_000)
        session.cameBack(at = 2_000, lockAfterMillis = fiveMinutes)
        // Coming back again without having left does not count the old absence.
        session.cameBack(at = 2_000 + fiveMinutes * 2, lockAfterMillis = fiveMinutes)
        assertTrue(session.unlocked.value)
    }
}
