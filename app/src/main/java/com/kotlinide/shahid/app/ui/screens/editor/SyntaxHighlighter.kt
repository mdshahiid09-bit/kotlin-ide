package com.kotlinide.shahid.app.ui.screens.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.util.regex.Pattern

class SyntaxHighlighter(
    private val extension: String,
    private val isDark: Boolean = true
) : VisualTransformation {

    private val keywordColor = if (isDark) Color(0xFFFF7B72) else Color(0xFFCF222E)
    private val typeColor = if (isDark) Color(0xFF79C0FF) else Color(0xFF0550AE)
    private val stringColor = if (isDark) Color(0xFFA5D6FF) else Color(0xFF0A3069)
    private val commentColor = if (isDark) Color(0xFF8B949E) else Color(0xFF6E7681)
    private val annotationColor = if (isDark) Color(0xFFFFA657) else Color(0xFF953800)
    private val numberColor = if (isDark) Color(0xFF7EE787) else Color(0xFF116329)

    private val kotlinKeywords = setOf(
        "package", "import", "class", "interface", "object", "fun", "val", "var",
        "private", "public", "protected", "internal", "override", "return", "if",
        "else", "when", "for", "while", "do", "try", "catch", "finally", "throw",
        "is", "as", "in", "by", "companion", "data", "sealed", "enum", "open",
        "abstract", "lateinit", "suspend", "inline", "true", "false", "null", "this", "super"
    )

    private val gradleKeywords = setOf(
        "plugins", "alias", "android", "defaultConfig", "buildTypes", "release",
        "debug", "dependencies", "implementation", "testImplementation", "compileSdk",
        "minSdk", "targetSdk", "versionCode", "versionName", "namespace", "apply", "false", "true"
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val annotated = buildAnnotatedString {
            append(raw)

            if (extension == "kt" || extension == "kts") {
                highlightKotlin(raw)
            } else if (extension == "xml") {
                highlightXml(raw)
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }

    private fun AnnotatedString.Builder.highlightKotlin(text: String) {
        val commentMatcher = Pattern.compile("(//.*$|/\\*.*?\\*/)", Pattern.MULTILINE or Pattern.DOTALL).matcher(text)
        while (commentMatcher.find()) {
            addStyle(
                SpanStyle(color = commentColor, fontStyle = FontStyle.Italic),
                commentMatcher.start(),
                commentMatcher.end()
            )
        }

        val stringMatcher = Pattern.compile("\".*?\"").matcher(text)
        while (stringMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), stringMatcher.start(), stringMatcher.end())
        }

        val annotMatcher = Pattern.compile("@[a-zA-Z0-9_]+").matcher(text)
        while (annotMatcher.find()) {
            addStyle(
                SpanStyle(color = annotationColor, fontWeight = FontWeight.SemiBold),
                annotMatcher.start(),
                annotMatcher.end()
            )
        }

        val numMatcher = Pattern.compile("\\b[0-9]+[Lfd]?\\b").matcher(text)
        while (numMatcher.find()) {
            addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
        }

        val wordMatcher = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b").matcher(text)
        while (wordMatcher.find()) {
            val word = wordMatcher.group()
            if (kotlinKeywords.contains(word) || gradleKeywords.contains(word)) {
                addStyle(
                    SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold),
                    wordMatcher.start(),
                    wordMatcher.end()
                )
            } else if (word.first().isUpperCase()) {
                addStyle(SpanStyle(color = typeColor), wordMatcher.start(), wordMatcher.end())
            }
        }
    }

    private fun AnnotatedString.Builder.highlightXml(text: String) {
        val tagMatcher = Pattern.compile("</?[a-zA-Z0-9_:-]+").matcher(text)
        while (tagMatcher.find()) {
            addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), tagMatcher.start(), tagMatcher.end())
        }

        val attrMatcher = Pattern.compile("[a-zA-Z0-9_:-]+(?==)").matcher(text)
        while (attrMatcher.find()) {
            addStyle(SpanStyle(color = annotationColor), attrMatcher.start(), attrMatcher.end())
        }

        val stringMatcher = Pattern.compile("\".*?\"").matcher(text)
        while (stringMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), stringMatcher.start(), stringMatcher.end())
        }

        val commentMatcher = Pattern.compile("<!--.*?-->", Pattern.DOTALL).matcher(text)
        while (commentMatcher.find()) {
            addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
        }
    }
}
