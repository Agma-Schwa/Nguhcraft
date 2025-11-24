package org.nguh.nguhcraft.block

import com.mojang.serialization.MapCodec
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.PotatoBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.nguh.nguhcraft.item.NguhItems
import java.util.function.Function

class PeanutCropBlock(settings: Properties) : CropBlock(settings) {
    override fun codec(): MapCodec<PotatoBlock?> {
        return CODEC
    }

    override fun getBaseSeedId(): ItemLike {
        return NguhItems.PEANUTS
    }

    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return SHAPES_BY_AGE[this.getAge(state)]
    }

    companion object {
        val CODEC: MapCodec<PotatoBlock?> =
            simpleCodec<PotatoBlock?>(Function { properties: Properties? ->
                PotatoBlock(properties)
            })
        private val SHAPE_HEIGHTS: IntArray = intArrayOf(2, 4, 5, 9, 11, 14, 14, 14)
        private val SHAPES_BY_AGE: Array<VoxelShape> =
            boxes(7, { age: Int -> column(16.0, 0.0, SHAPE_HEIGHTS[age].toDouble()) })
    }
}