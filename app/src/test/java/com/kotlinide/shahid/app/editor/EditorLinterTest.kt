package com.kotlinide.shahid.app.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorLinterTest {

    @Test
    fun testValidKotlinCodeProducesNoErrors() {
        val code = """
            package com.example.app
            
            fun main() {
                val list = listOf(1, 2, 3)
                println("Items count: ${'$'}{list.size}")
            }
        """.trimIndent()

        val issues = EditorLinter.lint(code, "kt")
        val errors = issues.filter { it.severity == LintSeverity.ERROR }
        assertTrue("Expected no errors in valid code, got: $errors", errors.isEmpty())
    }

    @Test
    fun testUnclosedBraceDetected() {
        val code = """
            package com.example.app
            
            fun broken() {
                if (true) {
                    println("test")
            }
        """.trimIndent()

        val issues = EditorLinter.lint(code, "kt")
        val errors = issues.filter { it.severity == LintSeverity.ERROR }
        assertEquals(1, errors.size)
        assertTrue(errors[0].message.contains("Unclosed delimiter '{'"))
    }

    @Test
    fun testMismatchedBracketDetected() {
        val code = """
            val arr = [1, 2, 3)
        """.trimIndent()

        val issues = EditorLinter.lint(code, "kt")
        val errors = issues.filter { it.severity == LintSeverity.ERROR }
        assertTrue(errors.any { it.message.contains("Mismatched delimiter") })
    }

    @Test
    fun testUnclosedXmlTagDetected() {
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <LinearLayout xmlns:android="http://schemas.android.com/apk/res/android">
                <TextView android:text="Hello" />
        """.trimIndent()

        val issues = EditorLinter.lint(xml, "xml")
        val errors = issues.filter { it.severity == LintSeverity.ERROR }
        assertEquals(1, errors.size)
        assertTrue(errors[0].message.contains("Unclosed XML tag <LinearLayout>"))
    }
}
