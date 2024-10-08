package org.nguh.nguhcraft.item

import net.minecraft.block.Block
import net.minecraft.block.ChestBlock
import net.minecraft.block.DoorBlock
import net.minecraft.block.DoubleBlockProperties
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.block.enums.DoubleBlockHalf
import net.minecraft.component.DataComponentTypes
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.block.LockableBlockEntity
import org.nguh.nguhcraft.block.LockedDoorBlockEntity
import org.nguh.nguhcraft.server.ServerUtils.UpdateLock

class KeyItem : Item(
    Settings()
    .fireproof()
    .rarity(Rarity.UNCOMMON)
    .component(DataComponentTypes.LOCK, ContainerLock.EMPTY)
) {
    override fun appendTooltip(
        S: ItemStack,
        Ctx: TooltipContext,
        TT: MutableList<Text>,
        Ty: TooltipType
    ) = AppendLockTooltip(S, TT, Ty, KEY_PREFIX)

    override fun useOnBlock(Ctx: ItemUsageContext): ActionResult {
        // If this is not a lockable block, do nothing.
        val W = Ctx.world
        val BE = GetLockableEntity(W, Ctx.blockPos) ?: return ActionResult.PASS

        // If the block is not locked, do nothing.
        if (BE.lock.key.isEmpty()) return ActionResult.PASS

        // If it is, and the key doesn’t match, then we fail here.
        val Key = Ctx.stack.get(DataComponentTypes.LOCK)?.key
        if (Key != BE.lock.key) return ActionResult.FAIL

        // Key matches. Drop the lock and clear it.
        if (W is ServerWorld) {
            val Lock = LockItem.Create(BE.lock)
            Block.dropStack(W, Ctx.blockPos, Lock)
            UpdateLock(BE, ContainerLock.EMPTY)
        }

        W.playSound(
            Ctx.player,
            Ctx.blockPos,
            SoundEvents.BLOCK_CHAIN_BREAK,
            SoundCategory.BLOCKS,
            1.0f,
            1.0f
        )

        return ActionResult.success(W.isClient)
    }

    companion object {
        private val KEY_PREFIX = Text.literal("Id: ").formatted(Formatting.YELLOW)

        private object Accessor : DoubleBlockProperties.PropertyRetriever<ChestBlockEntity, ChestBlockEntity?> {
            override fun getFromBoth(
                Left: ChestBlockEntity,
                Right: ChestBlockEntity
            ): ChestBlockEntity {
                if ((Left as LockableBlockEntity).lock.key.isNotEmpty()) return Left
                return Right
            }

            override fun getFrom(BE: ChestBlockEntity) = BE
            override fun getFallback() = null
        }

        fun AppendLockTooltip(S: ItemStack, TT: MutableList<Text>, Ty: TooltipType, Prefix: Text) {
            val Lock = S.get(DataComponentTypes.LOCK) ?: return
            if (Lock.key.isEmpty()) return
            val Key = Text.literal(if (Ty.isAdvanced) Lock.key else Lock.key.substring(0..<13) + "...")
            TT.add(Prefix.copy().append(Key.formatted(Formatting.LIGHT_PURPLE)))
        }

        @JvmStatic
        fun CanOpen(S: ItemStack, Lock: String): Boolean {
            if (Lock.isEmpty()) return true
            if (!S.isOf(NguhItems.KEY)) return false
            val Key = S.get(DataComponentTypes.LOCK)?.key ?: return false
            return Key == Lock
        }

        /**
        * Get the actual block entity to use for locking.
        *
        * Normally, that is just the block entity at that location, if any; however,
        * if the chest in question is a double chest, then for some ungodly reason,
        * there will be TWO block entities for the same chest, and we only want to
        * lock one of them. This handles getting whichever one is already locked, in
        * that case.
        *
        * For lockable doors, get the lower half instead.
        */
        fun GetLockableEntity(W: World, Pos: BlockPos): LockableBlockEntity? {
            val BE = W.getBlockEntity(Pos)

            // Handle (double) chests.
            if (BE is ChestBlockEntity) {
                val St = W.getBlockState(Pos)
                val BES = (St.block as ChestBlock).getBlockEntitySource(St, W, Pos, true)

                // This stupid cast is necessary because Kotlin is too dumb to
                // interface with the corresponding Java method properly.
                val Cast =  BES as DoubleBlockProperties.PropertySource<ChestBlockEntity>
                return Cast.apply(Accessor) as LockableBlockEntity
            }

            // Handle doors.
            if (BE is LockedDoorBlockEntity) {
                val St = W.getBlockState(Pos)
                if (St.get(DoorBlock.HALF) == DoubleBlockHalf.UPPER) return GetLockableEntity(W, Pos.down())
                return BE
            }

            // All other containers are not double blocks.
            if (BE is LockableContainerBlockEntity) return BE as LockableBlockEntity
            return null
        }

        /** Check if a chest is locked. */
        @JvmStatic
        fun IsChestLocked(BE: BlockEntity): Boolean {
            val W = BE.world ?: return false
            val E = GetLockableEntity(W, BE.pos) ?: return false
            return E.lock.key.isNotEmpty()
        }
    }
}