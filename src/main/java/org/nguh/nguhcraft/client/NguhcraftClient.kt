package org.nguh.nguhcraft.client

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.ChatFormatting
import net.minecraft.client.particle.FlameParticle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties
import net.minecraft.core.BlockPos
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.network.chat.Component
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.block.ChestVariantProperty
import org.nguh.nguhcraft.block.NguhBlockModels
import org.nguh.nguhcraft.client.render.Renderer
import org.nguh.nguhcraft.client.render.WorldRendering
import org.nguh.nguhcraft.particle.NguhParticles

@Environment(EnvType.CLIENT)
class NguhcraftClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientNetworkHandler.Init()
        Renderer.Init()
        NguhcraftItemGroups.Init()
        NguhBlockModels.InitRenderLayers()
        NguhBlockModels.InitColorRegistry()
        NguhParticles.ClientSideInitialisation()

        ClientCommandRegistrationCallback.EVENT.register { Dispatcher, _ ->
            Dispatcher.register(RenderCommand())
        }

        ServerLifecycleEvents.SERVER_STARTING.register {
            InHypershotContext = false
            BypassesRegionProtection = false
            Vanished = false
            LastInteractedLecternPos = BlockPos.ZERO
            Renderer.ActOnSessionStart()
        }

        SelectItemModelProperties.ID_MAPPER.put(Id("chest_variant"), ChestVariantProperty.TYPE)
    }

    companion object {
        // FIXME: All of these should be attached to some singleton 'Session' object so
        //        they don’t accidentally persist across saves.
        @JvmField @Volatile var InHypershotContext = false
        @JvmField @Volatile var BypassesRegionProtection = false
        @JvmField @Volatile var Vanished = false
        @JvmField @Volatile var LastInteractedLecternPos: BlockPos = BlockPos.ZERO

        @JvmStatic
        fun ProcessF3(key: Int): Boolean {
            return false
        }

        fun RenderCommand(): LiteralArgumentBuilder<FabricClientCommandSource> = literal<FabricClientCommandSource>("render")
            .then(literal<FabricClientCommandSource>("regions")
                .executes {
                    WorldRendering.RenderRegions = !WorldRendering.RenderRegions
                    it.source.sendFeedback(Component.literal(
                        "Region rendering is now ${if (WorldRendering.RenderRegions) "enabled" else "disabled"}."
                    ).withStyle(ChatFormatting.YELLOW))
                    0
                }
            )
            .then(literal<FabricClientCommandSource>("spawns")
                .requires { it.player.hasPermissions(4) }
                .executes {
                    WorldRendering.RenderSpawns = !WorldRendering.RenderSpawns
                    it.source.sendFeedback(Component.literal(
                        "Entity spawn rendering is now ${if (WorldRendering.RenderSpawns) "enabled" else "disabled"}."
                    ).withStyle(ChatFormatting.YELLOW))
                    0
                }
            )
    }
}
