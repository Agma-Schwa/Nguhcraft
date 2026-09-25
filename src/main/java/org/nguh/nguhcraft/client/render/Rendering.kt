package org.nguh.nguhcraft.client.render

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents
import net.minecraft.client.renderer.debug.DebugRenderer

/** RAII helper to avoid leaking rendering state. */
@Environment(EnvType.CLIENT)
object Renderer {
    fun ActOnSessionStart() {
        WorldRendering.ActOnSessionStart()
    }

    fun Init() {
        // Not sure when it is best to call this but BEFORE_GIZMOS seemed good.
        LevelRenderEvents.BEFORE_GIZMOS.register { Ctx -> WorldRendering.RenderWorld(Ctx) }
        WorldRendering.Init()
        HUDRenderer.Init()
    }
}
