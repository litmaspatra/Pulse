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

private data class WrapPattern(val regex: Regex, val style: SpanStyle)

private val wrapPatterns = listOf(
    WrapPattern(Regex("\\*\\*(.+?)\\*\\*"), SpanStyle(fontWeight = FontWeight.Bold)),
    WrapPattern(Regex("(?<!_)_(.+?)_(?!_)"), SpanStyle(fontStyle = FontStyle.Italic)),
    WrapPattern(Regex("\\+\\+(.+?)\\+\\+"), SpanStyle(textDecoration = TextDecoration.Underline))
)

// Checked before the plain bullet marker, since "- [ ] " also starts with "- ".
private val lineMarkers = listOf(
    "- [ ] " to "\u2610 ", // ☐
    "- [x] " to "\u2611 ", // ☑
    "- [X] " to "\u2611 ",
    "- " to "\u2022 "      // •
)

/**
 * Obsidian-style live-preview markdown for the journal editor.
 *
 * INLINE styles (**bold**, _italic_, ++underline++): markers are hidden and
 * the inner text rendered styled — UNLESS the cursor is currently inside or
 * touching that span, in which case the raw markers are shown unstyled so
 * you can see and navigate past them normally. This needs to know where the
 * cursor is, which is why this class takes `cursorOffset` in its
 * constructor — a new instance is built whenever the cursor or text
 * changes (see JournalEditorScreen).
 *
 * LINE markers ("- ", "- [ ] ", "- [x] "): always rendered as glyphs
 * regardless of cursor position — there's no "typing inside the marker"
 * case for these, they only ever sit at the very start of a line.
 *
 * The transformation processes the document one LINE at a time. Earlier
 * versions searched for the next bold/italic match across the *entire
 * remaining document* and, if none was found, dumped everything after that
 * point as unprocessed raw text — which silently broke bullet/checkbox
 * rendering on every line after the first one that had no bold/italic in
 * it. Bounding the search to the current line fixes that.
 */
class MarkdownVisualTransformation(
    private val cursorOffset: Int? = null
) : VisualTransformation {

    private sealed class Marker {
        abstract val origStart: Int
        abstract val origEnd: Int
        abstract val transStart: Int
        abstract val transEnd: Int

        /** Wrap markers (bold/italic/underline) — symmetric prefix+suffix. */
        data class Wrap(
            override val origStart: Int, override val origEnd: Int,
            override val transStart: Int, override val transEnd: Int,
            val identity: Boolean
        ) : Marker()

        /** Line-prefix markers (bullet/checkbox) — asymmetric, prefix only. */
        data class Prefix(
            override val origStart: Int, override val origEnd: Int,
            override val transStart: Int, override val transEnd: Int
        ) : Marker()
    }

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
                markers.add(Marker.Prefix(i, i + marker.length, transStart, transEnd))
                i += marker.length
                continue
            }

            val lineEnd = original.indexOf('\n', i).let { if (it == -1) original.length else it }
            val searchSpace = original.substring(i, lineEnd)

            val nextMatch = wrapPatterns
                .mapNotNull { p -> p.regex.find(searchSpace)?.let { it to p.style } }
                .minByOrNull { it.first.range.first }

            if (nextMatch == null) {
                // Nothing left to transform on THIS line — copy verbatim up
                // through the newline, then let the loop continue so the
                // next line still gets its own marker checks. (Previously
                // this branch broke out of the whole loop, which is what
                // silently killed bullets/checkboxes past line 1.)
                val copyEnd = if (lineEnd < original.length) lineEnd + 1 else lineEnd
                builder.append(original.substring(i, copyEnd))
                i = copyEnd
                continue
            }

            val (match, style) = nextMatch
            val matchStartAbs = i + match.range.first
            val matchEndAbs = i + match.range.last + 1

            if (matchStartAbs > i) {
                builder.append(original.substring(i, matchStartAbs))
            }

            val cursorInside = cursorOffset != null && cursorOffset in matchStartAbs..matchEndAbs

            if (cursorInside) {
                // Show the raw markers, unstyled — identity mapping, so the
                // cursor behaves exactly like a plain text field while
                // you're actively editing this formatted run.
                val transStart = builder.length
                builder.append(original.substring(matchStartAbs, matchEndAbs))
                val transEnd = builder.length
                markers.add(Marker.Wrap(matchStartAbs, matchEndAbs, transStart, transEnd, identity = true))
            } else {
                val inner = match.groupValues[1]
                val transStart = builder.length
                builder.append(inner)
                val transEnd = builder.length
                builder.addStyle(style, transStart, transEnd)
                markers.add(Marker.Wrap(matchStartAbs, matchEndAbs, transStart, transEnd, identity = false))
            }

            i = matchEndAbs
        }

        val transformed = builder.toAnnotatedString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                var shift = 0
                for (m in markers) {
                    if (offset <= m.origStart) return offset - shift
                    val origLen = m.origEnd - m.origStart
                    val transLen = m.transEnd - m.transStart
                    if (offset <= m.origEnd) {
                        return when (m) {
                            is Marker.Prefix -> m.transEnd
                            is Marker.Wrap -> {
                                if (m.identity) {
                                    m.transStart + (offset - m.origStart)
                                } else {
                                    val clamped = (offset - m.origStart - (origLen - transLen) / 2)
                                        .coerceIn(0, transLen)
                                    m.transStart + clamped
                                }
                            }
                        }
                    }
                    shift += origLen - transLen
                }
                return offset - shift
            }

            override fun transformedToOriginal(offset: Int): Int {
                var shift = 0
                for (m in markers) {
                    if (offset <= m.transStart) return offset + shift
                    val origLen = m.origEnd - m.origStart
                    val transLen = m.transEnd - m.transStart
                    if (offset <= m.transEnd) {
                        return when (m) {
                            is Marker.Prefix -> m.origEnd
                            is Marker.Wrap -> {
                                if (m.identity) {
                                    m.origStart + (offset - m.transStart)
                                } else {
                                    m.origStart + (origLen - transLen) / 2 + (offset - m.transStart)
                                }
                            }
                        }
                    }
                    shift += origLen - transLen
                }
                return offset + shift
            }
        }

        return TransformedText(transformed, offsetMapping)
    }
}