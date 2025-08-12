package org.nguh.nguhcraft.client.render

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.render.*
import net.minecraft.client.render.RenderLayer.*
import net.minecraft.util.Colors
import net.minecraft.util.TriState

@Environment(EnvType.CLIENT)
class VertexAllocator(private val L: RenderLayer) {
    fun Draw(C: (VertexConsumer) -> Unit) {
        val VC = Tessellator.getInstance().begin(L.drawMode, L.vertexFormat)
        C(VC)
        val Vertices = VC.endNullable()
        if (Vertices != null) BufferRenderer.drawWithGlobalProgram(Vertices)

        // Reset the shader colour to our rendering from accidentally tinting other things.
        Renderer.SetShaderColour(Colors.WHITE)
    }
}

@Environment(EnvType.CLIENT)
object Layers {
    class Wrapper(private val L: RenderLayer) {
        fun Use(C: (VC: VertexAllocator) -> Unit) {
            L.startDrawing()
            C(VertexAllocator(L))
            L.endDrawing()
        }
    }

    val BARRIERS = Wrapper(of(
        "nguhcraft:barriers",
        VertexFormats.POSITION_TEXTURE,
        VertexFormat.DrawMode.QUADS,
        DEFAULT_BUFFER_SIZE,
        false,
        true,
        MultiPhaseParameters.builder()
            .program(POSITION_TEXTURE_PROGRAM)
            .texture(RenderPhase.Texture(WorldBorderRendering.FORCEFIELD, TriState.FALSE, false))
            .transparency(OVERLAY_TRANSPARENCY)
            .lightmap(ENABLE_LIGHTMAP)
            .target(WEATHER_TARGET)
            .writeMaskState(ALL_MASK)
            .layering(WORLD_BORDER_LAYERING)
            .cull(DISABLE_CULLING)
            .build(false)
    ))

    val LINES = Wrapper(getLines())

    val REGION_LINES = Wrapper(of(
        "nguhcraft:region_lines",
        VertexFormats.POSITION_COLOR,
        VertexFormat.DrawMode.DEBUG_LINES,
        DEFAULT_BUFFER_SIZE,
        false,
        true,
        MultiPhaseParameters.builder()
            .program(POSITION_COLOR_PROGRAM)
            .transparency(NO_TRANSPARENCY)
            .lineWidth(FULL_LINE_WIDTH)
            .cull(DISABLE_CULLING)
            .build(false)
    ))
}