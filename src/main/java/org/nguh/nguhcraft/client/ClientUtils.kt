package org.nguh.nguhcraft.client

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mojang.logging.LogUtils
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import java.text.Normalizer
import java.util.function.Consumer

inline fun MatrixStack.Push(Transformation: MatrixStack.() -> Unit) {
    push()
    Transformation()
    pop()
}

@Environment(EnvType.CLIENT)
private class EmojiTrie {
    class Node {
        val Children = mutableMapOf<Char, Node>()
        var Replacement: String? = null
        fun IsMatch() = Replacement != null
    }

    val Root = Node()
    constructor(Replacements: Map<String, String>) {
        for ((Seq, Repl) in Replacements) {
            var N = Root
            for (C in Seq) N = N.Children.getOrPut(C) { Node() }
            N.Replacement = Repl
        }
    }
}

@Environment(EnvType.CLIENT)
object ClientUtils {
    private val LOGGER = LogUtils.getLogger()

    /** 2000 is reasonable, and we can still send it to Discord this way. */
    const val MAX_CHAT_LENGTH = 2000

    /**
     * Emojis we support.
     *
     * Some of these are our server emojis, but the images and data for all
     * Unicode emoji are taken from https://github.com/AmberWat/PixelTwemojiMC-9.
     */
    val EMOJI_REPLACEMENTS = buildMap {
        putAll(LoadEmojiReplacements("/assets/nguhcraft/emoji/twemoji.json"))
        putAll(LoadEmojiReplacements("/assets/nguhcraft/emoji/nguhmoji.json"))
    }

    /**
     * Trie containing multi-codepoint sequences that we map to single codepoints.
     *
     * Rather than doing this ‘properly’, we cheat by 1. not having failure links
     * because the codepoints that appear in an emoji are unlikely to occur on their
     * own, and 2. by continuing the search if we see a ZWJ after a match.
     */
    private val MULTI_CODEPOINT_REPLACEMENTS = EmojiTrie(
        LoadEmojiReplacements("/assets/nguhcraft/emoji/replacements.json")
    )

    private val LORE_STYLE = Style.EMPTY.withItalic(false).withFormatting(Formatting.GRAY)

    /** Get the client instance. */
    @JvmStatic
    fun Client(): MinecraftClient = MinecraftClient.getInstance()

    /** Format lore text in a tooltip. */
    @JvmStatic
    fun FormatLoreForTooltip(Consumer: Consumer<Text>, Lines: List<Text>): Boolean {
        // This is terrible; if only Mojang would just implement this properly...
        if (Lines.size == 1) {
            val S = Lines.first().string
            if (S.startsWith('\u0001')) {
                for (Line in S.slice(1..<S.length).split('\n'))
                    Consumer.accept(Text.literal(Line).setStyle(LORE_STYLE))
                return true
            }
        }

        return false
    }

    /** Load emoji replacement data from a resource. */
    private fun LoadEmojiReplacements(Path: String): Map<String, String> = Gson().fromJson(
        ClientUtils.javaClass.getResource(Path)!!.readText(),
        object : TypeToken<Map<String, String>>() {}.type
    )

    /** Zero-width joiner. */
    private const val ZWJ = '\u200D'

    /** Preprocess text for rendering. */
    @JvmStatic
    fun RenderText(In: String) = try {
        RenderTextImpl(In)
    } catch (E: Exception) {
        LOGGER.error("Failed to render text: $In", E)
        In
    }

    private fun RenderTextImpl(In: String): String {
        // Normalise to NFC so we can render at least *some*
        // combining characters as precomposed glyphs.
        val Normalised = Normalizer.normalize(In, Normalizer.Form.NFC)

        // Avoid allocating a string if there are no emojis to replace.
        val HasNamedEmojis = run {
            val First = Normalised.indexOf(':')
            First != -1 && Normalised.indexOf(':', First + 1) != -1
        }

        // Render named emojis. Fortunately, they are delimited by ':'s, which
        // means we can just do a lookup rather than having to construct e.g.
        // a trie to find arbitrary substrings.
        val Replaced = if (HasNamedEmojis) buildString {
            var Pos = 0
            while (true) {
                // Find the start and end of the next emoji.
                var ColonPos = Normalised.indexOf(':', Pos)
                if (ColonPos == -1) break
                val NextColonPos = Normalised.indexOf(':', ColonPos + 1)
                if (NextColonPos == -1) break

                // Try to find the emoji replacement.
                val Emoji = EMOJI_REPLACEMENTS[Normalised.substring(ColonPos + 1, NextColonPos)]

                // If we have an emoji, append it and everything before it.
                if (Emoji != null) {
                    append(Normalised.substring(Pos, ColonPos))
                    append(Emoji)
                }

                // Otherwise, just append the text literally.
                else append(Normalised.substring(Pos, NextColonPos + 1))

                // And keep searching after the second colon.
                Pos = NextColonPos + 1
            }

            // Append any remaining text.
            if (Pos < Normalised.length) append(Normalised.substring(Pos))
        } else Normalised

        // Replace multi-codepoint emojis.
        return buildString {
            var Depth = 0
            var N = MULTI_CODEPOINT_REPLACEMENTS.Root
            for (I in 0..<Replaced.length) {
                val C = Replaced[I]
                val Child = N.Children[C]

                // If this is not in the trie, append the current character as
                // well as any characters that are part of the current sequence
                // if we’re not at the root.
                if (Child == null) {
                    N = MULTI_CODEPOINT_REPLACEMENTS.Root
                    if (Depth != 0) {
                        append(Replaced, I - Depth, I /* Include C here */ + 1)
                        Depth = 0
                    } else {
                        append(C)
                    }
                }

                // Keep going if we don’t have a match yet or if the next char
                // is a ZWJ that is also in the trie.
                else if (
                    !Child.IsMatch() || (
                        I + 1 < Replaced.length &&
                        Replaced[I + 1] == ZWJ &&
                        Child.Children[ZWJ] != null
                    )
                ) {
                    Depth++
                    N = Child
                }

                // This is a match with no ZWJ in sight; ship it out.
                else {
                    append(Child.Replacement)
                    N = MULTI_CODEPOINT_REPLACEMENTS.Root
                    Depth = 0
                }
            }

            // Append any remaining characters.
            if (Depth != 0) append(Replaced, Replaced.length - Depth, Replaced.length)
        }
    }
}