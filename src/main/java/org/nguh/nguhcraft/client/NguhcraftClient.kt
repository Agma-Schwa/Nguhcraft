package org.nguh.nguhcraft.client

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.client.render.item.property.select.SelectProperties
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.server.command.CommandManager
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.block.ChestVariantProperty
import org.nguh.nguhcraft.block.NguhBlockModels
import org.nguh.nguhcraft.client.render.Renderer
import org.nguh.nguhcraft.client.render.WorldRendering

@Environment(EnvType.CLIENT)
class NguhcraftClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientNetworkHandler.Init()
        Renderer.Init()

        Registry.register(Registries.ITEM_GROUP, Id("treasures"), TREASURES_ITEM_GROUP)

        NguhBlockModels.InitRenderLayers()

        ClientCommandRegistrationCallback.EVENT.register { Dispatcher, _ ->
            Dispatcher.register(RenderCommand())
        }

        ServerLifecycleEvents.SERVER_STARTING.register {
            InHypershotContext = false
            BypassesRegionProtection = false
            Vanished = false
            LastInteractedLecternPos = BlockPos.ORIGIN
            Renderer.ActOnSessionStart()
        }

        SelectProperties.ID_MAPPER.put(Id("chest_variant"), ChestVariantProperty.TYPE)
    }

    companion object {
        val TREASURES_ITEM_GROUP: ItemGroup = net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
            .icon { ItemStack(Items.NETHERITE_INGOT) }
            .displayName(Text.literal("Treasures"))
            .entries {  Ctx, Entries -> Treasures.AddAll(Ctx, Entries) }
            .build()

        // FIXME: All of these should be attached to some singleton 'Session' object so
        //        they don’t accidentally persist across saves.
        @JvmField @Volatile var InHypershotContext = false
        @JvmField @Volatile var BypassesRegionProtection = false
        @JvmField @Volatile var Vanished = false
        @JvmField @Volatile var LastInteractedLecternPos: BlockPos = BlockPos.ORIGIN

        @JvmStatic
        fun ProcessF3(key: Int): Boolean {
            return false
        }

        fun RenderCommand(): LiteralArgumentBuilder<FabricClientCommandSource> = literal<FabricClientCommandSource>("render")
            .then(literal<FabricClientCommandSource>("regions")
                .executes {
                    WorldRendering.RenderRegions = !WorldRendering.RenderRegions
                    it.source.sendFeedback(Text.literal(
                        "Region rendering is now ${if (WorldRendering.RenderRegions) "enabled" else "disabled"}."
                    ).formatted(Formatting.YELLOW))
                    0
                }
            )
            .then(literal<FabricClientCommandSource>("spawns")
                .requires { it.player.hasPermissionLevel(4) }
                .executes {
                    WorldRendering.RenderSpawns = !WorldRendering.RenderSpawns
                    it.source.sendFeedback(Text.literal(
                        "Entity spawn rendering is now ${if (WorldRendering.RenderSpawns) "enabled" else "disabled"}."
                    ).formatted(Formatting.YELLOW))
                    0
                }
            )
    }
}
