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

    // Theme color palette
    private val keywordColor = if (isDark) Color(0xFFFF7B72) else Color(0xFFCF222E)
    private val typeColor = if (isDark) Color(0xFF79C0FF) else Color(0xFF0550AE)
    private val stringColor = if (isDark) Color(0xFFA5D6FF) else Color(0xFF0A3069)
    private val commentColor = if (isDark) Color(0xFF8B949E) else Color(0xFF6E7681)
    private val annotationColor = if (isDark) Color(0xFFFFA657) else Color(0xFF953800)
    private val numberColor = if (isDark) Color(0xFF7EE787) else Color(0xFF116329)
    private val tagColor = if (isDark) Color(0xFF7EE787) else Color(0xFF116329)
    private val attrColor = if (isDark) Color(0xFFD2A8FF) else Color(0xFF8250DF)

    private val kotlinKeywords = setOf(
        "package", "import", "class", "interface", "object", "fun", "val", "var",
        "private", "public", "protected", "internal", "override", "return", "if",
        "else", "when", "for", "while", "do", "try", "catch", "finally", "throw",
        "is", "as", "in", "by", "companion", "data", "sealed", "enum", "open",
        "abstract", "lateinit", "suspend", "inline", "true", "false", "null", "this", "super",
        "typealias", "constructor", "init", "where", "out", "field", "reified"
    )

    private val javaKeywords = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
        "const", "continue", "default", "do", "double", "else", "enum", "extends", "final",
        "finally", "float", "for", "goto", "if", "implements", "import", "instanceof", "int",
        "interface", "long", "native", "new", "package", "private", "protected", "public",
        "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "try", "void", "volatile", "while", "true", "false", "null"
    )

    private val jsKeywords = setOf(
        "const", "let", "var", "function", "return", "if", "else", "for", "while", "do",
        "switch", "case", "break", "continue", "try", "catch", "finally", "throw", "class",
        "extends", "new", "this", "super", "import", "export", "from", "default", "async",
        "await", "yield", "typeof", "instanceof", "in", "of", "null", "undefined", "true", "false"
    )

    private val gradleKeywords = setOf(
        "plugins", "alias", "android", "defaultConfig", "buildTypes", "release",
        "debug", "dependencies", "implementation", "testImplementation", "compileSdk",
        "minSdk", "targetSdk", "versionCode", "versionName", "namespace", "apply", "false", "true",
        "sourceCompatibility", "targetCompatibility", "compose", "buildFeatures", "kotlinOptions",
        "jvmTarget", "proguardFiles", "getDefaultProguardFile", "include", "rootProject"
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val ext = extension.lowercase()

        val annotated = buildAnnotatedString {
            append(raw)

            when (ext) {
                "kt", "kts" -> highlightKotlin(raw)
                "java" -> highlightJava(raw)
                "xml", "html", "svg" -> highlightXml(raw)
                "json" -> highlightJson(raw)
                "js", "ts", "jsx", "tsx" -> highlightJs(raw)
                "css", "scss" -> highlightCss(raw)
                "yml", "yaml" -> highlightYaml(raw)
                "md", "markdown" -> highlightMarkdown(raw)
                "gradle", "properties", "toml", "pro" -> highlightGradle(raw)
                else -> highlightGeneric(raw)
            }
        }
        return TransformedText(annotated, OffsetMapping.Identity)
    }

    private fun AnnotatedString.Builder.highlightKotlin(text: String) {
        val commentMatcher = Pattern.compile("(//.*$|/\\*.*?\\*/)", Pattern.MULTILINE or Pattern.DOTALL).matcher(text)
        while (commentMatcher.find()) {
            addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
        }
        val stringMatcher = Pattern.compile("(\".*?\"|'[^']*')").matcher(text)
        while (stringMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), stringMatcher.start(), stringMatcher.end())
        }
        val annotMatcher = Pattern.compile("@[a-zA-Z0-9_]+").matcher(text)
        while (annotMatcher.find()) {
            addStyle(SpanStyle(color = annotationColor, fontWeight = FontWeight.SemiBold), annotMatcher.start(), annotMatcher.end())
        }
        val numMatcher = Pattern.compile("\\b[0-9]+[Lfd]?\\b").matcher(text)
        while (numMatcher.find()) {
            addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
        }
        val wordMatcher = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b").matcher(text)
        while (wordMatcher.find()) {
            val word = wordMatcher.group()
            if (kotlinKeywords.contains(word)) {
                addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), wordMatcher.start(), wordMatcher.end())
            } else if (word.first().isUpperCase()) {
                addStyle(SpanStyle(color = typeColor), wordMatcher.start(), wordMatcher.end())
            }
        }
    }

    private fun AnnotatedString.Builder.highlightJava(text: String) {
        val commentMatcher = Pattern.compile("(//.*$|/\\*.*?\\*/)", Pattern.MULTILINE or Pattern.DOTALL).matcher(text)
        while (commentMatcher.find()) {
            addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
        }
        val stringMatcher = Pattern.compile("(\".*?\"|'[^']*')").matcher(text)
        while (stringMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), stringMatcher.start(), stringMatcher.end())
        }
        val annotMatcher = Pattern.compile("@[a-zA-Z0-9_]+").matcher(text)
        while (annotMatcher.find()) {
            addStyle(SpanStyle(color = annotationColor, fontWeight = FontWeight.SemiBold), annotMatcher.start(), annotMatcher.end())
        }
        val numMatcher = Pattern.compile("\\b[0-9]+[Lfd]?\\b").matcher(text)
        while (numMatcher.find()) {
            addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
        }
        val wordMatcher = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b").matcher(text)
        while (wordMatcher.find()) {
            val word = wordMatcher.group()
            if (javaKeywords.contains(word)) {
                addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), wordMatcher.start(), wordMatcher.end())
            } else if (word.first().isUpperCase()) {
                addStyle(SpanStyle(color = typeColor), wordMatcher.start(), wordMatcher.end())
            }
        }
    }

    private fun AnnotatedString.Builder.highlightXml(text: String) {
        val tagMatcher = Pattern.compile("</?[a-zA-Z0-9_:-]+").matcher(text)
        while (tagMatcher.find()) {
            addStyle(SpanStyle(color = tagColor, fontWeight = FontWeight.Bold), tagMatcher.start(), tagMatcher.end())
        }
        val attrMatcher = Pattern.compile("[a-zA-Z0-9_:-]+(?==)").matcher(text)
        while (attrMatcher.find()) {
            addStyle(SpanStyle(color = attrColor), attrMatcher.start(), attrMatcher.end())
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

    private fun AnnotatedString.Builder.highlightJson(text: String) {
        val keyMatcher = Pattern.compile("\"[^\"]*\"(?=\\s*:)").matcher(text)
        while (keyMatcher.find()) {
            addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold), keyMatcher.start(), keyMatcher.end())
        }
        val stringMatcher = Pattern.compile("(?<=:\\s*)\"[^\"]*\"").matcher(text)
        while (stringMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), stringMatcher.start(), stringMatcher.end())
        }
        val numMatcher = Pattern.compile("\\b-?[0-9]+(\\.[0-9]+)?\\b").matcher(text)
        while (numMatcher.find()) {
            addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
        }
        val boolMatcher = Pattern.compile("\\b(true|false|null)\\b").matcher(text)
        while (boolMatcher.find()) {
            addStyle(SpanStyle(color = typeColor, fontWeight = FontWeight.Bold), boolMatcher.start(), boolMatcher.end())
        }
    }

    private fun AnnotatedString.Builder.highlightJs(text: String) {
        val commentMatcher = Pattern.compile("(//.*$|/\\*.*?\\*/)", Pattern.MULTILINE or Pattern.DOTALL).matcher(text)
        while (commentMatcher.find()) {
            addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
        }
        val stringMatcher = Pattern.compile("(\".*?\"|'.*?'|`.*?`)").matcher(text)
        while (stringMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), stringMatcher.start(), stringMatcher.end())
        }
        val numMatcher = Pattern.compile("\\b[0-9]+(\\.[0-9]+)?\\b").matcher(text)
        while (numMatcher.find()) {
            addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
        }
        val wordMatcher = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b").matcher(text)
        while (wordMatcher.find()) {
            val word = wordMatcher.group()
            if (jsKeywords.contains(word)) {
                addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), wordMatcher.start(), wordMatcher.end())
            }
        }
    }

    private fun AnnotatedString.Builder.highlightCss(text: String) {
        val propMatcher = Pattern.compile("[a-zA-Z-]+(?=\\s*:)").matcher(text)
        while (propMatcher.find()) {
            addStyle(SpanStyle(color = typeColor), propMatcher.start(), propMatcher.end())
        }
        val valueMatcher = Pattern.compile("(?<=:\\s*)[^;]+(?=;)").matcher(text)
        while (valueMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), valueMatcher.start(), valueMatcher.end())
        }
        val commentMatcher = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL).matcher(text)
        while (commentMatcher.find()) {
            addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
        }
    }

    private fun AnnotatedString.Builder.highlightYaml(text: String) {
        val commentMatcher = Pattern.compile("#.*$", Pattern.MULTILINE).matcher(text)
        while (commentMatcher.find()) {
            addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
        }
        val keyMatcher = Pattern.compile("^[ \\t]*[a-zA-Z0-9_.-]+(?=:)", Pattern.MULTILINE).matcher(text)
        while (keyMatcher.find()) {
            addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), keyMatcher.start(), keyMatcher.end())
        }
        val stringMatcher = Pattern.compile("(\".*?\"|'.*?')").matcher(text)
        while (stringMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), stringMatcher.start(), stringMatcher.end())
        }
    }

    private fun AnnotatedString.Builder.highlightMarkdown(text: String) {
        val headerMatcher = Pattern.compile("^#{1,6}\\s.*$", Pattern.MULTILINE).matcher(text)
        while (headerMatcher.find()) {
            addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), headerMatcher.start(), headerMatcher.end())
        }
        val boldMatcher = Pattern.compile("\\*\\*.*?\\*\\*").matcher(text)
        while (boldMatcher.find()) {
            addStyle(SpanStyle(color = annotationColor, fontWeight = FontWeight.Bold), boldMatcher.start(), boldMatcher.end())
        }
        val linkMatcher = Pattern.compile("\\[.*?\\]\\(.*?\\)").matcher(text)
        while (linkMatcher.find()) {
            addStyle(SpanStyle(color = typeColor), linkMatcher.start(), linkMatcher.end())
        }
        val codeBlockMatcher = Pattern.compile("```.*?```", Pattern.DOTALL).matcher(text)
        while (codeBlockMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), codeBlockMatcher.start(), codeBlockMatcher.end())
        }
    }

    private fun AnnotatedString.Builder.highlightGradle(text: String) {
        val commentMatcher = Pattern.compile("(//.*$|#.*$|/\\*.*?\\*/)", Pattern.MULTILINE or Pattern.DOTALL).matcher(text)
        while (commentMatcher.find()) {
            addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
        }
        val stringMatcher = Pattern.compile("(\".*?\"|'.*?')").matcher(text)
        while (stringMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), stringMatcher.start(), stringMatcher.end())
        }
        val wordMatcher = Pattern.compile("\\b[a-zA-Z_][a-zA-Z0-9_]*\\b").matcher(text)
        while (wordMatcher.find()) {
            val word = wordMatcher.group()
            if (gradleKeywords.contains(word)) {
                addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), wordMatcher.start(), wordMatcher.end())
            }
        }
    }

    private fun AnnotatedString.Builder.highlightGeneric(text: String) {
        val commentMatcher = Pattern.compile("(//.*$|#.*$)", Pattern.MULTILINE).matcher(text)
        while (commentMatcher.find()) {
            addStyle(SpanStyle(color = commentColor, fontStyle = FontStyle.Italic), commentMatcher.start(), commentMatcher.end())
        }
        val stringMatcher = Pattern.compile("(\".*?\"|'.*?')").matcher(text)
        while (stringMatcher.find()) {
            addStyle(SpanStyle(color = stringColor), stringMatcher.start(), stringMatcher.end())
        }
    }
}
