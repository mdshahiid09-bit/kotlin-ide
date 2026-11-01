package com.kotlinide.shahid.app.data.security

import org.junit.Assert.assertEquals
import org.junit.Test

class TokenStorageTest {

    @Test
    fun testCleanTokenTrimsWhitespaceAndNewlines() {
        val dirty = "  ghp_1234567890abcdef \r\n\t  "
        val clean = TokenStorage.cleanToken(dirty)
        assertEquals("ghp_1234567890abcdef", clean)
    }

    @Test
    fun testCleanTokenHandlesEmptyOrSpaces() {
        val spaces = "    \n\r  \t "
        val clean = TokenStorage.cleanToken(spaces)
        assertEquals("", clean)
    }
}
