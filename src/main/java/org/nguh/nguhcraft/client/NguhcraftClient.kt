package org.nguh.nguhcraft.client

import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.item.property.select.SelectProperties
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Formatting
import net.minecraft.util.math.BlockPos
import org.nguh.nguhcraft.server.MCBASIC
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.block.ChestVariantProperty
import org.nguh.nguhcraft.block.NguhBlocks
import org.nguh.nguhcraft.client.ClientUtils.Client

@Environment(EnvType.CLIENT)
class NguhcraftClient : ClientModInitializer {
    override fun onInitializeClient() {
        ClientNetworkHandler.Init()

        WorldRenderEvents.BEFORE_DEBUG_RENDER.register { RegionRenderer.Render(it) }
        HudRenderCallback.EVENT.register {
            Ctx, _ ->
            RegionRenderer.RenderHUD(Ctx)
            DisplayRenderer.RenderHUD(Ctx)
            RenderVanishedMessage(Ctx)
        }

        Registry.register(Registries.ITEM_GROUP, Id("treasures"), TREASURES_ITEM_GROUP)

        BlockRenderLayerMap.INSTANCE.putBlock(NguhBlocks.LOCKED_DOOR, RenderLayer.getCutout())
        BlockRenderLayerMap.INSTANCE.putBlock(NguhBlocks.PEARLESCENT_LANTERN, RenderLayer.getCutout())
        BlockRenderLayerMap.INSTANCE.putBlock(NguhBlocks.PEARLESCENT_CHAIN, RenderLayer.getCutout())

        ClientCommandRegistrationCallback.EVENT.register { Dispatcher, _ ->
            Dispatcher.register(RenderCommand())
        }

        ServerLifecycleEvents.SERVER_STARTING.register {
            InHypershotContext = false
            BypassesRegionProtection = false
            Vanished = false
            LastInteractedLecternPos = BlockPos.ORIGIN
        }

        SelectProperties.ID_MAPPER.put(Id("chest_variant"), ChestVariantProperty.TYPE)
    }

    companion object {
        val TREASURES_ITEM_GROUP: ItemGroup = net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
            .icon { ItemStack(Items.NETHERITE_INGOT) }
            .displayName(Text.literal("Treasures"))
            .entries {  Ctx, Entries -> Treasures.AddAll(Ctx, Entries) }
            .build()

        const val VANISH_MSG = "You are currently vanished"

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
                    RegionRenderer.ShouldRender = !RegionRenderer.ShouldRender
                    it.source.sendFeedback(Text.literal(
                        "Region rendering is now ${if (RegionRenderer.ShouldRender) "enabled" else "disabled"}."
                    ).formatted(Formatting.YELLOW))
                    0
                }
            )

        fun RenderVanishedMessage(Ctx: DrawContext) {
            if (!Vanished) return
            val TR = Client().textRenderer
            Ctx.drawText(
                TR,
                VANISH_MSG,
                Ctx.scaledWindowWidth  - TR.getWidth(VANISH_MSG) - 5,
                TR.getWrappedLinesHeight(VANISH_MSG, 10000) - 5,
                Colors.LIGHT_YELLOW,
                true
            )
        }
    }
}
