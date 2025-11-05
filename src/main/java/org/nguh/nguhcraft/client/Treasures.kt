package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.component.ComponentType
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.*
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments.*
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributeModifier
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Item
import net.minecraft.item.ItemGroup
import net.minecraft.item.ItemGroup.DisplayContext
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments.ARCANE
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments.HYPERSHOT
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments.SMELTING
import java.util.*

@Environment(EnvType.CLIENT)
object Treasures {
    fun AddAll(Ctx: ItemGroup.DisplayContext, Entries: ItemGroup.Entries) {
        val ESSENCE_FLASK = Potion(Ctx, "Ancient Drop of Cherry", 0xFFBFD6,
            StatusEffectInstance(StatusEffects.HEALTH_BOOST, 60 * 20, 24),
            StatusEffectInstance(StatusEffects.REGENERATION, 60 * 20, 5)
        ).lore("ancient_drop_of_cherry").build()

        val MOLTEN_PICKAXE = Builder(Ctx, Items.NETHERITE_PICKAXE, Name("Molten Pickaxe"))
            .unbreakable()
            .enchant(EFFICIENCY, 10)
            .enchant(FORTUNE, 5)
            .enchant(SMELTING)
            .build()

        val SCYTHE_OF_DOOM = Builder(Ctx, Items.NETHERITE_HOE, Name("Scythe of Doom"))
            .unbreakable()
            .enchant(EFFICIENCY, 10)
            .enchant(FORTUNE, 5)
            .enchant(FIRE_ASPECT, 4)
            .enchant(KNOCKBACK, 2)
            .enchant(LOOTING, 5)
            .enchant(SHARPNESS, 40)
            .build()

        val THOU_HAST_BEEN_YEETEN = Builder(Ctx, Items.MACE, Name("Thou Hast Been Yeeten"))
            .unbreakable()
            .enchant(ARCANE)
            .enchant(SHARPNESS, 255)
            .enchant(KNOCKBACK, 10)
            .enchant(CHANNELING, 2)
            .build()

        val THOU_HAS_BEEN_YEETEN_CROSSBOW = Builder(Ctx, Items.CROSSBOW, Name("Thou Hast Been Yeeten (Crossbow Version)"))
            .unbreakable()
            .enchant(HYPERSHOT, 100)
            .build()

        val TRIDENT_OF_THE_SEVEN_WINDS = Builder(Ctx, Items.TRIDENT, Name("Trident of the Seven Winds"))
            .unbreakable()
            .enchant(RIPTIDE, 10)
            .enchant(IMPALING, 10)
            .build()

        val WRATH_OF_ZEUS = Builder(Ctx, Items.TRIDENT, Name("Wrath of Zeus"))
            .unbreakable()
            .enchant(SHARPNESS, 50)
            .enchant(MULTISHOT, 100)
            .enchant(CHANNELING, 2)
            .enchant(LOYALTY, 3)
            .build()

        Entries.add(THOU_HAST_BEEN_YEETEN)
        Entries.add(THOU_HAS_BEEN_YEETEN_CROSSBOW)
        Entries.add(WRATH_OF_ZEUS)
        Entries.add(TRIDENT_OF_THE_SEVEN_WINDS)
        Entries.add(SCYTHE_OF_DOOM)
        Entries.add(MOLTEN_PICKAXE)
        Entries.add(ESSENCE_FLASK)
        Entries.add(ItemStack(Items.PETRIFIED_OAK_SLAB))
    }


    private fun Name(Name: String, Format: Formatting = Formatting.GOLD): Text = Text.literal(Name)
        .setStyle(Style.EMPTY.withItalic(false).withFormatting(Format))

    private fun Potion(
        Ctx: DisplayContext,
        Name: String,
        Colour: Int,
        vararg Effects: StatusEffectInstance
    ) = Builder(Ctx, Items.POTION, Name(Name))
        .set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent(
            Optional.empty(),
            Optional.of(Colour),
            listOf(*Effects),
            Optional.empty()
        ))


    private class Builder(private val Ctx: DisplayContext, I: Item, Name: Text) {
        private val S = ItemStack(I)
        private fun apply(F: (S: ItemStack) -> Unit) = also { F(S) }
        init { set(DataComponentTypes.CUSTOM_NAME, Name) }

        /** Build the item stack. */
        fun build() = S

        /** Enchant the item stack. */
        fun enchant(Enchantment: RegistryKey<Enchantment>, Level: Int = 1): Builder {
            val RL = Ctx.lookup.getOrThrow(RegistryKeys.ENCHANTMENT)
            val Entry = RL.getOrThrow(Enchantment)
            return apply { S.addEnchantment(Entry, Level) }
        }

        /** Add lore to the stack. */
        fun lore(Key: String): Builder {
            return set(DataComponentTypes.LORE, LoreComponent(
                listOf(Text.translatable("lore.nguhcraft.${Key}"))
            ))
        }

        /** Add an attribute modifier. */
        fun modifier(
            Attr: RegistryEntry<EntityAttribute>,
            Slot: AttributeModifierSlot,
            Mod: EntityAttributeModifier
        ) = set(
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            AttributeModifiersComponent.DEFAULT.with(
                Attr,
                Mod,
                Slot
            )
        )

        /** Set a component on this item stack. */
        fun <T> set(type: ComponentType<in T>, value: T? = null)
            = apply { it.set(type, value) }

        /** Make this item stack unbreakable. */
        fun unbreakable() = set(DataComponentTypes.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE)
    }
}