package org.nguh.nguhcraft.item

import com.mojang.serialization.Codec
import net.minecraft.block.Block
import net.minecraft.block.ChestBlock
import net.minecraft.block.DoorBlock
import net.minecraft.block.DoubleBlockProperties
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.block.entity.LockableContainerBlockEntity
import net.minecraft.block.enums.DoubleBlockHalf
import net.minecraft.component.Component
import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.block.LockableBlockEntity
import org.nguh.nguhcraft.block.LockedDoorBlockEntity
import org.nguh.nguhcraft.mixin.common.ComponentPredicateAccessor
import org.nguh.nguhcraft.server.ServerUtils.UpdateLock

/**
 * Get the key associated with a container lock.
 *
 * If the lock somehow has multiple lock components, only the
 * first one is returned.
 */
fun ContainerLock.GetKey(): String? {
    val Comps = this.predicate.components()
    if (Comps.isEmpty) return null
    return ((Comps as ComponentPredicateAccessor).components[0] as Component<*>).value as String
}

class KeyItem : Item(
    Settings()
    .fireproof()
    .rarity(Rarity.UNCOMMON)
    .registryKey(RegistryKey.of(RegistryKeys.ITEM, ID))
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

        // If the block is not locked, do nothing; if it is, and the
        // key doesn’t match, then we fail here.
        if (BE.lock == ContainerLock.EMPTY) return ActionResult.PASS
        if (!BE.lock.canOpen(Ctx.stack)) return ActionResult.FAIL

        // Key matches. Drop the lock and clear it.
        if (W is ServerWorld) {
            // This could theoretically fail if someone creates a container
            // lock that uses some other random item predicate, so only drop
            // a lock if we can extract a stored key.
            val Key = BE.lock.GetKey()
            if (Key != null) {
                val Lock = LockItem.Create(Key)
                Block.dropStack(W, Ctx.blockPos, Lock)
            }

            // Remove the component from the block entity either way.
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

        return ActionResult.SUCCESS
    }

    companion object {
        @JvmField val ID = Id("key")
        @JvmField val COMPONENT_ID = ID

        val COMPONENT: ComponentType<String> = Registry.register(
            Registries.DATA_COMPONENT_TYPE,
            COMPONENT_ID,
            ComponentType.builder<String>().codec(Codec.STRING).build()
        )

        private val KEY_PREFIX = Text.literal("Id: ").formatted(Formatting.YELLOW)

        private object Accessor : DoubleBlockProperties.PropertyRetriever<ChestBlockEntity, ChestBlockEntity?> {
            override fun getFromBoth(
                Left: ChestBlockEntity,
                Right: ChestBlockEntity
            ): ChestBlockEntity {
                if ((Left as LockableBlockEntity).lock != ContainerLock.EMPTY) return Left
                return Right
            }

            override fun getFrom(BE: ChestBlockEntity) = BE
            override fun getFallback() = null
        }

        fun AppendLockTooltip(S: ItemStack, TT: MutableList<Text>, Ty: TooltipType, Prefix: Text) {
            val Key = S.get(COMPONENT) ?: return
            val Str = Text.literal(if (Ty.isAdvanced || Key.length < 13) Key else Key.substring(0..<13) + "...")
            TT.add(Prefix.copy().append(Str.formatted(Formatting.LIGHT_PURPLE)))
        }

        /** Create an instance with the specified key. */
        fun Create(Key: String): ItemStack {
            val St = ItemStack(NguhItems.KEY)
            St.set(COMPONENT, Key)
            St.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            return St
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
        @JvmStatic
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
            return E.lock != ContainerLock.EMPTY
        }
    }
}