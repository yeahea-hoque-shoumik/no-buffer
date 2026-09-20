package com.prime.nobuffer.blocklist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.SecureRandom

class LockPasswordTest {

    @Test
    fun rejectsShortOrSimplePasswords() {
        assertFalse(LockPassword.evaluate("short").isValid)
        assertFalse(LockPassword.evaluate("alllowercaseletterssss").isValid)
        assertFalse(LockPassword.evaluate("ALLUPPERCASELETTERSSSS").isValid)
        assertFalse(LockPassword.evaluate("NoSpecialChars1234567").isValid)
        assertFalse(LockPassword.evaluate("NoDigitsHere!!!!!!!!").isValid)
        assertFalse(LockPassword.evaluate("Has a space!!!!!!123").isValid)
    }

    @Test
    fun acceptsTwentyCharMixedPassword() {
        val password = "CorrectHorse!Battery1"
        assertTrue(password.length >= 20)
        assertTrue(LockPassword.evaluate(password).isValid)
    }

    @Test
    fun createThenMatches() {
        val password = "CorrectHorse!Battery1"
        val stored = LockPassword.create(password, SecureRandom())
        assertTrue(LockPassword.matches(password, stored))
        assertFalse(LockPassword.matches("CorrectHorse!Battery2", stored))
        assertFalse(LockPassword.matches("", stored))
    }

    @Test
    fun lockoutDoublesAfterFiveFailures() {
        assertEquals(0L, LockPassword.lockoutMillis(0))
        assertEquals(0L, LockPassword.lockoutMillis(4))
        assertEquals(30_000L, LockPassword.lockoutMillis(5))
        assertEquals(60_000L, LockPassword.lockoutMillis(6))
        assertEquals(120_000L, LockPassword.lockoutMillis(7))
        assertEquals(3_840_000L, LockPassword.lockoutMillis(12))
        assertEquals(3_840_000L, LockPassword.lockoutMillis(20))
    }
}
