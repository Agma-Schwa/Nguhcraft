package org.nguh.nguhcraft.client.render

import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.util.math.ColorHelper
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4fStack

/** RAII helper to avoid leaking rendering state. */
@Environment(EnvType.CLIENT)
object Renderer {
    fun ActOnSessionStart() {
        WorldRendering.ActOnSessionStart()
    }

    fun Init() {
        /*WorldRenderEvents.BEFORE_DEBUG_RENDER.register { Ctx -> WorldRendering.RenderWorld(Ctx) }
        HudRenderCallback.EVENT.register { Ctx, _ -> HUDRenderer.RenderHUD(Ctx) }*/
    }

    /*fun PushModelViewMatrix(Translation: Vec3d, C: (MS: Matrix4fStack) -> Unit) {
        val MS: Matrix4fStack = RenderSystem.getModelViewStack()
        MS.pushMatrix()
        MS.translate(Translation.x.toFloat(), Translation.y.toFloat(), Translation.z.toFloat())
        C(MS)
        MS.popMatrix()
    }

    fun SetShaderColour(colour: Int) {
        RenderSystem.setShaderColor(
            ColorHelper.getRed(colour) / 255.0f,
            ColorHelper.getGreen(colour) / 255.0f,
            ColorHelper.getBlue(colour) / 255.0f,
            ColorHelper.getAlpha(colour) / 255.0f
        )
    }*/
}
