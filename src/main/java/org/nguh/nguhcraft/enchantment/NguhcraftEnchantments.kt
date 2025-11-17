package org.nguh.nguhcraft.enchantment

import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhcraftEnchantments {
    private fun Key(S: String): ResourceKey<Enchantment> = ResourceKey.create(Registries.ENCHANTMENT, Id(S))

    /**
     * The arcane enchantment causes an entity to deal ‘arcane’ damage, which
     * functions more or less like vanilla’s ‘magic’ damage, i.e. it bypasses
     * armour etc.
     */
    @JvmField val ARCANE = Key("arcane")

    /**
     * The health enchantment increases an entity’s maximum health.
     *
     * Per level, this adds 2 health points (= 1 heart) to the entity’s
     * maximum health. This works for any armour slot.
     */
    @JvmField val HEALTH = Key("health")

    /**
     * The homing enchantment causes projectiles to home in on a target.
     *
     * When a projectile is fired, the game will attempt to find a target
     * for it; if the player is looking directly at the entity, that entity
     * becomes the target; otherwise, any living entity within a certain
     * radius of the projectile’s path becomes the target. Hostile entities
     * are targeted in preference to neutral entities, and nearby entities
     * are targeted in preference to distant entities.
     *
     * Villagers and iron golems are never targeted, unless the player is
     * pointing directly at them. (Zombified) piglins are only targeted if
     * they are already hostile.
     */
    @JvmField val HOMING = Key("homing")

    /**
     * The hypershot enchantment fires additional projectiles.
     *
     * A living entity that fires a projectile from a weapon enchanted
     * with hypershot enters a state of hypershot, with a counter set
     * to the enchantment level. While in this state, the entity will
     * fire a copy of the projectile every tick, decrementing the counter
     * each time until it reaches zero. The weapon may rapidly lose
     * durability while in this state.
     *
     * If the entity switches weapons, dies, disconnects, is otherwise
     * removed from the world, or the server stops, the hypershot state
     * is cancelled. The weapon cannot be used again while in hypershot
     * state.
     *
     * If the weapon has homing, all projectiles fired by hypershot will
     * have homing; the target selection is done separately for each new
     * projectile.
     *
     * If the weapon has multishot, that many projectiles are fired each
     * tick; the counter is still only decremented once per tick.
     */
    @JvmField val HYPERSHOT = Key("hypershot")

    /**
     * The saturation enchantment reduces hunger.
     *
     * For the exact formula, see ServerUtils#HandleSaturationEnchantment.
     */
    @JvmField val SATURATION = Key("saturation")

    /**
     * The smelting enchantment causes any blocks mined to be smelted.
     *
     * The block item, not the item dropped by the block, is smelted. For
     * example, mining stone will yield smooth stone, not stone, otherwise
     * this would just be another silk touch.
     *
     * Silk touch is incompatible with smelting and ignored if present.
     */
    @JvmField val SMELTING = Key("smelting")
}