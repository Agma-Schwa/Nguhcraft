package org.nguh.nguhcraft.server

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.TridentItem
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import net.minecraft.util.Hand
import org.nguh.nguhcraft.TridentUtils
import org.nguh.nguhcraft.mixin.server.RangedWeaponItemAccessor
import org.nguh.nguhcraft.network.ClientFlags
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor

data class HypershotContext(
    /** The hand the weapon is held in. */
    val Hand: Hand,

    /** The weapon used to fire the projectile. */
    val Weapon: ItemStack,

    /** List of projectiles to fire. */
    val Projectiles: List<ItemStack>,

    /** Initial speed modifier. */
    val Speed: Float,

    /** Initial divergence modifier. */
    val Divergence: Float,

    /** Whether this projectile is fully charged. */
    val Critical: Boolean,

    /** Remaining ticks. */
    var Ticks: Int
) {
    /**
    * Tick this context.
    *
    * @return `true` if we should remove the context, `false` otherwise.
    */
    fun Tick(SW: ServerWorld, Shooter: LivingEntity): Boolean {
        if (TickImpl(SW, Shooter) == EXPIRED) {
            (Shooter as LivingEntityAccessor).hypershotContext = null
            if (Shooter is ServerPlayerEntity) Shooter.SetClientFlag(
                ClientFlags.IN_HYPERSHOT_CONTEXT,
                false
            )

            return EXPIRED
        }

        return !EXPIRED
    }

    private fun TickImpl(SW: ServerWorld, Shooter: LivingEntity): Boolean {
        // Cancel if we should stop or are dead.
        if (Ticks-- < 1 || Shooter.isDead || Shooter.isRemoved) return EXPIRED

        // Make sure we’re still holding the item.
        if (Shooter.getStackInHand(Hand) != Weapon) return EXPIRED

        // Ok, fire the projectile(s).
        //
        // Take care to duplicate the projectile item stacks unless we’re on the
        // last tick, in which case we can just use the original list.
        val I = Weapon.item
        if (I is RangedWeaponItemAccessor) {
            I.InvokeShootAll(
                SW,
                Shooter,
                Hand,
                Weapon,
                if (Ticks < 1) Projectiles else Projectiles.toList().map { it.copy() },
                Speed,
                Divergence,
                Critical,
                null
            )

            // Also play a sound effect.
            if (Shooter is PlayerEntity) SW.playSound(
                null,
                Shooter.x,
                Shooter.y,
                Shooter.z,
                SoundEvents.ENTITY_ARROW_SHOOT,
                SoundCategory.PLAYERS,
                1.0f,
                1.0f / (SW.getRandom().nextFloat() * 0.4f + 1.2f)
            )
        }

        // We also support tridents here.
        else if (I is TridentItem && Shooter is PlayerEntity) {
            TridentUtils.ActOnTridentThrown(
                SW,
                Shooter,
                Weapon,
                1
            )
        }

        // Keep ticking this.
        return !EXPIRED
    }

    companion object {
        const val EXPIRED: Boolean = true
    }
}
