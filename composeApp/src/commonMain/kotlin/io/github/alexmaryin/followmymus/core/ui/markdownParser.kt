package io.github.alexmaryin.followmymus.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Simple Markdown-like parser for Compose AnnotatedString.
 *
 * Supported:
 *  #, ##, ### – headings
 *
 *  *bold* – bold text
 *
 *  **italic** – italic text
 *
 *  - item – unordered list
 *
 *  1. item – ordered list
 *
 *  --- – section divider
 */
fun parseSimpleMarkdown(input: String): AnnotatedString = buildAnnotatedString {
    val lines = input.lines()

    for (line in lines) {
        val trimmed = line.trim()

        when {
            trimmed.isEmpty() -> {
                append("\n")
            }

            trimmed == "---" -> {
                // Divider as a visual separator
                pushStyle(SpanStyle(color = Color.Gray))
                append("───────────────\n")
                pop()
            }

            trimmed.startsWith("### ") -> appendStyled(trimmed.removePrefix("### "), FontWeight.Bold, 18)
            trimmed.startsWith("## ") -> appendStyled(trimmed.removePrefix("## "), FontWeight.Bold, 20)
            trimmed.startsWith("# ") -> appendStyled(trimmed.removePrefix("# "), FontWeight.Bold, 22)

            // Ordered list (e.g. "1. item")
            trimmed.matches(Regex("^\\d+\\.\\s+.*")) -> {
                val number = Regex("^(\\d+)\\.\\s+").find(trimmed)?.groupValues?.get(1)
                append("${number}. ")
                val content = trimmed.replace(Regex("^\\d+\\.\\s+"), "")
                appendInlineStyles(content)
                append("\n")
            }

            // Unordered list (- item)
            trimmed.startsWith("- ") -> {
                append("• ")
                appendInlineStyles(trimmed.removePrefix("- "))
                append("\n")
            }

            else -> {
                appendInlineStyles(trimmed)
                append("\n")
            }
        }
    }
}

private fun AnnotatedString.Builder.appendStyled(
    text: String,
    weight: FontWeight,
    sizeSp: Int
) {
    pushStyle(SpanStyle(fontWeight = weight, fontSize = sizeSp.sp))
    appendInlineStyles(text)
    pop()
    append("\n")
}

/**
 * Handles inline *bold* and **italic** markup.
 */
private fun AnnotatedString.Builder.appendInlineStyles(text: String) {
    var remaining = text
    while (remaining.isNotEmpty()) {
        val boldStart = remaining.indexOf('*')
        val italicStart = remaining.indexOf("**")

        val nextIndex =
            listOfNotNull(boldStart.takeIf { it >= 0 }, italicStart.takeIf { it >= 0 })
                .minOrNull() ?: -1

        if (nextIndex == -1) {
            append(remaining)
            break
        }

        if (italicStart == nextIndex) {
            // italic
            val end = remaining.indexOf("**", italicStart + 2)
            if (end != -1) {
                append(remaining.take(italicStart))
                val content = remaining.substring(italicStart + 2, end)
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(content)
                pop()
                remaining = remaining.substring(end + 2)
            } else {
                append(remaining)
                break
            }
        } else {
            // bold
            val end = remaining.indexOf('*', boldStart + 1)
            if (end != -1) {
                append(remaining.take(boldStart))
                val content = remaining.substring(boldStart + 1, end)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(content)
                pop()
                remaining = remaining.substring(end + 1)
            } else {
                append(remaining)
                break
            }
        }
    }
}
