package org.nguh.nguhcraft.client

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.ChatFormatting
import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceKey
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.entity.EquipmentSlotGroup
import net.minecraft.world.entity.ai.attributes.Attribute
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.item.*
import net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters
import net.minecraft.world.item.alchemy.PotionContents
import net.minecraft.world.item.component.ItemAttributeModifiers
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments.*
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments.ARCANE
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments.HYPERSHOT
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments.SMELTING
import java.util.*

@Environment(EnvType.CLIENT)
object Treasures {
    fun AddAll(Ctx: ItemDisplayParameters, Entries: CreativeModeTab.Output) {
        val ESSENCE_FLASK = Potion(Ctx, "ancient_drop_of_cherry", 0xFFBFD6,
            MobEffectInstance(MobEffects.HEALTH_BOOST, 60 * 20, 24),
            MobEffectInstance(MobEffects.REGENERATION, 60 * 20, 5)
        ).lore("ancient_drop_of_cherry").set(DataComponents.RARITY, value = Rarity.EPIC).build()

        val MOLTEN_PICKAXE = Builder(Ctx, Items.NETHERITE_PICKAXE, "molten_pickaxe")
            .unbreakable()
            .enchant(EFFICIENCY, 10)
            .enchant(FORTUNE, 5)
            .enchant(SMELTING)
            .build()

        val SCYTHE_OF_DOOM = Builder(Ctx, Items.NETHERITE_HOE, "scythe_of_doom")
            .unbreakable()
            .enchant(EFFICIENCY, 10)
            .enchant(FORTUNE, 5)
            .enchant(FIRE_ASPECT, 4)
            .enchant(KNOCKBACK, 2)
            .enchant(LOOTING, 5)
            .enchant(SHARPNESS, 40)
            .build()

        val THOU_HAST_BEEN_YEETEN = Builder(Ctx, Items.MACE, "thou_hast_been_yeeten")
            .unbreakable()
            .enchant(ARCANE)
            .enchant(SHARPNESS, 255)
            .enchant(KNOCKBACK, 10)
            .enchant(CHANNELING, 2)
            .build()

        val THOU_HAS_BEEN_YEETEN_CROSSBOW = Builder(Ctx, Items.CROSSBOW, "thou_hast_been_yeeten_crossbow_version")
            .unbreakable()
            .enchant(HYPERSHOT, 100)
            .build()

        val TRIDENT_OF_THE_SEVEN_WINDS = Builder(Ctx, Items.TRIDENT, "trident_of_the_seven_winds")
            .unbreakable()
            .enchant(RIPTIDE, 10)
            .enchant(IMPALING, 10)
            .build()

        val WRATH_OF_ZEUS = Builder(Ctx, Items.TRIDENT, "wrath_of_zeus")
            .unbreakable()
            .enchant(SHARPNESS, 50)
            .enchant(MULTISHOT, 100)
            .enchant(CHANNELING, 2)
            .enchant(LOYALTY, 3)
            .build()

        Entries.accept(THOU_HAST_BEEN_YEETEN)
        Entries.accept(THOU_HAS_BEEN_YEETEN_CROSSBOW)
        Entries.accept(WRATH_OF_ZEUS)
        Entries.accept(TRIDENT_OF_THE_SEVEN_WINDS)
        Entries.accept(SCYTHE_OF_DOOM)
        Entries.accept(MOLTEN_PICKAXE)
        Entries.accept(ESSENCE_FLASK)
        Entries.accept(ItemStack(Items.PETRIFIED_OAK_SLAB))
    }


    private fun Name(Name: String, Format: ChatFormatting = ChatFormatting.GOLD): Component = Component.literal(Name)
        .setStyle(Style.EMPTY.withItalic(false).applyFormat(Format))

    private fun Potion(
        Ctx: ItemDisplayParameters,
        Key: String,
        Colour: Int,
        vararg Effects: MobEffectInstance
    ) = Builder(Ctx, Items.POTION, Key)
        .set(DataComponents.POTION_CONTENTS, PotionContents(
            Optional.empty(),
            Optional.of(Colour),
            listOf(*Effects),
            Optional.empty()
        ))


    private class Builder(private val Ctx: ItemDisplayParameters, I: Item, Key: String) {
        private val S = ItemStack(I)
        private fun apply(F: (S: ItemStack) -> Unit) = also { F(S) }
        init { set(DataComponents.CUSTOM_NAME, Component.translatable("item.nguhcraft.$Key")) }

        /** Build the item stack. */
        fun build() = S

        /** Enchant the item stack. */
        fun enchant(Enchantment: ResourceKey<Enchantment>, Level: Int = 1): Builder {
            val RL = Ctx.holders.lookupOrThrow(Registries.ENCHANTMENT)
            val Entry = RL.getOrThrow(Enchantment)
            return apply { S.enchant(Entry, Level) }
        }

        /** Add lore to the stack. */
        fun lore(Key: String): Builder {
            return set(DataComponents.LORE, ItemLore(
                listOf(Component.translatable("lore.nguhcraft.${Key}"))
            ))
        }

        /** Add an attribute modifier. */
        fun modifier(
            Attr: Holder<Attribute>,
            Slot: EquipmentSlotGroup,
            Mod: AttributeModifier
        ) = set(
            DataComponents.ATTRIBUTE_MODIFIERS,
            ItemAttributeModifiers.EMPTY.withModifierAdded(
                Attr,
                Mod,
                Slot
            )
        )

        /** Set a component on this item stack. */
        fun <T> set(type: DataComponentType<in T>, value: T? = null)
            = apply { it.set(type, value) }

        /** Make this item stack unbreakable. */
        fun unbreakable() = set(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE)
    }
}