package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.MinecraftClient
import net.minecraft.client.util.math.MatrixStack
import java.text.Normalizer

inline fun MatrixStack.Push(Transformation: MatrixStack.() -> Unit) {
    push()
    Transformation()
    pop()
}

@Environment(EnvType.CLIENT)
object ClientUtils {
    /** 2000 is reasonable, and we can still send it to Discord this way. */
    const val MAX_CHAT_LENGTH = 2000

    /** Get the client instance. */
    @JvmStatic
    fun Client(): MinecraftClient = MinecraftClient.getInstance()

    /** Preprocess text for rendering. */
    @JvmStatic
    fun RenderText(In: String): String {
        // Normalise to NFC so we can render at least *some*
        // combining characters as precomposed glyphs.
        val Normalised = Normalizer.normalize(In, Normalizer.Form.NFC)
        return Normalised

/*        // For our custom font, we need to do this manually.
        //
        // The strings passed to the builder here are normalised
        // by it, so we don’t need to worry about that here.
        val Trie = ReplacementTrie.Builder()
            .Add("ụ̄", "\uE59E")
            .Build()

        // Replace all occurrences of the patterns.
        return Trie.Replace(Normalised)*/
    }
}