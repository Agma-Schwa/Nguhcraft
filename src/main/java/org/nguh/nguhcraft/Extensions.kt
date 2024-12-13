package org.nguh.nguhcraft

import net.minecraft.block.AbstractBlock
import net.minecraft.block.Block
import net.minecraft.block.BlockState
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.tag.TagKey

infix fun AbstractBlock.AbstractBlockState.isa(B: TagKey<Block>) = this.isIn(B)
infix fun AbstractBlock.AbstractBlockState.isa(B: RegistryEntry<Block>) = this.isOf(B)
infix fun BlockState.isa(B: Block) = this.isOf(B)