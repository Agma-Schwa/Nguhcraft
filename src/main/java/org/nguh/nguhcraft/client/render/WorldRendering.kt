package org.nguh.nguhcraft.client.render

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.client.render.VertexConsumer
import net.minecraft.util.Colors
import net.minecraft.util.Formatting
import net.minecraft.util.Util
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.Direction
import net.minecraft.util.profiler.Profilers
import org.nguh.nguhcraft.Constants
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.unaryMinus
import kotlin.math.min

@Environment(EnvType.CLIENT)
object WorldRendering {
    var RenderRegions = false
    private const val GOLD = 0xFFFFAA00.toInt()

    fun RenderWorld(Ctx: WorldRenderContext) {
        Profilers.get().push("nguhcraft:world_rendering")

        // Transform all points relative to the camera position.
        Renderer.PushModelViewMatrix(-Ctx.camera().pos) {
            val DT = -(Util.getMeasuringTimeMs() % 3000L).toFloat() / 3000.0f

            // Render barriers.
            Layers.BARRIERS.Use { RenderBarriers(Ctx, it, DT) }

            // Render regions.
            Layers.REGION_LINES.Use { RenderRegions(Ctx, it) }
        }

        Profilers.get().pop()
    }

    private fun RenderBarriers(Ctx: WorldRenderContext, VA: VertexAllocator, DT: Float) {
        val CW = Ctx.world()
        val WR = Ctx.worldRenderer()
        val MinY = CW.bottomY
        val MaxY = CW.topYInclusive + 1
        val CameraPos = Ctx.camera().pos

        // Render barriers for each region.
        for (R in ProtectionManager.GetRegions(CW)) {
            if (!R.ShouldRenderEntryExitBarrier()) continue
            if (R.World != CW.registryKey) continue
            if (R.DistanceFrom(CameraPos) > WR.viewDistance * 16) continue
            VA.Draw { RenderBarrier(it, R, MinY = MinY, MaxY = MaxY, DT) }
        }
    }

    private fun RenderBarrier(VC: VertexConsumer, R: Region, MinY: Int, MaxY: Int, DT: Float) {
        val Colour = when {
            R.ColourOverride != null -> R.ColourOverride!!
            !R.AllowsPlayerEntry() && !R.AllowsPlayerExit() -> GOLD
            !R.AllowsPlayerExit() -> Colors.LIGHT_RED
            !R.AllowsPlayerEntry() -> Colors.CYAN
            else -> return
        }

        // Set shader colour. This is why we need to render each barrier separately.
        Renderer.SetShaderColour(Colour)

        // Helper to add a quad from (x, y, z).
        val MinX = R.MinX
        val MaxX = R.OutsideMaxX
        val MinZ = R.MinZ
        val MaxZ = R.OutsideMaxZ

        // Helper to draw a 2×2 quad in the XY or ZY plane.
        fun Quad(Dir: Direction, X: Int, Y: Int, Z: Int, InvertScroll: Boolean) {
            // Make sure we don’t draw outside the bounds of the barrier.
            val EndX = min(MaxX, X + Dir.offsetX * 2)
            val EndY = Y + 2
            val EndZ = min(MaxZ, Z + Dir.offsetZ * 2)

            // Clamp the U coordinate so we don’t stretch the texture if the last
            // square happens to be 1×2 instead of 2×2.
            var U = DT
            var EndU = U + (if (Dir == Direction.EAST) EndX - X else EndZ - Z).toFloat() / 2
            if (InvertScroll) {
                U = EndU
                EndU = DT
            }

            // Draw the quad.
            VC.vertex(X.toFloat(), Y.toFloat(), Z.toFloat()).texture(U, DT)
            VC.vertex(EndX.toFloat(), Y.toFloat(), EndZ.toFloat()).texture(EndU, DT)
            VC.vertex(EndX.toFloat(), EndY.toFloat(), EndZ.toFloat()).texture(EndU, DT + 1F)
            VC.vertex(X.toFloat(), EndY.toFloat(), Z.toFloat()).texture(U, DT + 1F)
        }

        // XY min.
        for (X in MinX..<MaxX step 2)
            for (Y in MinY..<MaxY step 2)
                Quad(Direction.EAST, X, Y, MinZ, false)

        // XY max.
        for (X in MinX..<MaxX step 2)
            for (Y in MinY..<MaxY step 2)
                Quad(Direction.EAST, X, Y, MaxZ, true)

        // ZY min.
        for (Z in MinZ..<MaxZ step 2)
            for (Y in MinY..<MaxY step 2)
                Quad(Direction.SOUTH, MinX, Y, Z, true)

        // ZY max.
        for (Z in MinZ..<MaxZ step 2)
            for (Y in MinY..<MaxY step 2)
                Quad(Direction.SOUTH, MaxX, Y, Z, false)
    }

    private fun RenderRegions(Ctx: WorldRenderContext, VA: VertexAllocator) {
        if (!RenderRegions) return
        val CW = Ctx.world()
        val WR = Ctx.worldRenderer()
        val MinY = CW.bottomY
        val MaxY = CW.topYInclusive + 1
        val CameraPos = Ctx.camera().pos

        // Render each region.
        Renderer.SetShaderColour(Colors.WHITE)
        for (R in ProtectionManager.GetRegions(CW)) {
            if (R.DistanceFrom(CameraPos) > WR.viewDistance * 16) continue
            VA.Draw { RenderRegion(it, R, Colour = R.ColourOverride ?: Colors.LIGHT_YELLOW, MinY = MinY, MaxY = MaxY) }
        }
    }

    private fun RenderRegion(VC: VertexConsumer, R: Region, Colour: Int, MinY: Int, MaxY: Int) {
        val MinX = R.MinX
        val MaxX = R.OutsideMaxX
        val MinZ = R.MinZ
        val MaxZ = R.OutsideMaxZ

        // Helper to add a vertex.
        fun Vertex(X: Int, Y: Int, Z: Int) = VC.vertex(
            X.toFloat(),
            Y.toFloat(),
            Z.toFloat()
        ).color(Colour)

        // Vertical lines along X axis.
        for (X in MinX.toInt()..MaxX.toInt()) {
            Vertex(X, MinY, MinZ).color(Colour)
            Vertex(X, MaxY, MinZ).color(Colour)
            Vertex(X, MinY, MaxZ).color(Colour)
            Vertex(X, MaxY, MaxZ).color(Colour)
        }

        // Vertical lines along Z axis.
        for (Z in MinZ.toInt()..MaxZ.toInt()) {
            Vertex(MinX, MinY, Z).color(Colour)
            Vertex(MinX, MaxY, Z).color(Colour)
            Vertex(MaxX, MinY, Z).color(Colour)
            Vertex(MaxX, MaxY, Z).color(Colour)
        }

        // Horizontal lines.
        for (Y in MinY.toInt()..MaxY.toInt()) {
            Vertex(MinX, Y, MinZ).color(Colour)
            Vertex(MaxX, Y, MinZ).color(Colour)
            Vertex(MinX, Y, MaxZ).color(Colour)
            Vertex(MaxX, Y, MaxZ).color(Colour)
            Vertex(MinX, Y, MinZ).color(Colour)
            Vertex(MinX, Y, MaxZ).color(Colour)
            Vertex(MaxX, Y, MinZ).color(Colour)
            Vertex(MaxX, Y, MaxZ).color(Colour)
        }
    }
}