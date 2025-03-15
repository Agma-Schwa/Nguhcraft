package org.nguh.nguhcraft.client.render

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Colors
import net.minecraft.util.math.BlockPos
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.client.NguhcraftClient
import org.nguh.nguhcraft.client.accessors.DisplayData
import org.nguh.nguhcraft.client.render.WorldRendering.RenderRegions
import org.nguh.nguhcraft.protect.ProtectionManager
import kotlin.math.min

@Environment(EnvType.CLIENT)
object HUDRenderer {
    private const val PADDING = 2
    private const val VANISH_MSG = "You are currently vanished"

    fun RenderHUD(Ctx: DrawContext) {
        RenderRegionName(Ctx)
        RenderActiveDisplay(Ctx)
        RenderVanishedMessage(Ctx)
    }

    private fun RenderActiveDisplay(Ctx: DrawContext) {
        val C = Client()
        val D = C.DisplayData ?: return
        if (D.Lines.isEmpty()) return

        // Compute height and width of the display.
        val TR = C.textRenderer
        val WindowWd = Ctx.scaledWindowWidth
        val WindowHt = Ctx.scaledWindowHeight
        val MaxWd = WindowWd / 2
        val Height = D.Lines.sumOf { TR.getWrappedLinesHeight(it, MaxWd) }
        val Width = min(D.Lines.maxOf { TR.getWidth(it) }, WindowHt / 2)

        // Center the display in vertically on the right side of the screen.
        val X = WindowWd - Width - 2
        var Y = (WindowHt - Height) / 2
        Ctx.fill(X - 2, Y - 2, X + Width + 2, Y + Height + 2, C.options.getTextBackgroundColor(.3f))

        // Render each line of the display.
        for (Line in D.Lines) {
            Ctx.drawWrappedTextWithShadow(TR, Line, X, Y, MaxWd, Colors.WHITE)
            Y += TR.getWrappedLinesHeight(Line, MaxWd)
        }
    }

    private fun RenderRegionName(Ctx: DrawContext) {
        if (!RenderRegions) return

        // Check if we’re in a region.
        val C = Client()
        val PlayerPos = BlockPos.ofFloored(C.player?.pos ?: return)
        val PlayerRegion = ProtectionManager.FindRegionContainingBlock(C.world!!, PlayerPos)
        if (PlayerRegion == null) return

        // If so, draw it in the bottom-right corner.
        val TextToRender = "Region: ${PlayerRegion.Name}"
        val Width = C.textRenderer.getWidth(TextToRender)
        val Height = C.textRenderer.fontHeight
        Ctx.drawText(
            C.textRenderer,
            TextToRender,
            Ctx.scaledWindowWidth - PADDING - Width,
            Ctx.scaledWindowHeight - PADDING - Height,
            Colors.LIGHT_YELLOW,
            true
        )
    }

    private fun RenderVanishedMessage(Ctx: DrawContext) {
        if (!NguhcraftClient.Vanished) return
        val TR = Client().textRenderer
        Ctx.drawText(
            TR,
            VANISH_MSG,
            Ctx.scaledWindowWidth  - TR.getWidth(VANISH_MSG) - 5,
            TR.getWrappedLinesHeight(VANISH_MSG, 10000) - 5,
            Colors.LIGHT_YELLOW,
            true
        )
    }
}