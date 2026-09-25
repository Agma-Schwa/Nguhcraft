package org.nguh.nguhcraft.client.render

import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.renderpearl.api.pipeline.ColorTargetState
import com.mojang.renderpearl.api.pipeline.DepthStencilState
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology
import com.mojang.renderpearl.api.pipeline.RenderPipeline
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.CommonColors
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.client.renderer.WorldBorderRenderer
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType
import net.minecraft.client.renderer.rendertype.RenderTypes
import net.minecraft.util.ARGB
import net.minecraft.util.Util
import net.minecraft.util.profiling.Profiler
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Matrix4f
import org.joml.Vector4f
import org.joml.Vector4fc
import org.nguh.nguhcraft.client.Push
import org.nguh.nguhcraft.entity.EntitySpawnManager
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.protect.Region
import org.nguh.nguhcraft.unaryMinus
import java.util.*
import kotlin.math.absoluteValue

@Environment(EnvType.CLIENT)
interface RenderLayerMultiPhaseShaderColourAccessor {
    fun `Nguhcraft$SetShaderColour`(Colour: Vector4fc)
}

@Environment(EnvType.CLIENT)
object WorldRendering {
    private const val GOLD = 0xFFFFAA00.toInt()

    // =========================================================================
    //  Render Data
    // =========================================================================
    @JvmField @Volatile var Spawns = listOf<EntitySpawnManager.Spawn>()
    var RenderRegions = false
    var RenderSpawns = false

    // =========================================================================
    //  Pipelines and Layers
    // =========================================================================
    val POSITION_COLOR_LINES_PIPELINE: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder()
            .withBindGroupLayout(BindGroupLayouts.DYNAMIC_TRANSFORMS)
            .withBindGroupLayout(BindGroupLayouts.PROJECTION)
            .withLocation("pipeline/debug_line_strip")
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withCull(false)
            .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
            .withPrimitiveTopology(PrimitiveTopology.DEBUG_LINES)
            .withColorTargetState(ColorTargetState.DEFAULT)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build()
    )

    val REGION_LINES: RenderType = RenderType.create(
        "nguhcraft:region_lines",
        RenderSetup.builder(POSITION_COLOR_LINES_PIPELINE)
            .createRenderSetup()
    )

    val REGION_BARRIERS: RenderType = RenderType.create(
        "nguhcraft:barriers",
        RenderSetup.builder(RenderPipelines.WORLD_BORDER)
            .withTexture("Sampler0", WorldBorderRenderer.FORCEFIELD_LOCATION)
            .useLightmap()
            .createRenderSetup()
    )

    // =========================================================================
    //  Setup
    // =========================================================================
    fun ActOnSessionStart() {
        Spawns = listOf()
        RenderRegions = false
        RenderSpawns = false
    }

    fun Init() {
        // Does nothing but must be called early to run static constructors.
    }

    fun RenderWorld(Ctx: LevelRenderContext) {
        Profiler.get().push("nguhcraft:world_rendering")
        val PS = Ctx.poseStack()
        PS.Push {
            // Transform all points relative to the camera position.
            translate(-Ctx.levelState().cameraRenderState.pos)

            // Render barriers.
            val DT = -(Util.getMillis() % 3000L).toFloat() / 3000.0f
            RenderBarriers(Ctx, DT)

            // Render regions.
            if (RenderRegions) RenderRegions(Ctx)

            // Render spawn positions.
            if (RenderSpawns) RenderSpawns(Ctx)
        }
        Profiler.get().pop()
    }

    // =========================================================================
    //  Region Barriers
    // =========================================================================
    private fun RenderBarriers(Ctx: LevelRenderContext, DT: Float) {
        val CL = Minecraft.getInstance().level!!
        val MinY = CL.minY
        val MaxY = CL.maxY + 1
        val CameraPos = Ctx.levelState().cameraRenderState.pos
        val PS = Ctx.poseStack()
        val MaxDist = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16
        val Collector = Ctx.submitNodeCollector()

        // Render barriers for each region.
        for (R in ProtectionManager.GetRegions(CL)) {
            if (R.DistanceFrom(CameraPos) > MaxDist * 16) continue
            val Colour = R.BarrierColor() ?: continue
            Collector.submitCustomGeometry(PS, REGION_BARRIERS) { Pose, VC ->
                RenderBarrier(VC, Pose.pose(), R, Colour, MinY = MinY, MaxY = MaxY, DT)
            }
//            val VC = Tesselator.getInstance().begin(REGION_BARRIERS.mode(), REGION_BARRIERS.format())
//            RenderBarrier(VC, MTX, R, Colour, MinY = MinY, MaxY = MaxY, DT)
//            REGION_BARRIERS.draw(VC.build() ?: continue)
        }
    }

    private fun RenderBarrier(VC: VertexConsumer, MTX: Matrix4f, R: Region, Colour: Int, MinY: Int, MaxY: Int, DT: Float) {
        // Set shader colour. This is why we need to render each barrier separately.
        (REGION_BARRIERS as RenderLayerMultiPhaseShaderColourAccessor).`Nguhcraft$SetShaderColour`(
            Vector4f(
                ARGB.redFloat(Colour),
                ARGB.greenFloat(Colour),
                ARGB.blueFloat(Colour),
                1.0f
            )
        )

        val MinX = R.MinX
        val MaxX = R.OutsideMaxX
        val MinZ = R.MinZ
        val MaxZ = R.OutsideMaxZ
        val DX = (MinX - MaxX).toFloat().absoluteValue / 2
        val DY = (MinY - MaxY).toFloat().absoluteValue / 2
        val DZ = (MinZ - MaxZ).toFloat().absoluteValue / 2
        fun Quad(X1: Int, Y1: Int, X2: Int, Y2: Int, Z1: Int, Z2: Int, InvertScroll: Boolean) {
            var U = DT
            var EndU = U + if (X1 == X2) DZ else DX
            if (InvertScroll) {
                U = EndU
                EndU = DT
            }

            VC.addVertex(MTX, X1.toFloat(), Y1.toFloat(), Z1.toFloat()).setUv(U, DT)
            VC.addVertex(MTX, X2.toFloat(), Y1.toFloat(), Z2.toFloat()).setUv(EndU, DT)
            VC.addVertex(MTX, X2.toFloat(), Y2.toFloat(), Z2.toFloat()).setUv(EndU, DT + DY)
            VC.addVertex(MTX, X1.toFloat(), Y2.toFloat(), Z1.toFloat()).setUv(U, DT + DY)
        }

        Quad(MinX, MinY, MaxX, MaxY, MinZ, MinZ, false)
        Quad(MinX, MinY, MaxX, MaxY, MaxZ, MaxZ, true)
        Quad(MinX, MinY, MinX, MaxY, MinZ, MaxZ, true)
        Quad(MaxX, MinY, MaxX, MaxY, MinZ, MaxZ, false)
    }

    // =========================================================================
    //  Region Outlines
    // =========================================================================
    private fun RenderRegions(Ctx: LevelRenderContext) {
        val CL = Minecraft.getInstance().level ?: return
        val MinY = CL.minY
        val MaxY = CL.maxY + 1
        val CameraPos = Ctx.levelState().cameraRenderState.pos
        val MaxDist = Minecraft.getInstance().options.getEffectiveRenderDistance() * 16

        Ctx.submitNodeCollector().submitCustomGeometry(Ctx.poseStack(), REGION_LINES) { Pose, VC ->
            for (R in ProtectionManager.GetRegions(CL)) {
                if (R.ShouldRenderEntryExitBarrier()) continue
                if (R.DistanceFrom(CameraPos) > MaxDist) continue
                RenderRegion(VC, Pose.pose(), R, Colour = R.ColourOverride ?: CommonColors.SOFT_YELLOW, MinY = MinY, MaxY = MaxY)
            }
        }
    }

    private fun RenderRegion(VC: VertexConsumer, MTX: Matrix4f, R: Region, Colour: Int, MinY: Int, MaxY: Int) {
        val MinX = R.MinX
        val MaxX = R.OutsideMaxX
        val MinZ = R.MinZ
        val MaxZ = R.OutsideMaxZ

        // Helper to add a vertex.
        fun Vertex(X: Int, Y: Int, Z: Int) = VC.addVertex(
            MTX,
            X.toFloat(),
            Y.toFloat(),
            Z.toFloat()
        ).setColor(Colour)

        // Vertical lines along X axis.
        for (X in MinX..MaxX) {
            Vertex(X, MinY, MinZ)
            Vertex(X, MaxY, MinZ)
            Vertex(X, MinY, MaxZ)
            Vertex(X, MaxY, MaxZ)
        }

        // Vertical lines along Z axis.
        for (Z in MinZ..MaxZ) {
            Vertex(MinX, MinY, Z)
            Vertex(MinX, MaxY, Z)
            Vertex(MaxX, MinY, Z)
            Vertex(MaxX, MaxY, Z)
        }

        // Horizontal lines.
        for (Y in MinY..MaxY) {
            Vertex(MinX, Y, MinZ)
            Vertex(MaxX, Y, MinZ)
            Vertex(MinX, Y, MaxZ)
            Vertex(MaxX, Y, MaxZ)
            Vertex(MinX, Y, MinZ)
            Vertex(MinX, Y, MaxZ)
            Vertex(MaxX, Y, MinZ)
            Vertex(MaxX, Y, MaxZ)
        }
    }

    private val SPAWN_BOX: VoxelShape = Shapes.box(-.15, .15, -.15, .15, .45, .15)
    private val SPAWN_COLOUR = ARGB.colorFromFloat(1f, .4f, .4f, .8f)

    private fun RenderSpawns(Ctx: LevelRenderContext) {
        val PS = Ctx.poseStack()
        val Cam = Ctx.levelState().cameraRenderState.pos
        val Collector = Ctx.submitNodeCollector()

        for (S in Spawns) {
            PS.pushPose()
            PS.translate(S.SpawnPos.x - Cam.x, S.SpawnPos.y - Cam.y, S.SpawnPos.z - Cam.z)
            Collector.submitShapeOutline(PS, SPAWN_BOX, RenderTypes.lines(), SPAWN_COLOUR, 2f, false)
            PS.popPose()
        }
    }
}