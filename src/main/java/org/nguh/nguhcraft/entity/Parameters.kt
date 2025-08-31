package org.nguh.nguhcraft.entity

import com.mojang.serialization.Codec
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.PotionContentsComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.attribute.EntityAttribute
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.CreeperEntity
import net.minecraft.entity.mob.MobEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.registry.DynamicRegistryManager
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.world.explosion.ExplosionBehavior
import org.nguh.nguhcraft.MakeEnumCodec
import org.nguh.nguhcraft.event.EventDifficulty
import org.nguh.nguhcraft.mixin.entity.CreeperEntityAccessor
import java.util.Optional

// Hack so we don’t have to pass the stupid registry manager around everywhere.
private lateinit var _RegistryManager: DynamicRegistryManager

interface GhastModeAccessor {
    fun `Nguhcraft$GetGhastMode`(): MachineGunGhastMode
    fun `Nguhcraft$SetGhastMode`(mode: MachineGunGhastMode)
}

enum class MachineGunGhastMode(@JvmField val CooldownReset: Int) {
    NORMAL(-40),
    FAST(0),
    FASTER(10),
    FASTEST(17),

    /** This is as fast as it can go (1 fireball per tick). */
    INSTANT(19);

    companion object {
        @JvmField val CODEC = MakeEnumCodec<MachineGunGhastMode>()
    }
}

data class Equipment(
    private val Head: ItemStack? = null,
    private val Chest: ItemStack? = null,
    private val Legs: ItemStack? = null,
    private val Feet: ItemStack? = null,
    private val MainHand: ItemStack? = null,
    private val OffHand: ItemStack? = null,
) {
    fun Equip(ME: MobEntity) {
        Equip(ME, EquipmentSlot.HEAD, Head)
        Equip(ME, EquipmentSlot.CHEST, Chest)
        Equip(ME, EquipmentSlot.LEGS, Legs)
        Equip(ME, EquipmentSlot.FEET, Feet)
        Equip(ME, EquipmentSlot.MAINHAND, MainHand)
        Equip(ME, EquipmentSlot.OFFHAND, OffHand)
    }

    companion object {
        private fun Equip(ME: MobEntity, Slot: EquipmentSlot, S: ItemStack?) {
            if (S != null) ME.equipStack(Slot, S.copy())
        }

        val IRON_ARMOUR = Equipment(
            Head = ItemStack(Items.IRON_HELMET),
            Chest = ItemStack(Items.IRON_CHESTPLATE),
            Legs = ItemStack(Items.IRON_LEGGINGS),
            Feet = ItemStack(Items.IRON_BOOTS),
        )

        val DIAMOND_ARMOUR = Equipment(
            Head = ItemStack(Items.DIAMOND_HELMET),
            Chest = ItemStack(Items.DIAMOND_CHESTPLATE),
            Legs = ItemStack(Items.DIAMOND_LEGGINGS),
            Feet = ItemStack(Items.DIAMOND_BOOTS),
        )

        val NETHERITE_ARMOUR = Equipment(
            Head = ItemStack(Items.NETHERITE_HELMET),
            Chest = ItemStack(Items.NETHERITE_CHESTPLATE),
            Legs = ItemStack(Items.NETHERITE_LEGGINGS),
            Feet = ItemStack(Items.NETHERITE_BOOTS),
        )
    }
}

data class MobParameters(
    private val Health: Double? = null,
    private val Armour: Double? = null,
    private val ArmourToughness: Double? = null,
    private val AttackDamage: Double? = null,
    private val MovementSpeed: Double? = null,
    private val Equipment: Equipment? = null,
    private val Special: ((LivingEntity) -> Unit)? = null,
) {
    fun Apply(LE: LivingEntity) {
        // Save the current registry manager.
        _RegistryManager = LE.registryManager

        // Set default attributes.
        ApplyDefaults(LE)

        // Set health.
        if (Health != null) {
            LE.SetAttributeValue(EntityAttributes.MAX_HEALTH, Health)
            LE.health = Health.toFloat()
        }

        // Set attributes.
        if (Armour != null) LE.SetAttributeValue(EntityAttributes.ARMOR, Armour)
        if (ArmourToughness != null) LE.SetAttributeValue(EntityAttributes.ARMOR_TOUGHNESS, ArmourToughness)
        if (AttackDamage != null) LE.SetAttributeValue(EntityAttributes.ATTACK_DAMAGE, AttackDamage)
        if (MovementSpeed != null) LE.SetAttributeValue(EntityAttributes.MOVEMENT_SPEED, MovementSpeed)

        // Apply equipment before Special() since the latter may override it.
        if (LE is MobEntity) Equipment?.Equip(LE)

        // Apply special effects.
        if (Special != null) Special(LE)

        // Prevent equipment drops. Do this last as equipping things might
        // mess with these parameters.
        if (LE is MobEntity) {
            LE.setCanPickUpLoot(false)
            LE.setEquipmentDropChance(EquipmentSlot.MAINHAND, 0.0f)
            LE.setEquipmentDropChance(EquipmentSlot.OFFHAND, 0.0f)
            LE.setEquipmentDropChance(EquipmentSlot.HEAD, 0.0f)
            LE.setEquipmentDropChance(EquipmentSlot.CHEST, 0.0f)
            LE.setEquipmentDropChance(EquipmentSlot.LEGS, 0.0f)
            LE.setEquipmentDropChance(EquipmentSlot.FEET, 0.0f)
            LE.setEquipmentDropChance(EquipmentSlot.BODY, 0.0f)
        }
    }

    companion object {
        private fun ApplyDefaults(LE: LivingEntity) {
            LE.SetAttributeValue(EntityAttributes.MOVEMENT_EFFICIENCY, 1.0)
            LE.SetAttributeValue(EntityAttributes.WATER_MOVEMENT_EFFICIENCY, 1.0)
        }

        /** Override the base value of an attribute; does nothing if the attribute doesn’t exist. */
        private fun LivingEntity.SetAttributeValue(Attr: RegistryEntry<EntityAttribute>, V: Double) {
            getAttributeInstance(Attr)?.baseValue = V
        }
    }
}

data class DifficultyBasedParameters(
    private val BadDays: MobParameters,
    private val WorseDays: MobParameters,
    private val WorstDays: MobParameters,
    private val LastDays: MobParameters,
) {
    fun Apply(LE: LivingEntity, D: EventDifficulty) {
        when (D) {
            EventDifficulty.THE_GOOD_DAYS -> {}
            EventDifficulty.THE_BAD_DAYS -> BadDays.Apply(LE)
            EventDifficulty.THE_WORSE_DAYS -> WorseDays.Apply(LE)
            EventDifficulty.THE_WORST_DAYS -> WorstDays.Apply(LE)
            EventDifficulty.THE_LAST_DAYS -> LastDays.Apply(LE)
        }
    }

    fun WithSpecial(
        BadDays: ((LE: LivingEntity) -> Unit)? = null,
        WorseDays: ((LE: LivingEntity) -> Unit)? = null,
        WorstDays: ((LE: LivingEntity) -> Unit)? = null,
        LastDays: ((LE: LivingEntity) -> Unit)? = null,
    ) = DifficultyBasedParameters(
        BadDays = BadDays?.let { this.BadDays.copy(Special = BadDays) } ?: this.BadDays,
        WorseDays = WorseDays?.let { this.WorseDays.copy(Special = WorseDays) } ?: this.WorseDays,
        WorstDays = WorstDays?.let { this.WorstDays.copy(Special = WorstDays) } ?: this.WorstDays,
        LastDays = LastDays?.let { this.LastDays.copy(Special = LastDays) } ?: this.LastDays,
    )

    fun WithSpecialAll(Special: ((LivingEntity) -> Unit)) =
        WithSpecial(Special, Special, Special, Special)
}

object Parameters {
    private fun ItemStack.Enchant(E: RegistryKey<Enchantment>, Level: Int = 1) =
        also { _RegistryManager.getOptionalEntry(E).ifPresent { this.addEnchantment(it, Level) } }

    private fun EquipMainHand(I: Item, Consumer: (ItemStack) -> Unit) = { LE: LivingEntity ->
        LE.equipStack(EquipmentSlot.MAINHAND, ItemStack(I).also(Consumer))
    }

    private fun MakeBowAndArrows(Difficulty: Int) = { LE: LivingEntity ->
        val Bow = ItemStack(Items.BOW)
            .Enchant(Enchantments.FLAME, 1)
            .Enchant(Enchantments.POWER, 1 + Difficulty)

        val Arrows = ItemStack(Items.TIPPED_ARROW).also { it.set(DataComponentTypes.POTION_CONTENTS, PotionContentsComponent(
            Optional.empty(),
            Optional.of(StatusEffects.INSTANT_DAMAGE.value().color),
            listOf(
                StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, Difficulty / 2)
            ),
            Optional.empty(),
        )) }

        LE.equipStack(EquipmentSlot.MAINHAND, Bow)
        LE.equipStack(EquipmentSlot.OFFHAND, Arrows)
    }

    private fun MakeCreeper(FuseTime: Int, Charged: Boolean = false) = { LE: LivingEntity ->
        val C = LE as CreeperEntity
        (C as CreeperEntityAccessor).setFuseTime(FuseTime)
        if (Charged) C.dataTracker.set(CreeperEntityAccessor.getCharged(), true)
    }

    private val Creeper = DifficultyBasedParameters(
        BadDays = MobParameters(
            Health = 30.0,
            MovementSpeed = .3,
            Special = MakeCreeper(25)
        ),
        WorseDays = MobParameters(
            Health = 30.0,
            MovementSpeed = .4,
            Special = MakeCreeper(20, true)
        ),
        WorstDays = MobParameters(
            Health = 35.0,
            MovementSpeed = .5,
            Special = MakeCreeper(15, true)
        ),
        LastDays = MobParameters(
            Health = 40.0,
            MovementSpeed = .75,
            Special = MakeCreeper(10, true)
        ),
    )

    private val Ghast = DifficultyBasedParameters(
        BadDays = MobParameters(
            Health = 30.0,
            Armour = 2.0,
            ArmourToughness = 1.0,
            Special = { (it as GhastModeAccessor).`Nguhcraft$SetGhastMode`(MachineGunGhastMode.FAST) }
        ),
        WorseDays = MobParameters(
            Health = 40.0,
            Armour = 4.0,
            ArmourToughness = 2.0,
            Special = { (it as GhastModeAccessor).`Nguhcraft$SetGhastMode`(MachineGunGhastMode.FASTER) }
        ),
        WorstDays = MobParameters(
            Health = 50.0,
            Armour = 6.0,
            ArmourToughness = 4.0,
            Special = { (it as GhastModeAccessor).`Nguhcraft$SetGhastMode`(MachineGunGhastMode.FASTEST) }
        ),
        LastDays = MobParameters(
            Health = 60.0,
            Armour = 10.0,
            ArmourToughness = 6.0,
            Special = { (it as GhastModeAccessor).`Nguhcraft$SetGhastMode`(MachineGunGhastMode.INSTANT) }
        ),
    )

    private val Skeleton = DifficultyBasedParameters(
        BadDays = MobParameters(
            Health = 30.0,
            Armour = 2.0,
            ArmourToughness = 1.0,
            Equipment = Equipment(Head = ItemStack(Items.LEATHER_HELMET)),
            Special = MakeBowAndArrows(1)
        ),
        WorseDays = MobParameters(
            Health = 35.0,
            Armour = 3.0,
            ArmourToughness = 1.5,
            Equipment = Equipment(Head = ItemStack(Items.IRON_HELMET)),
            Special = MakeBowAndArrows(2)
        ),
        WorstDays = MobParameters(
            Health = 40.0,
            Armour = 4.0,
            ArmourToughness = 2.0,
            Equipment = Equipment(Head = ItemStack(Items.DIAMOND_HELMET)),
            Special = MakeBowAndArrows(3)
        ),
        LastDays = MobParameters(
            Health = 40.0,
            Armour = 5.0,
            ArmourToughness = 2.5,
            Equipment = Equipment(Head = ItemStack(Items.NETHERITE_HELMET)),
            Special = MakeBowAndArrows(4)
        ),
    )

    private val Vindicator = DifficultyBasedParameters(
        BadDays = MobParameters(
            Health = 40.0,
            Armour = 1.5,
            ArmourToughness = 1.0,
            AttackDamage = 8.5
        ),
        WorseDays = MobParameters(
            Health = 52.0,
            Armour = 2.5,
            ArmourToughness = 1.5,
            AttackDamage = 12.0,
            Equipment = Equipment(MainHand = ItemStack(Items.DIAMOND_AXE))
        ),
        WorstDays = MobParameters(
            Health = 68.0,
            Armour = 4.0,
            ArmourToughness = 2.5,
            AttackDamage = 16.0,
            Equipment = Equipment(MainHand = ItemStack(Items.DIAMOND_AXE))
        ),
        LastDays = MobParameters(
            Health = 84.0,
            Armour = 6.0,
            ArmourToughness = 3.5,
            AttackDamage = 25.0,
            Equipment = Equipment(MainHand = ItemStack(Items.NETHERITE_AXE))
        ),
    )

    private val Zombie = DifficultyBasedParameters(
        BadDays = MobParameters(
            Health = 30.0,
            Armour = 2.0,
            ArmourToughness = 1.0,
            AttackDamage = 5.0,
            Equipment = Equipment(Head = ItemStack(Items.LEATHER_HELMET))
        ),
        WorseDays = MobParameters(
            Health = 25.0,
            AttackDamage = 7.0,
            Equipment = Equipment.IRON_ARMOUR.copy(MainHand = ItemStack(Items.IRON_SWORD))
        ),
        WorstDays = MobParameters(
            Health = 30.0,
            AttackDamage = 9.0,
            Equipment = Equipment.DIAMOND_ARMOUR.copy(MainHand = ItemStack(Items.DIAMOND_SWORD))
        ),
        LastDays = MobParameters(
            Health = 35.0,
            AttackDamage = 11.0,
            Equipment = Equipment.NETHERITE_ARMOUR.copy(MainHand = ItemStack(Items.NETHERITE_SWORD))
        ),
    )

    private val Drowned = Zombie.WithSpecialAll(EquipMainHand(Items.TRIDENT) { it.Enchant(Enchantments.CHANNELING, 2) })

    val BY_TYPE = hashMapOf(
        EntityType.GHAST to Ghast,
        EntityType.ZOMBIE to Zombie,
        EntityType.DROWNED to Drowned,
        EntityType.SKELETON to Skeleton,
        EntityType.STRAY to Skeleton,
        EntityType.BOGGED to Skeleton,
        EntityType.VINDICATOR to Vindicator,
        EntityType.CREEPER to Creeper,
    )
}