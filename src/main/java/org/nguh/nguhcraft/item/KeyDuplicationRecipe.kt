package org.nguh.nguhcraft.item

import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeSerializer
import net.minecraft.world.item.crafting.CustomRecipe
import net.minecraft.world.item.crafting.CraftingBookCategory
import net.minecraft.world.item.crafting.CraftingInput
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.world.level.Level

class KeyDuplicationRecipe(C: CraftingBookCategory) : CustomRecipe(C) {
    /** Get the paired key and check that there is an unpaired one. */
    private fun GetPairedKey(Input: CraftingInput): ItemStack? {
        var Paired: ItemStack? = null
        var Unpaired: ItemStack? = null

        for (Slot in 0..<Input.size()) {
            val St = Input.getItem(Slot)
            if (St.isEmpty) continue
            if (!St.`is`(NguhItems.KEY)) return null
            if (!St.has(KeyItem.COMPONENT)) {
                if (Unpaired != null) return null
                Unpaired = St
            } else {
                if (Paired != null) return null
                Paired = St
            }
        }

        if (Paired == null || Unpaired == null) return null
        return Paired
    }

    override fun matches(Input: CraftingInput, W: Level): Boolean {
        // Need exactly one stack of paired keys and one stack of unpaired keys.
        return GetPairedKey(Input) != null
    }

    override fun assemble(Input: CraftingInput, L: HolderLookup.Provider): ItemStack {
        val Paired = GetPairedKey(Input) ?: return ItemStack.EMPTY
        return Paired.copyWithCount(1)
    }

    override fun getRemainingItems(Input: CraftingInput): NonNullList<ItemStack> {
        val L = NonNullList.withSize(Input.size(), ItemStack.EMPTY)
        for (Slot in 0..<Input.size()) {
            val St = Input.getItem(Slot)
            if (St.`is`(NguhItems.KEY) && St.has(KeyItem.COMPONENT)) {
                L[Slot] = St.copyWithCount(1)
                break
            }
        }
        return L
    }

    override fun getSerializer() = SERIALISER
    companion object { lateinit var SERIALISER: RecipeSerializer<KeyDuplicationRecipe> }
}