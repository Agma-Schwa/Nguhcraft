package org.nguh.nguhcraft.client.render

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.DeltaTracker
import net.minecraft.util.CommonColors
import net.minecraft.core.BlockPos
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
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
    private val EL_REGION_NAME = Id("region_name")
    private val EL_DISPLAY = Id("display")
    private val EL_VANISHED = Id("vanished")

    fun Init() {
        HudElementRegistry.addFirst(EL_REGION_NAME, ::RenderActiveDisplay)
        HudElementRegistry.attachElementAfter(EL_REGION_NAME, EL_DISPLAY, ::RenderRegionName)
        HudElementRegistry.attachElementAfter(EL_DISPLAY, EL_VANISHED, ::RenderVanishedMessage)
    }

    private fun RenderActiveDisplay(Ctx: GuiGraphics, RTC: DeltaTracker) {
        val C = Client()
        val D = C.DisplayData ?: return
        if (D.Lines.isEmpty()) return

        // Compute height and width of the display.
        val TR = C.font
        val WindowWd = Ctx.guiWidth()
        val WindowHt = Ctx.guiHeight()
        val MaxWd = WindowWd / 2
        val Height = D.Lines.sumOf { TR.wordWrapHeight(it, MaxWd) }
        val Width = min(D.Lines.maxOf { TR.width(it) }, WindowHt / 2)

        // Center the display in vertically on the right side of the screen.
        val X = WindowWd - Width - 2
        var Y = (WindowHt - Height) / 2
        Ctx.fill(X - 2, Y - 2, X + Width + 2, Y + Height + 2, C.options.getBackgroundColor(.3f))

        // Render each line of the display.
        for (Line in D.Lines) {
            Ctx.drawWordWrap(TR, Line, X, Y, MaxWd, CommonColors.WHITE)
            Y += TR.wordWrapHeight(Line, MaxWd)
        }
    }

    private fun RenderRegionName(Ctx: GuiGraphics, RTC: DeltaTracker) {
        if (!RenderRegions) return

        // Check if we’re in a region.
        val C = Client()
        val PlayerPos = BlockPos.containing(C.player?.position() ?: return)
        val PlayerRegion = ProtectionManager.FindRegionContainingBlock(C.level!!, PlayerPos) ?: return

        // If so, draw it in the bottom-right corner.
        val TextToRender = "Region: ${PlayerRegion.Name}"
        val Width = C.font.width(TextToRender)
        val Height = C.font.lineHeight
        Ctx.drawString(
            C.font,
            TextToRender,
            Ctx.guiWidth() - PADDING - Width,
            Ctx.guiHeight() - PADDING - Height,
            CommonColors.SOFT_YELLOW,
            true
        )
    }

    private fun RenderVanishedMessage(Ctx: GuiGraphics, RTC: DeltaTracker) {
        if (!NguhcraftClient.Vanished) return
        val TR = Client().font
        Ctx.drawString(
            TR,
            VANISH_MSG,
            Ctx.guiWidth() - TR.width(VANISH_MSG) - 5,
            TR.wordWrapHeight(VANISH_MSG, 10000) - 5,
            CommonColors.SOFT_YELLOW,
            true
        )
    }
}