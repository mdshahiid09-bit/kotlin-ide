package com.kotlinide.shahid.app.editor

enum class LintSeverity {
    ERROR,
    WARNING,
    INFO
}

data class LintIssue(
    val line: Int,
    val column: Int,
    val message: String,
    val severity: LintSeverity
)

object EditorLinter {

    fun lint(content: String, fileExtension: String): List<LintIssue> {
        if (content.isBlank()) return emptyList()

        val issues = mutableListOf<LintIssue>()
        val ext = fileExtension.lowercase()

        when (ext) {
            "kt", "kts", "java" -> {
                lintKotlinOrJava(content, issues)
            }
            "xml" -> {
                lintXml(content, issues)
            }
            "json" -> {
                lintJson(content, issues)
            }
            else -> {
                lintGenericBrackets(content, issues)
            }
        }

        return issues.sortedBy { it.line }
    }

    private fun lintKotlinOrJava(content: String, issues: MutableList<LintIssue>) {
        val lines = content.lines()

        // Check bracket / parenthesis balance
        lintGenericBrackets(content, issues)

        // Check unclosed strings
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (!trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                var inString = false
                var escape = false
                for (ch in line) {
                    if (ch == '\\' && !escape) {
                        escape = true
                        continue
                    }
                    if (ch == '"' && !escape) {
                        inString = !inString
                    }
                    escape = false
                }
                if (inString && !line.contains("\"\"\"")) {
                    issues.add(
                        LintIssue(
                            line = index + 1,
                            column = line.length,
                            message = "Unclosed string literal",
                            severity = LintSeverity.ERROR
                        )
                    )
                }
            }
        }

        // Package warning if Kotlin source inside src/
        if (content.contains("class ") || content.contains("fun ")) {
            val hasPackage = lines.any { it.trim().startsWith("package ") }
            if (!hasPackage) {
                issues.add(
                    LintIssue(
                        line = 1,
                        column = 1,
                        message = "Missing package declaration in Kotlin source file",
                        severity = LintSeverity.WARNING
                    )
                )
            }
        }
    }

    private fun lintXml(content: String, issues: MutableList<LintIssue>) {
        val lines = content.lines()
        val tagStack = ArrayDeque<Pair<String, Int>>()

        lines.forEachIndexed { index, rawLine ->
            val line = rawLine.trim()
            if (line.startsWith("<!--") || line.startsWith("<?xml")) return@forEachIndexed

            val tagPattern = Regex("""<(/)?([a-zA-Z0-9_.:-]+)([^>]*)(/)?>""")
            for (match in tagPattern.findAll(rawLine)) {
                val isClosing = match.groupValues[1] == "/"
                val tagName = match.groupValues[2]
                val isSelfClosing = match.groupValues[4] == "/"

                if (isSelfClosing) {
                    continue
                }

                if (isClosing) {
                    if (tagStack.isEmpty()) {
                        issues.add(
                            LintIssue(
                                line = index + 1,
                                column = match.range.first + 1,
                                message = "Unexpected closing tag </$tagName>",
                                severity = LintSeverity.ERROR
                            )
                        )
                    } else {
                        val (topTag, topLine) = tagStack.removeLast()
                        if (topTag != tagName) {
                            issues.add(
                                LintIssue(
                                    line = index + 1,
                                    column = match.range.first + 1,
                                    message = "Mismatched closing tag </$tagName>, expected </$topTag> (opened at line $topLine)",
                                    severity = LintSeverity.ERROR
                                )
                            )
                        }
                    }
                } else {
                    tagStack.addLast(Pair(tagName, index + 1))
                }
            }
        }

        while (tagStack.isNotEmpty()) {
            val (unclosedTag, line) = tagStack.removeLast()
            issues.add(
                LintIssue(
                    line = line,
                    column = 1,
                    message = "Unclosed XML tag <$unclosedTag>",
                    severity = LintSeverity.ERROR
                )
            )
        }
    }

    private fun lintJson(content: String, issues: MutableList<LintIssue>) {
        lintGenericBrackets(content, issues)
    }

    private fun lintGenericBrackets(content: String, issues: MutableList<LintIssue>) {
        val stack = ArrayDeque<Triple<Char, Int, Int>>()
        val lines = content.lines()

        for ((lineIdx, line) in lines.withIndex()) {
            var inSingleLineComment = false
            var inString = false
            var escape = false

            for ((colIdx, ch) in line.withIndex()) {
                if (inSingleLineComment) break

                if (ch == '/' && colIdx + 1 < line.length && line[colIdx + 1] == '/') {
                    inSingleLineComment = true
                    continue
                }

                if (ch == '\\' && !escape) {
                    escape = true
                    continue
                }

                if (ch == '"' && !escape) {
                    inString = !inString
                }
                escape = false

                if (inString) continue

                when (ch) {
                    '{', '(', '[' -> {
                        stack.addLast(Triple(ch, lineIdx + 1, colIdx + 1))
                    }
                    '}', ')', ']' -> {
                        if (stack.isEmpty()) {
                            issues.add(
                                LintIssue(
                                    line = lineIdx + 1,
                                    column = colIdx + 1,
                                    message = "Unmatched closing delimiter '$ch'",
                                    severity = LintSeverity.ERROR
                                )
                            )
                        } else {
                            val (top, topL, topC) = stack.removeLast()
                            val expected = when (top) {
                                '{' -> '}'
                                '(' -> ')'
                                '[' -> ']'
                                else -> ' '
                            }
                            if (ch != expected) {
                                issues.add(
                                    LintIssue(
                                        line = lineIdx + 1,
                                        column = colIdx + 1,
                                        message = "Mismatched delimiter '$ch', expected '$expected' for open '$top' at line $topL:$topC",
                                        severity = LintSeverity.ERROR
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        while (stack.isNotEmpty()) {
            val (openChar, line, col) = stack.removeLast()
            issues.add(
                LintIssue(
                    line = line,
                    column = col,
                    message = "Unclosed delimiter '$openChar'",
                    severity = LintSeverity.ERROR
                )
            )
        }
    }
}
