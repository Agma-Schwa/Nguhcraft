package org.nguh.nguhcraft.client

import com.mojang.logging.LogUtils
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import org.apache.commons.lang3.StringUtils
import org.nguh.nguhcraft.client.ClientUtils.EMOJI_REPLACEMENTS
import org.slf4j.Logger
import java.util.*

/**
 * Convert Markdown text into a Component.
 *
 * This only handles inline markdown because that’s all Minecraft can render. The dialect
 * of Markdown we use is close to whatever Discord uses.
 *
 * @implNote This code was prototyped as and adapted from
 * [this C++ implementation](https://github.com/Sirraide/inline-markdown-parser).
 */
@Environment(EnvType.CLIENT)
internal class MarkdownParser private constructor(private val MD: String) {
    private open class Node
    private class Emph(var S: ChatFormatting) : Node() {
        var Children: LinkedList<Node> = LinkedList()
    }

    private class Span(
        /** Position of the first character this span represents.  */
        var Start: Int,
        /** Position of the first character after this span.  */
        val End: Int,
        /** Whether this is a code span.  */
        val IsCode: Boolean = false
    ) : Node() {
        /** Index of this span in the Nodes array, if it is present there.  */
        var Index: Int = 0

        /** @return The number of characters in this span. */
        val Size get() = End - Start
    }

    private class LiteralText(val Text: String) : Node()

    @JvmRecord
    private data class Delimiter(val S: Span, val CanOpen: Boolean, val CanClose: Boolean) {
        /** Whether this delimiter is both closing and opening. */
        val Clopen get() = CanOpen && CanClose

        /** How long this delimiter is; e.g. 2 for `**`. */
        val Count get() = S.Size

        /** @return The delimiter kind, e.g. `*` for `**`. */
        fun Kind(P: MarkdownParser): Char {
            return P.MD[S.Start]
        }

        /** Remove `Count` characters from the start of this delimiter.  */
        fun Remove(Count: Int) {
            S.Start += Count
        }
    }

    private val DelimiterStack = ArrayList<Delimiter?>()
    private val Nodes = ArrayList<Node>()

    init {
        DelimiterStack.add(null) // Bottom of stack.
        Parse()
        ProcessEmphasis()
    }

    /** Classify a delimiter’s properties such as openness etc.  */
    private fun ClassifyDelimiter(StartOfText: Int, Text: Span): Boolean {
        // 6.2 Emphasis and strong emphasis.
        //
        // Rules rearranged slightly.
        //
        // 1. `*`/`**`
        //   1a. can open (strong) emphasis iff it is part of a left-flanking delimiter run, and
        //   1b. can close (strong) emphasis iff it is part of a right-flanking delimiter run.
        //
        // EXTENSION: `~~`/`||` behaves like `**`. Discord also seems to apply these rules for
        // '__', which CommonMark does not. We follow Discord’s behaviour here, as this also
        // simplifies the implementation.
        val Next = if (Text.End < MD.length) MD[Text.End] else 0.toChar()
        val Prev = if (Text.Start > 0) MD[Text.Start - 1] else 0.toChar()
        val LeftFlanking = IsFlankingDelimiter(Prev, Next)
        val RightFlanking = IsFlankingDelimiter(Next, Prev)

        // If this can neither open nor close, it is not a delimiter.
        if (!LeftFlanking && !RightFlanking) return false

        // We have a delimiter; append the text we’ve read so far.
        if (StartOfText != Text.Start) Nodes.add(Span(StartOfText, Text.Start))

        // Then, create the delimiter.
        Nodes.add(Text)
        DelimiterStack.add(Delimiter(Text, LeftFlanking, RightFlanking))
        return true
    }

    /** @return Whether `Closer` can be used to close `Opener` */
    private fun DelimitersMatch(Opener: Delimiter?, Closer: Delimiter?): Boolean {
        // 6.2 Emphasis and strong emphasis Rule 9/10
        //
        // If one of the delimiters can both open and close emphasis, then the sum of the
        // lengths of the delimiter runs containing the opening and closing delimiters must
        // not be a multiple of 3 unless both lengths are multiples of 3.
        if (Opener!!.Kind(this) != Closer!!.Kind(this)) return false
        if (!Opener.Clopen && !Closer.Clopen) return true
        val L1 = Opener.Count
        val L2 = Closer.Count
        if (L1 % 3 == 0 && L2 % 3 == 0) return true
        return (L1 + L2) % 3 != 0
    }

    /**
     * Get the style for a delimiter.
     *
     * @param Kind The delimiter kind, e.g. `*` for `**`.
     * @param Strong Whether the delimiter run contains 2 or more delimiters.
     * @return The style that corresponds to a delimiter
     */
    private fun DelimiterStyle(Kind: Char, Strong: Boolean): ChatFormatting {
        return when (Kind) {
            '*' -> if (Strong) ChatFormatting.BOLD else ChatFormatting.ITALIC
            '_' -> if (Strong) ChatFormatting.UNDERLINE else ChatFormatting.ITALIC
            '~' -> ChatFormatting.STRIKETHROUGH // Always strong
            '|' -> ChatFormatting.OBFUSCATED // Always strong
            else -> throw IllegalArgumentException("Invalid delimiter kind: $Kind")
        }
    }

    /**
     * Check whether a delimiter is left-flanking or right-flanking.
     *
     * @param Prev The character before the delimiter if this is a left-flanking delimiter,
     * or after if right-flanking.
     * @param Next The character after the delimiter if this is a left-flanking delimiter,
     * or before if right-flanking.
     * @return Whether the delimiter is left/right-flanking, depending on the parameters.
     */
    private fun IsFlankingDelimiter(Prev: Char, Next: Char): Boolean {
        // 6.2 Emphasis and strong emphasis
        //
        // A left-flanking delimiter run is a delimiter run that is
        //
        //   (1) not followed by Unicode whitespace,
        //
        //   (2) and either
        //
        //       (2a) not followed by a Unicode punctuation character, or
        //
        //       (2b) followed by a Unicode punctuation character and preceded
        //            by Unicode whitespace or a Unicode punctuation character.
        //
        //   (*) For purposes of this definition, the beginning and the end of the
        //       line count as Unicode whitespace.
        //
        // -----
        //
        // In more intelligible terms, this means that:
        //
        //   1. If the delimiter is at end of text, return FALSE.
        //   2. If the next character is whitespace, return FALSE.
        //   3. If the next character is NOT punctuation, return TRUE.
        //   4. If the delimiter is at start of text, return TRUE.
        //   5. If the previous character is whitespace, return TRUE.
        //   6. If the previous character is punctuation, return TRUE.
        //   7. Otherwise, return FALSE.
        //
        // The same applies to right-flanking, but replace every occurrence of ‘next’
        // with ‘previous’ and vice versa. The algorithm implemented here is the one
        // for left-flanking delimiters. It can be used to compute the right-flanking
        // property by swapping the ‘Next’ and ‘Prev’ parameters.
        if (Next.code == 0) return false // 1.
        if (IsWhitespace(Next)) return false // 2.
        if (!IsPunctuation(Next)) return true // 3.
        if (Prev.code == 0) return true // 4.
        if (IsWhitespace(Prev)) return true // 5.
        if (IsPunctuation(Prev)) return true // 6.
        return false // 7.
    }

    /** @return Whether `C` is a unicode punctuation character as per CommonMark.
     */
    private fun IsPunctuation(C: Char): Boolean {
        return when (Character.getType(C).toByte()) {
            Character.CONNECTOR_PUNCTUATION,
            Character.DASH_PUNCTUATION,
            Character.START_PUNCTUATION,
            Character.END_PUNCTUATION,
            Character.INITIAL_QUOTE_PUNCTUATION,
            Character.FINAL_QUOTE_PUNCTUATION,
            Character.OTHER_PUNCTUATION,
            Character.MATH_SYMBOL,
            Character.CURRENCY_SYMBOL,
            Character.MODIFIER_SYMBOL,
            Character.OTHER_SYMBOL -> true
            else -> false
        }
    }

    /** @return Whether `C` is a unicode whitespace character as per CommonMark. */
    private fun IsWhitespace(C: Char): Boolean {
        return when (C) {
            ' ', '\t', '\n', '\u000c' /* \f */, '\r' -> true
            else -> Character.getType(C) == Character.SPACE_SEPARATOR.toInt()
        }
    }

    /** Split Markdown text into nodes and build delimiter stack.  */
    private fun Parse() {
        var Pos = 0
        var StartOfText = 0
        val Length = MD.length
        parse@ while (Pos < Length) {
            // 6.1 Code spans
            //
            // A backtick string is a string of one or more backtick characters
            // (`) that is neither preceded nor followed by a backtick.
            //
            // 6.2 Emphasis and strong emphasis
            //
            // A delimiter run is either
            //
            //    (1) a sequence of one or more `*` characters that is not preceded or
            //        followed by a non-backslash-escaped `*` character, or
            //
            //    (2) a sequence of one or more `_` characters that is not preceded or
            //        followed by a non-backslash-escaped `_` character.
            //
            // EXTENSION: `~~`/`||` are also a delimiters, and we treat emoji names as literals.
            val Start = StringUtils.indexOfAny(MD.substring(Pos), "*_~|`:")
            if (Start == -1) {
                Nodes.add(Span(StartOfText, Length))
                return
            }

            // Convert relative to absolute position.
            var Offs = Pos + Start

            // Check if this is escaped; to do that, read backslashes before the character;
            // note that backslashes can escape each other, so only treat this as escaped
            // if we find an odd number of backslashes.
            var Backslashes = 0
            while (Start - Backslashes > 0 && MD[Offs - Backslashes - 1] == '\\') Backslashes++
            if ((Backslashes and 1) != 0) {
                Pos = Offs + 1
                continue
            }

            // Read the rest of the delimiter.
            val Kind = MD[Offs]
            var Count = 1
            while (Offs + Count < Length && MD[Offs + Count] == Kind) Count++

            // Handle backticks first; unlike emphasis, they are very straight-forward;
            // simply read ahead until we find a corresponding number of backticks (note
            // that backslash-escapes are not allowed in code spans, so we don’t even
            // have to worry about that).
            if (Kind == '`') {
                var SearchStart = Offs + Count
                val Run = MD.substring(Offs, Offs + Count)
                while (true) {
                    val End = MD.substring(SearchStart).indexOf(Run)

                    // If we don’t find a matching backtick string, then these backticks are
                    // literal; stop searching.
                    if (End == -1) {
                        // Only skip past the initial backticks.
                        Pos = Offs + Count
                        continue@parse
                    }

                    // On the other hand, if there are extra backticks here, then this is a
                    // longer backtick string, which doesn’t match the one we’re looking for.
                    //
                    // This handles the case of e.g.: ‘` `` `’, which is ‘<code>``</code>’.
                    val EndPos = SearchStart + End
                    if (EndPos + Count < Length && MD[EndPos + Count] == '`') {
                        // As an optimisation, just skip past all backticks here since we
                        // know that this backtick string isn’t the end anyway.
                        var C = Count
                        do C++
                        while (EndPos + C < Length && MD[EndPos + C] == '`')
                        SearchStart = EndPos + C
                        continue
                    }

                    // Otherwise, we’ve found the end of a code span.
                    Nodes.add(Span(StartOfText, Offs))
                    Nodes.add(Span(Offs + Count, EndPos, true))
                    StartOfText = EndPos + Count
                    Pos = StartOfText
                    continue@parse
                }
            }

            // EXTENSION: Emoji names are literals. An emoji name is a pair of colons
            // with only alphanumeric chars and underscores in between.
            //
            // We need to handle emoji names here too because otherwise, we run into
            // the issue that they may be split into multiple spans, and consequently,
            // parts of the emoji name may be in different literal text components,
            // which means that our replacement later on wouldn’t recognise them.
            if (Kind == ':') {
                // If we have more than one colon in a row, skip to the last one.
                if (Count > 1) Offs = Offs + Count - 1

                // Find the next colon.
                val End = MD.indexOf(':', Offs + 1)
                if (End == -1) {
                    Pos = Offs + 1
                    continue
                }

                // If there is one, look it up immediately so we only have to do this
                // once rather than every time we render it.
                val EmojiName = MD.substring(Offs + 1, End)
                val Emoji = EMOJI_REPLACEMENTS[EmojiName]

                // We found an emoji, skip over the second colon and add it as a literal.
                if (Emoji != null) {
                    Nodes.add(Span(StartOfText, Offs))
                    Nodes.add(LiteralText(Emoji))
                    Pos = End + 1
                    StartOfText = Pos
                }

                // Otherwise, this is just a literal colon. Do NOT skip over the second
                // colon as there may be text in between we need to render; instead, only
                // skip over the first one.
                else Pos = Offs + 1

                // Resume processing after either colon.
                continue
            }

            // EXTENSION: A single `~`/`/` is not a delimiter.
            if ((Kind == '~' || Kind == '|') && Count == 1) {
                Pos = Offs + 1
                continue
            }

            // Create the delimiter.
            if (ClassifyDelimiter(StartOfText, Span(Offs, Offs + Count))) StartOfText = Offs + Count

            // Move past it.
            Pos = Offs + Count
        }

        // Append the last text node.
        if (StartOfText < Length) Nodes.add(Span(StartOfText, Length))
    }

    /** Process emphasis and construct final AST.  */
    private fun ProcessEmphasis() {
        // Note: we always have an empty delimiter at the bottom of the stack.
        if (DelimiterStack.size < 2) return

        // Let current_position point to the element on the delimiter stack
        // just above stack_bottom (or the first element if stack_bottom is NULL).
        var CurrentPosition = 1

        // Initialise indices for each node.
        UpdateNodeIndices(0)

        // Then we repeat the following until we run out of potential closers:
        while (true) {
            // Move current_position forward in the delimiter stack (if needed) until
            // we find the first potential closer with delimiter * or _. (This will
            // be the potential closer closest to the beginning of the input – the
            // first one in parse order.)
            //
            // Note: can_close_strong implies can_close, so we only need to check
            // for the latter.
            while (
                CurrentPosition != DelimiterStack.size &&
                !DelimiterStack[CurrentPosition]!!.CanClose
            ) CurrentPosition++

            // Out of closers.
            if (CurrentPosition == DelimiterStack.size) return

            // Now, look back in the stack (staying above stack_bottom and the openers_bottom
            // for this delimiter type) for the first matching potential opener (see below
            // for the definition of ‘matching’).
            val Closer = DelimiterStack[CurrentPosition]
            var Opener = CurrentPosition - 1
            var Found = false
            while (Opener != 0) {
                Found = DelimitersMatch(DelimiterStack[Opener], Closer)
                if (Found) break
                Opener--
            }

            // If one is found:
            if (Found) {
                // Figure out whether we have emphasis or strong emphasis: if both closer and
                // opener spans have length >= 2, we have strong, otherwise regular.
                //
                // EXTENSION: Two __ are underlining instead of strong emphasis.
                val O = DelimiterStack[Opener]
                val Strong = O!!.Count >= 2 && Closer!!.Count >= 2
                val Style = DelimiterStyle(O.Kind(this), Strong)

                // Move all nodes between opener and closer into a new node.
                val E = Emph(Style)
                val TextNodes = Nodes.subList(O.S.Index + 1, Closer!!.S.Index)
                E.Children.addAll(TextNodes)
                TextNodes.clear()
                TextNodes.add(E)

                // Remove any delimiters between the opener and closer from the delimiter stack.
                val Delims = DelimiterStack.subList(Opener + 1, CurrentPosition)
                val DelimsRemoved = Delims.size
                Delims.clear()

                // Remove 1 (for regular emph) or 2 (for strong emph) delimiters from the
                // opening and closing text nodes.
                val Count = if (Strong) 2 else 1
                O.Remove(Count)
                Closer.Remove(Count)

                // If they become empty as a result, remove them and remove the corresponding
                // element of the delimiter stack.
                if (O.Count == 0) {
                    // Removing nodes invalidated the opener and closer, and removing the opener
                    // invalidates the closer again.
                    Opener -= DelimsRemoved
                    CurrentPosition -= DelimsRemoved + 1

                    // Everything after the opener has moved, but the opener
                    // index is still correct, so we can just delete it here.
                    Nodes.removeAt(O.S.Index)
                    DelimiterStack.removeAt(Opener)
                }

                // If the closing node is removed, reset current_position to the next element
                // in the stack. In any case, we need to update the node indices first.
                UpdateNodeIndices(O.S.Index)
                if (Closer.Count == 0) {
                    Nodes.removeAt(Closer.S.Index)
                    DelimiterStack.removeAt(CurrentPosition)
                    UpdateNodeIndices(Closer.S.Index)
                }
            } else {
                // If the closer at current_position is not a potential opener, remove it from the
                // delimiter stack (since we know it can’t be a closer either). Advance current_position
                // to the next element in the stack.
                if (!Closer!!.CanOpen) DelimiterStack.removeAt(CurrentPosition)
                else CurrentPosition++
            }
        }
    }

    /** Render Markdown AST to Component.  */
    private fun RenderNodes(Nodes: List<Node>): MutableComponent {
        val C = Component.empty()
        for (N in Nodes) {
            if (N is Span) C.append(Render(N))
            else if (N is Emph) C.append(Render(N))
            else if (N is LiteralText) C.append(Component.nullToEmpty(N.Text))
        }
        return C
    }

    /** Render an Emph to a Component.  */
    private fun Render(E: Emph) = RenderNodes(E.Children).withStyle(E.S)

    /** Render a Span to a Component.  */
    private fun Render(S: Span): Component {
        // Apply normalisation to code spans.
        if (S.IsCode) {
            // First, line endings are converted to spaces.
            var T = MD.substring(S.Start, S.End).replace('\n', ' ')

            // If the resulting string both begins and ends with a space character,
            // but does not consist entirely of space characters, a single space
            // character is removed from the front and back. This allows you to include
            // code that begins or ends with backtick characters, which must be separated
            // by whitespace from the opening or closing backtick strings.
            if (
                T.length > 2 && T[0] == ' ' && T[T.length - 1] == ' ' &&
                !T.chars().allMatch { it == ' '.code }
            ) T = T.substring(1, T.length - 1)

            // That’s all for code spans.
            return Component.literal(T)
        }

        // Regular text; process escapes.
        val SB = StringBuilder()
        val T = MD.substring(S.Start, S.End)
        var Pos = 0
        var StartOfText = 0
        while (true) {
            val Backslash = T.indexOf('\\', Pos)
            if (Backslash == -1 || Backslash == T.length - 1) {
                SB.append(T, StartOfText, T.length)
                break
            }

            // 2.4 Backslash escapes
            //
            // Any ASCII punctuation character may be backslash-escaped. Backslashes
            // before other characters are treated as literal backslashes and will
            // be appended before we break above at the latest.
            val Escaped = T[Backslash + 1]
            if (ESCAPABLE.contains(Escaped)) {
                SB.append(T, StartOfText, Backslash)
                SB.append(Escaped)
                StartOfText = Backslash + 2
            }

            // Move past the backslash.
            Pos = Backslash + 2
        }

        // Return the processed text.
        return Component.literal(SB.toString())
    }

    /** Set the stored index in a Span to that Span’s index in the Nodes array.  */
    private fun UpdateNodeIndices(StartingAt: Int) {
        for (I in StartingAt until Nodes.size)
            if (Nodes[I] is Span)
                (Nodes[I] as Span).Index = I
    }

    companion object {
        private val LOGGER: Logger = LogUtils.getLogger()
        private const val ESCAPABLE = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

        /** Render a Markdown string into a Component.  */
        fun Render(MD: String): MutableComponent {
            try {
                val P = MarkdownParser(MD)
                return P.RenderNodes(P.Nodes)
            } catch (E: Exception) {
                LOGGER.error("Error rendering markdown: {}", MD)
                E.printStackTrace()
                return Component.literal(MD)
            }
        }
    }
}
