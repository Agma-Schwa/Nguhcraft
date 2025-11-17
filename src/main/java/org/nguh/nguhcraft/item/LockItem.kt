package org.nguh.nguhcraft.item

import com.mojang.serialization.Codec
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.LockCode
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.item.TooltipFlag
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionResult
import net.minecraft.ChatFormatting
import net.minecraft.world.item.Rarity
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.server.ServerUtils.UpdateLock
import java.util.function.Consumer

class LockItem : Item(
    Properties()
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
    ) { TC.accept(KeyItem.GetLockTooltip(S, Ty, LOCK_PREFIX)) }

    override fun useOn(Ctx: UseOnContext): InteractionResult {
        val W = Ctx.level
        val Pos = Ctx.clickedPos
        val BE = KeyItem.GetLockableEntity(W, Pos)
        if (BE != null) {
            // Already locked.
            if (BE.IsLocked()) return InteractionResult.FAIL

            // Check if the lock is paired.
            val Key = Ctx.itemInHand.get(KeyItem.COMPONENT) ?: return InteractionResult.FAIL

            // Apply the lock.
            if (!W.isClientSide) {
                UpdateLock(BE, Key)
                Ctx.itemInHand.shrink(1)
            }

            W.playSound(
                Ctx.player,
                Pos,
                SoundEvents.IRON_DOOR_CLOSE,
                SoundSource.BLOCKS,
                1.0f,
                1.0f
            )

            return InteractionResult.SUCCESS
        }
        return InteractionResult.PASS
    }

    companion object {
        private val LOCK_PREFIX = Component.literal("Id: ").withStyle(ChatFormatting.YELLOW)
        val ID = Id("lock")

        /** Create a lock item stack with the specified key. */
        fun Create(Key: String): ItemStack {
            val St = ItemStack(NguhItems.LOCK)
            St.set(KeyItem.COMPONENT, Key)
            St.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
            return St
        }
    }
}