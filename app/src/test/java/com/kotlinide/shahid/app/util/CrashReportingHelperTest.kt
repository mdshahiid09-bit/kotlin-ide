package com.kotlinide.shahid.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportingHelperTest {

    @Test
    fun testSanitizeClassicPatToken() {
        val input = "Error: Failed with ghp_1234567890abcdefghijklmnopqrstuvwxyz12 on request"
        val output = CrashReportingHelper.sanitize(input)
        assertFalse(output.contains("ghp_1234567890abcdefghijklmnopqrstuvwxyz12"))
        assertTrue(output.contains("[REDACTED_PAT_TOKEN]"))
    }

    @Test
    fun testSanitizeFineGrainedPatToken() {
        val input = "Auth token is github_pat_11ABCD_efghijk1234567890_xyz987654321"
        val output = CrashReportingHelper.sanitize(input)
        assertFalse(output.contains("github_pat_11ABCD_efghijk1234567890_xyz987654321"))
        assertTrue(output.contains("[REDACTED_PAT_TOKEN]"))
    }

    @Test
    fun testSanitizeAuthorizationHeaderAndBearer() {
        val input = "Header: Authorization: Bearer ghp_sample1234567890abcdefghijklmnopqrstuvwxyz"
        val output = CrashReportingHelper.sanitize(input)
        assertFalse(output.contains("sample1234567890"))
        assertTrue(output.contains("Authorization: Bearer [REDACTED]"))
    }

    @Test
    fun testSanitizeKeystorePasswords() {
        val input = "Keystore config: password=\"MySecretPass123\", keypass='KeySecret456'"
        val output = CrashReportingHelper.sanitize(input)
        assertFalse(output.contains("MySecretPass123"))
        assertFalse(output.contains("KeySecret456"))
        assertTrue(output.contains("password=[REDACTED]"))
        assertTrue(output.contains("keypass=[REDACTED]"))
    }
}
