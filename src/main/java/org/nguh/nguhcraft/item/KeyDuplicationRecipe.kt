package org.nguh.nguhcraft.item

import net.minecraft.item.ItemStack
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.SpecialCraftingRecipe
import net.minecraft.recipe.book.CraftingRecipeCategory
import net.minecraft.recipe.input.CraftingRecipeInput
import net.minecraft.registry.RegistryWrapper
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World

class KeyDuplicationRecipe(C: CraftingRecipeCategory) : SpecialCraftingRecipe(C) {
    /** Get the paired key and check that there is an unpaired one. */
    private fun GetPairedKey(Input: CraftingRecipeInput): ItemStack? {
        var Paired: ItemStack? = null
        var Unpaired: ItemStack? = null

        for (Slot in 0..<Input.size()) {
            val St = Input.getStackInSlot(Slot)
            if (St.isEmpty) continue
            if (!St.isOf(NguhItems.KEY)) return null
            if (!St.contains(KeyItem.COMPONENT)) {
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

    override fun matches(Input: CraftingRecipeInput, W: World): Boolean {
        // Need exactly one stack of paired keys and one stack of unpaired keys.
        return GetPairedKey(Input) != null
    }

    override fun craft(Input: CraftingRecipeInput, L: RegistryWrapper.WrapperLookup): ItemStack {
        val Paired = GetPairedKey(Input) ?: return ItemStack.EMPTY
        return Paired.copyWithCount(1)
    }

    override fun getRecipeRemainders(Input: CraftingRecipeInput): DefaultedList<ItemStack> {
        val L = DefaultedList.ofSize(Input.size(), ItemStack.EMPTY)
        for (Slot in 0..<Input.size()) {
            val St = Input.getStackInSlot(Slot)
            if (St.isOf(NguhItems.KEY) && St.contains(KeyItem.COMPONENT)) {
                L[Slot] = St.copyWithCount(1)
                break
            }
        }
        return L
    }

    override fun getSerializer() = SERIALISER
    companion object { lateinit var SERIALISER: RecipeSerializer<KeyDuplicationRecipe> }
}