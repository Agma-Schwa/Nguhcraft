package org.nguh.nguhcraft.item

import com.mojang.serialization.MapCodec
import net.minecraft.core.component.DataComponents
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.world.LockCode
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.core.NonNullList
import net.minecraft.world.level.Level
import java.util.*

class KeyLockPairingRecipe : CustomRecipe() {
    private fun GetKeyAndLocks(Input: CraftingInput): Pair<ItemStack?, Int> {
        var Key: ItemStack? = null
        var Locks = 0

        for (Slot in 0..<Input.size()) {
            val St = Input.getItem(Slot)
            if (St.isEmpty) continue
            when (St.item) {
                NguhItems.KEY -> {
                    if (Key != null) return null to 0
                    Key = St
                }
                NguhItems.LOCK -> Locks++
                else -> return null to 0
            }
        }

        return Key to Locks
    }

    private fun GetOrCreateContainerLock(Key: ItemStack): String {
        if (!Key.has(KeyItem.COMPONENT))
            Key.set(KeyItem.COMPONENT, UUID.randomUUID().toString())
        return Key.get(KeyItem.COMPONENT)!!
    }

    override fun assemble(Input: CraftingInput): ItemStack {
        // Get the key and lock count and do a sanity check.
        val (Key, Locks) = GetKeyAndLocks(Input)
        if (Key == null || Locks == 0) return ItemStack.EMPTY

        // Pair them.
        val LockComponent = GetOrCreateContainerLock(Key)
        val Lock = ItemStack(NguhItems.LOCK, Locks)
        Lock.set(KeyItem.COMPONENT, LockComponent)

        // Make both glow so we know that they’re paired.
        Key.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
        Lock.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
        return Lock
    }

    override fun matches(Input: CraftingInput, W: Level): Boolean {
        // Need exactly one key and as many locks as you want; the locks,
        // if already paired, will simply be overwritten.
        val (Key, Locks) = GetKeyAndLocks(Input)
        return Key != null && Locks > 0
    }

    override fun getRemainingItems(Input: CraftingInput): NonNullList<ItemStack> {
        val L = NonNullList.withSize(Input.size(), ItemStack.EMPTY)
        for (Slot in 0..<Input.size()) {
            val St = Input.getItem(Slot)
            if (St.`is`(NguhItems.KEY)) {
                // Copy with a count of 1 because this is the remainder after
                // applying the recipe *once*, which only ever consumes one
                // item in each slot.
                L[Slot] = St.copyWithCount(1)
                break
            }
        }
        return L
    }

    override fun getSerializer() = SERIALISER

    companion object {
        val INSTANCE = KeyLockPairingRecipe()
        val MAP_CODEC: MapCodec<KeyLockPairingRecipe> = MapCodec.unit(INSTANCE)
        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, KeyLockPairingRecipe> = StreamCodec.unit(INSTANCE)
        val SERIALISER: RecipeSerializer<KeyLockPairingRecipe> = RecipeSerializer(MAP_CODEC, STREAM_CODEC)
    }
}