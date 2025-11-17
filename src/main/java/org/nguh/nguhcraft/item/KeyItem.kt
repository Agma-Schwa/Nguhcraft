package org.nguh.nguhcraft.item

import com.mojang.serialization.Codec
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.ChestBlock
import net.minecraft.world.level.block.DoorBlock
import net.minecraft.world.level.block.DoubleBlockCombiner
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.ChestBlockEntity
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.BundleContents
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.SlotAccess
import net.minecraft.world.item.BundleItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.world.inventory.Slot
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.world.inventory.ClickAction
import net.minecraft.ChatFormatting
import net.minecraft.world.item.Rarity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.block.LockedDoorBlockEntity
import org.nguh.nguhcraft.server.ServerUtils.UpdateLock
import java.util.function.Consumer

class KeyItem : Item(
    Properties()
    .fireResistant()
    .rarity(Rarity.UNCOMMON)
    .setId(ResourceKey.create(Registries.ITEM, ID))
) {
    @Deprecated("Deprecated by Mojang")
    override fun appendHoverText(
        S: ItemStack,
        Ctx: TooltipContext,
        TDC: TooltipDisplay,
        TC: Consumer<Component>,
        Ty: TooltipFlag
    ) { TC.accept(GetLockTooltip(S, Ty, KEY_PREFIX)) }

    override fun useOn(Ctx: UseOnContext) = UseOnBlock(Ctx)

    companion object {
        @JvmField val ID = Id("key")
        @JvmField val COMPONENT_ID = ID

        @JvmField
        val COMPONENT: DataComponentType<String> = Registry.register(
            BuiltInRegistries.DATA_COMPONENT_TYPE,
            COMPONENT_ID,
            DataComponentType.builder<String>().persistent(Codec.STRING).build()
        )

        private val KEY_PREFIX = Component.literal("Id: ").withStyle(ChatFormatting.YELLOW)

        private object Accessor : DoubleBlockCombiner.Combiner<ChestBlockEntity, ChestBlockEntity?> {
            override fun acceptDouble(
                Left: ChestBlockEntity,
                Right: ChestBlockEntity
            ): ChestBlockEntity {
                if ((Left as LockableBlockEntity).IsLocked()) return Left
                return Right
            }

            override fun acceptSingle(BE: ChestBlockEntity) = BE
            override fun acceptNone() = null
        }

        /** Create an instance with the specified key. */
        fun Create(Key: String): ItemStack {
            val St = ItemStack(NguhItems.KEY)
            St.set(COMPONENT, Key)
            St.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
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
        fun GetLockableEntity(W: Level, Pos: BlockPos): LockableBlockEntity? {
            val BE = W.getBlockEntity(Pos)

            // Handle (double) chests.
            if (BE is ChestBlockEntity) {
                val St = W.getBlockState(Pos)
                val BES = (St.block as ChestBlock).combine(St, W, Pos, true)

                // This stupid cast is necessary because Kotlin is too dumb to
                // interface with the corresponding Java method properly.
                val Cast =  BES as DoubleBlockCombiner.NeighborCombineResult<ChestBlockEntity>
                return Cast.apply(Accessor) as LockableBlockEntity
            }

            // Handle doors.
            if (BE is LockedDoorBlockEntity) {
                val St = W.getBlockState(Pos)
                if (St.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) return GetLockableEntity(W, Pos.below())
                return BE
            }

            // All other containers are not double blocks.
            if (BE is BaseContainerBlockEntity) return BE as LockableBlockEntity
            return null
        }

        /** Get the UUID tooltip for a key or lock item. */
        fun GetLockTooltip(S: ItemStack, Ty: TooltipFlag, Prefix: Component): Component {
            val Key = S.get(COMPONENT) ?: return Prefix
            val Str = Component.literal(if (Ty.isAdvanced || Key.length < 13) Key else Key.substring(0..<13) + "...")
            return Component.empty().append(Prefix).append(Str.withStyle(ChatFormatting.LIGHT_PURPLE))
        }

        /** Check if a chest is locked. */
        @JvmStatic
        fun IsChestLocked(BE: BlockEntity): Boolean {
            val W = BE.level ?: return false
            val E = GetLockableEntity(W, BE.blockPos) ?: return false
            return E.IsLocked()
        }

        /** Run when a key is used on a block. */
        fun UseOnBlock(Ctx: UseOnContext): InteractionResult {
            // If this is not a lockable block, do nothing.
            val W = Ctx.level
            val BE = GetLockableEntity(W, Ctx.clickedPos) ?: return InteractionResult.PASS

            // If the block is not locked, do nothing; if it is, and the
            // key doesn’t match, then we fail here.
            val Key = BE.`Nguhcraft$GetLock`() ?: return InteractionResult.PASS
            if (!BE.CheckCanOpen(Ctx.player, Ctx.itemInHand)) return InteractionResult.FAIL

            // Key matches. Drop the lock and clear it.
            if (W is ServerLevel) {
                val Lock = LockItem.Create(Key)
                Block.popResource(W, Ctx.clickedPos, Lock)
                UpdateLock(BE, null)
            }

            W.playSound(
                Ctx.player,
                Ctx.clickedPos,
                SoundEvents.CHAIN_BREAK,
                SoundSource.BLOCKS,
                1.0f,
                1.0f
            )

            return InteractionResult.SUCCESS
        }
    }
}

class MasterKeyItem : Item(
    Properties()
        .fireResistant()
        .rarity(Rarity.EPIC)
        .setId(ResourceKey.create(Registries.ITEM, ID))
) {
    override fun useOn(Ctx: UseOnContext) = KeyItem.UseOnBlock(Ctx)
    companion object {
        @JvmField val ID = Id("master_key")
    }
}

class KeyChainItem : BundleItem(
    Properties()
        .fireResistant()
        .stacksTo(1)
        .rarity(Rarity.UNCOMMON)
        .component(DataComponents.BUNDLE_CONTENTS, BundleContents.EMPTY)
        .setId(ResourceKey.create(Registries.ITEM, ID))
) {
    override fun overrideOtherStackedOnMe(
        St: ItemStack,
        Other: ItemStack,
        Slot: Slot,
        Click: ClickAction,
        PE: Player,
        StackRef: SlotAccess
    ): Boolean {
        // A left click on the keyring is only valid if no item is selected
        // or if the selected item is a key.
        if (Click == ClickAction.PRIMARY && !IsEmptyOrKey(Other))
            return false

        return super.overrideOtherStackedOnMe(St, Other, Slot, Click, PE, StackRef)
    }

    override fun overrideStackedOnOther(
        St: ItemStack,
        Slot: Slot,
        Click: ClickAction,
        PE: Player
    ) = IsEmptyOrKey(Slot.item) && super.overrideStackedOnOther(St, Slot, Click, PE)

    override fun onUseTick(W: Level, U: LivingEntity, St: ItemStack, Ticks: Int) {
        /** Do nothing so people can’t accidentally drop the contents of this. */
    }

    override fun useOn(Ctx: UseOnContext) = KeyItem.UseOnBlock(Ctx)
    companion object {
        @JvmField val ID = Id("key_chain")
        @JvmStatic fun `is`(S: ItemStack) = S.`is`(NguhItems.KEY_CHAIN)

        /** Get the tooltip to render for the selected key. */
        @JvmStatic
        fun GetKeyTooltip(St: ItemStack) = KeyItem.GetLockTooltip(
            St,
            TooltipFlag.NORMAL,
            Component.empty().append(St.styledHoverName).append(": ")
        )

        /** We don’t allow adding master keys to keychains because that’s kind of pointless. */
        private fun IsEmptyOrKey(St: ItemStack): Boolean = St.isEmpty || St.`is`(NguhItems.KEY)
    }
}