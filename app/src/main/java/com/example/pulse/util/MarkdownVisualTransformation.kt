// FILE: app/src/main/java/com/example/pulse/util/MarkdownVisualTransformation.kt
package com.example.pulse.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration

private data class StylePattern(val regex: Regex, val style: SpanStyle)

private val inlinePatterns = listOf(
    StylePattern(Regex("\\*\\*(.+?)\\*\\*"), SpanStyle(fontWeight = FontWeight.Bold)),
    StylePattern(Regex("(?<!_)_(.+?)_(?!_)"), SpanStyle(fontStyle = FontStyle.Italic)),
    StylePattern(Regex("\\+\\+(.+?)\\+\\+"), SpanStyle(textDecoration = TextDecoration.Underline))
)

// Order matters: checkbox variants must be checked before the plain bullet
// marker, since "- [ ] " also starts with "- ".
private val lineMarkers = listOf(
    "- [ ] " to "\u2610 ", // ☐
    "- [x] " to "\u2611 ", // ☑
    "- [X] " to "\u2611 ",
    "- " to "\u2022 "      // •
)

/**
 * Live-preview markdown for the journal editor:
 *  - **bold**, _italic_, ++underline++ render styled with markers hidden
 *  - "- ", "- [ ] ", "- [x] " line-prefixes render as actual glyphs
 *    (•, ☐, ☑) instead of raw text
 *  - "1. " numbered markers are left as visible text — they're already
 *    readable as-is; auto-incrementing on Enter is handled in the
 *    ViewModel, not here
 *
 * Cursor mapping inside a replaced span is approximate (clamped to the
 * inner/symbol bounds), since replaced characters don't exist 1:1 in the
 * rendered string — standard tradeoff for live-preview markdown editors.
 */
class MarkdownVisualTransformation : VisualTransformation {

    private data class Marker(
        val origStart: Int, val origEnd: Int,
        val transStart: Int, val transEnd: Int
    )

    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        val builder = AnnotatedString.Builder()
        val markers = mutableListOf<Marker>()

        var i = 0
        while (i < original.length) {
            val atLineStart = i == 0 || original[i - 1] == '\n'

            val lineMarker = if (atLineStart) {
                lineMarkers.firstOrNull { (marker, _) -> original.startsWith(marker, i) }
            } else null

            if (lineMarker != null) {
                val (marker, symbol) = lineMarker
                val transStart = builder.length
                builder.append(symbol)
                val transEnd = builder.length
                markers.add(Marker(i, i + marker.length, transStart, transEnd))
                i += marker.length
                continue
            }

            val nextInline = inlinePatterns
                .mapNotNull { p -> p.regex.find(original, i)?.let { it to p.style } }
                .minByOrNull { it.first.range.first }

            if (nextInline == null) {
                builder.append(original.substring(i))
                break
            }

            val (match, style) = nextInline
            if (match.range.first > i) {
                builder.append(original.substring(i, match.range.first))
            }
            val inner = match.groupValues[1]
            val transStart = builder.length
            builder.append(inner)
            val transEnd = builder.length
            builder.addStyle(style, transStart, transEnd)
            markers.add(Marker(match.range.first, match.range.last + 1, transStart, transEnd))
            i = match.range.last + 1
        }

        val transformed = builder.toAnnotatedString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var shift = 0
                for (m in markers) {
                    if (offset <= m.origStart) return offset - shift
                    val markerLen = m.origEnd - m.origStart
                    val innerLen = m.transEnd - m.transStart
                    if (offset <= m.origEnd) {
                        val clamped = (offset - m.origStart - (markerLen - innerLen) / 2)
                            .coerceIn(0, innerLen)
                        return m.transStart + clamped
                    }
                    shift += markerLen - innerLen
                }
                return offset - shift
            }

            override fun transformedToOriginal(offset: Int): Int {
                var shift = 0
                for (m in markers) {
                    if (offset <= m.transStart) return offset + shift
                    val markerLen = m.origEnd - m.origStart
                    val innerLen = m.transEnd - m.transStart
                    if (offset <= m.transEnd) {
                        return m.origStart + (markerLen - innerLen) / 2 + (offset - m.transStart)
                    }
                    shift += markerLen - innerLen
                }
                return offset + shift
            }
        }

        return TransformedText(transformed, offsetMapping)
    }
}