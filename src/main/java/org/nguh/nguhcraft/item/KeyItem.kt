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
import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.BundleContentsComponent
import net.minecraft.component.type.TooltipDisplayComponent
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.StackReference
import net.minecraft.item.BundleItem
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.screen.slot.Slot
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.ClickType
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.block.LockedDoorBlockEntity
import org.nguh.nguhcraft.server.ServerUtils.UpdateLock
import java.util.function.Consumer

class KeyItem : Item(
    Settings()
    .fireproof()
    .rarity(Rarity.UNCOMMON)
    .registryKey(RegistryKey.of(RegistryKeys.ITEM, ID))
) {
    @Deprecated("Deprecated by Mojang")
    override fun appendTooltip(
        S: ItemStack,
        Ctx: TooltipContext,
        TDC: TooltipDisplayComponent,
        TC: Consumer<Text>,
        Ty: TooltipType
    ) { TC.accept(GetLockTooltip(S, Ty, KEY_PREFIX)) }

    override fun useOnBlock(Ctx: ItemUsageContext) = UseOnBlock(Ctx)

    companion object {
        @JvmField val ID = Id("key")
        @JvmField val COMPONENT_ID = ID

        @JvmField
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
                if ((Left as LockableBlockEntity).IsLocked()) return Left
                return Right
            }

            override fun getFrom(BE: ChestBlockEntity) = BE
            override fun getFallback() = null
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

        /** Get the UUID tooltip for a key or lock item. */
        fun GetLockTooltip(S: ItemStack, Ty: TooltipType, Prefix: Text): Text {
            val Key = S.get(COMPONENT) ?: return Prefix
            val Str = Text.literal(if (Ty.isAdvanced || Key.length < 13) Key else Key.substring(0..<13) + "...")
            return Text.empty().append(Prefix).append(Str.formatted(Formatting.LIGHT_PURPLE))
        }

        /** Check if a chest is locked. */
        @JvmStatic
        fun IsChestLocked(BE: BlockEntity): Boolean {
            val W = BE.world ?: return false
            val E = GetLockableEntity(W, BE.pos) ?: return false
            return E.IsLocked()
        }

        /** Run when a key is used on a block. */
        fun UseOnBlock(Ctx: ItemUsageContext): ActionResult {
            // If this is not a lockable block, do nothing.
            val W = Ctx.world
            val BE = GetLockableEntity(W, Ctx.blockPos) ?: return ActionResult.PASS

            // If the block is not locked, do nothing; if it is, and the
            // key doesn’t match, then we fail here.
            val Key = BE.`Nguhcraft$GetLock`()
            if (Key == null) return ActionResult.PASS
            if (!BE.CheckCanOpen(Ctx.player, Ctx.stack)) return ActionResult.FAIL

            // Key matches. Drop the lock and clear it.
            if (W is ServerWorld) {
                val Lock = LockItem.Create(Key)
                Block.dropStack(W, Ctx.blockPos, Lock)
                UpdateLock(BE, null)
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
    }
}

class MasterKeyItem : Item(
    Settings()
        .fireproof()
        .rarity(Rarity.EPIC)
        .registryKey(RegistryKey.of(RegistryKeys.ITEM, ID))
) {
    override fun useOnBlock(Ctx: ItemUsageContext) = KeyItem.UseOnBlock(Ctx)
    companion object {
        @JvmField val ID = Id("master_key")
    }
}

class KeyChainItem : BundleItem(
    Settings()
        .fireproof()
        .maxCount(1)
        .rarity(Rarity.UNCOMMON)
        .component(DataComponentTypes.BUNDLE_CONTENTS, BundleContentsComponent.DEFAULT)
        .registryKey(RegistryKey.of(RegistryKeys.ITEM, ID))
) {
    override fun onClicked(
        St: ItemStack,
        Other: ItemStack,
        Slot: Slot,
        Click: ClickType,
        PE: PlayerEntity,
        StackRef: StackReference
    ): Boolean {
        // A left click on the keyring is only valid if no item is selected
        // or if the selected item is a key.
        if (Click == ClickType.LEFT && !IsEmptyOrKey(Other))
            return false

        return super.onClicked(St, Other, Slot, Click, PE, StackRef)
    }

    override fun onStackClicked(
        St: ItemStack,
        Slot: Slot,
        Click: ClickType,
        PE: PlayerEntity
    ) = IsEmptyOrKey(Slot.stack) && super.onStackClicked(St, Slot, Click, PE)

    override fun usageTick(W: World, U: LivingEntity, St: ItemStack, Ticks: Int) {
        /** Do nothing so people can’t accidentally drop the contents of this. */
    }

    override fun useOnBlock(Ctx: ItemUsageContext) = KeyItem.UseOnBlock(Ctx)
    companion object {
        @JvmField val ID = Id("key_chain")
        @JvmStatic fun `is`(S: ItemStack) = S.isOf(NguhItems.KEY_CHAIN)

        /** Get the tooltip to render for the selected key. */
        @JvmStatic
        fun GetKeyTooltip(St: ItemStack) = KeyItem.GetLockTooltip(
            St,
            TooltipType.BASIC,
            Text.empty().append(St.formattedName).append(": ")
        )

        /** We don’t allow adding master keys to keychains because that’s kind of pointless. */
        private fun IsEmptyOrKey(St: ItemStack): Boolean = St.isEmpty || St.isOf(NguhItems.KEY)
    }
}