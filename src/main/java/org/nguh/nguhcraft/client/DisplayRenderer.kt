package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Colors
import org.nguh.nguhcraft.client.ClientUtils.Client
import org.nguh.nguhcraft.client.accessors.DisplayData
import kotlin.math.min

@Environment(EnvType.CLIENT)
object DisplayRenderer {
    fun RenderHUD(Ctx: DrawContext) {
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
}