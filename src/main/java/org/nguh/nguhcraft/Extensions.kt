package org.nguh.nguhcraft

import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.core.Holder
import net.minecraft.tags.TagKey

infix fun BlockBehaviour.BlockStateBase.isa(B: TagKey<Block>) = this.`is`(B)
infix fun BlockBehaviour.BlockStateBase.isa(B: Holder<Block>) = this.`is`(B)
infix fun BlockState.isa(B: Block) = this.`is`(B)
infix fun ItemStack.isa(I: Item) = this.`is`(I)