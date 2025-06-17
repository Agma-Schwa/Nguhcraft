package org.nguh.nguhcraft.item

import com.mojang.serialization.Codec
import net.minecraft.component.DataComponentTypes
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.tooltip.TooltipType
import net.minecraft.predicate.ComponentPredicate
import net.minecraft.predicate.item.ItemPredicate
import net.minecraft.predicate.item.ItemSubPredicate
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.ActionResult
import net.minecraft.util.Formatting
import net.minecraft.util.Rarity
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.mixin.common.ComponentPredicateAccessor
import org.nguh.nguhcraft.server.ServerUtils.UpdateLock
import kotlin.jvm.optionals.getOrNull

class LockPredicate(val Key: String) : ItemSubPredicate {
    override fun test(St: ItemStack): Boolean {
        if (St.isOf(NguhItems.MASTER_KEY)) return true
        if (St.isOf(NguhItems.KEY_CHAIN)) return CheckKeyChain(St)
        return CheckKey(St)
    }

    private fun CheckKey(St: ItemStack): Boolean {
        if (!St.isOf(NguhItems.KEY)) return false
        return St.get(KeyItem.COMPONENT) == Key
    }

    private fun CheckKeyChain(St: ItemStack) = St.get(DataComponentTypes.BUNDLE_CONTENTS)
        ?.iterate()
        ?.any(::CheckKey) == true

    companion object {
        val ID = Id("lock_predicate")
        val CODEC: Codec<LockPredicate> = Codec.STRING.xmap(::LockPredicate, LockPredicate::Key)
        val TYPE: ItemSubPredicate.Type<LockPredicate> = Registry.register(
            Registries.ITEM_SUB_PREDICATE_TYPE,
            ID,
            ItemSubPredicate.Type(CODEC)
        )

        fun RunStaticInitialisation() {}
    }
}

class LockItem : Item(
    Settings()
    .rarity(Rarity.UNCOMMON)
    .registryKey(RegistryKey.of(RegistryKeys.ITEM, ID))
) {
    override fun appendTooltip(
        S: ItemStack,
        Ctx: TooltipContext,
        TT: MutableList<Text>,
        Ty: TooltipType
    ) { TT.add(KeyItem.GetLockTooltip(S, Ty, LOCK_PREFIX)) }

    override fun useOnBlock(Ctx: ItemUsageContext): ActionResult {
        val W = Ctx.world
        val Pos = Ctx.blockPos
        val BE = KeyItem.GetLockableEntity(W, Pos)
        if (BE != null) {
            // Already locked.
            if (BE.lock != ContainerLock.EMPTY) return ActionResult.FAIL

            // Check if the lock is paired.
            val Key = Ctx.stack.getOrDefault(KeyItem.COMPONENT, null)
            if (Key == null) return ActionResult.FAIL

            // Apply the lock.
            if (!W.isClient) {
                UpdateLock(BE, CreateContainerLock(Key))
                Ctx.stack.decrement(1)
            }

            W.playSound(
                Ctx.player,
                Pos,
                SoundEvents.BLOCK_IRON_DOOR_CLOSE,
                SoundCategory.BLOCKS,
                1.0f,
                1.0f
            )

            return ActionResult.SUCCESS
        }
        return ActionResult.PASS
    }

    companion object {
        private val LOCK_PREFIX = Text.literal("Id: ").formatted(Formatting.YELLOW)
        val ID = Id("lock")

        /** Create a lock item stack with the specified key. */
        fun Create(Key: String): ItemStack {
            val St = ItemStack(NguhItems.LOCK)
            St.set(KeyItem.COMPONENT, Key)
            St.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true)
            return St
        }

        /** Create a container lock from a key. */
        fun CreateContainerLock(Key: String): ContainerLock {
            val Predicate = ItemPredicate.Builder.create()
                .subPredicate(LockPredicate.TYPE, LockPredicate(Key))
                .build()

            return ContainerLock(Predicate)
        }

        /** Retrieve the key from a legacy container lock item predicate. */
        private fun ExtractKeyFromLegacyContainerLock(Pred: ItemPredicate): String? {
            // Most locks will be using the new predicate, so check for that first.
            if (Pred.subPredicates[LockPredicate.TYPE] != null) return null

            // The legacy lock was created like this
            //
            //   ItemPredicate.Builder.create()
            //       .items(Registries.ITEM, NguhItems.KEY)
            //       .component(ComponentPredicate.builder().add(KeyItem.COMPONENT, Key).build())
            //       .build()
            //
            // so match that.
            if (Utils.GetSingleElement(Pred.items.getOrNull())?.value() != NguhItems.KEY) return null
            return Utils.GetSingleElement((Pred.components as ComponentPredicateAccessor).components)
                ?.takeIf { it.type == KeyItem.COMPONENT }
                ?.value as? String
        }

        /** Format the message that indicates why a container is locked. */
        @JvmStatic
        fun FormatLockedMessage(Lock: ContainerLock, BlockName: Text): MutableText = Text.translatable(
            "nguhcraft.block.locked",
            BlockName,
            Text.literal(Lock.GetKey() ?: "<error>").formatted(Formatting.LIGHT_PURPLE)
        )

        /** Upgrade an old-style container lock to our new predicate. */
        @JvmStatic
        fun UpgradeContainerLock(Lock: ContainerLock): ContainerLock =
            ExtractKeyFromLegacyContainerLock(Lock.predicate)?.let(::CreateContainerLock) ?: Lock
    }
}