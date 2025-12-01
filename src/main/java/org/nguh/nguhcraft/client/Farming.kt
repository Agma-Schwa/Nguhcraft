package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.level.block.Block
import net.minecraft.world.item.Item
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.ItemStack
import org.nguh.nguhcraft.block.NguhBlocks
import org.nguh.nguhcraft.item.NguhItems

@Environment(EnvType.CLIENT)
object Farming {
    fun AddAll(Ctx: CreativeModeTab.ItemDisplayParameters, E: CreativeModeTab.Output) {
        for (B in NguhBlocks.CRATES) add(E, B)
        add(E, NguhItems.GRAPES)
        add(E, NguhItems.PEANUTS)
        add(E, NguhItems.GRAPE_SEEDS)
        add(E, NguhItems.GRAPE_LEAF)
    }

    fun add(e: CreativeModeTab.Output, b: Block) = add(e, b.asItem())

    fun add(e: CreativeModeTab.Output, i: Item) = e.accept(ItemStack(i))
}