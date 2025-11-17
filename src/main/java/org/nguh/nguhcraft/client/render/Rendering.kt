package org.nguh.nguhcraft.client.render

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.renderer.debug.DebugRenderer

/** RAII helper to avoid leaking rendering state. */
@Environment(EnvType.CLIENT)
object Renderer {
    fun ActOnSessionStart() {
        WorldRendering.ActOnSessionStart()
    }

    fun Init() {
        WorldRenderEvents.BEFORE_DEBUG_RENDER.register { Ctx -> WorldRendering.RenderWorld(Ctx) }
        WorldRendering.Init()
        HUDRenderer.Init()
    }
}
