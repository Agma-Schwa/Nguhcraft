package org.nguh.nguhcraft

import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.entity.player.Player
import net.minecraft.world.entity.projectile.ThrownTrident
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.Utils.EnchantLvl
import org.nguh.nguhcraft.accessors.ProjectileEntityAccessor
import org.nguh.nguhcraft.accessors.TridentEntityAccessor
import org.nguh.nguhcraft.server.ServerUtils.MaybeEnterHypershotContext
import org.nguh.nguhcraft.server.ServerUtils.StrikeLightning

object TridentUtils {
    @JvmStatic
    fun ActOnBlockHit(TE: ThrownTrident, BHR: BlockHitResult) {
        // Don’t do anything if this has already dealt damage to prevent a
        // trident with no loyalty from striking lightning both when an entity
        // is hit and when it falls to the ground afterward.
        if ((TE as TridentEntityAccessor).`Nguhcraft$DealtDamage`()) return
        val W = TE.level()
        val Lvl = EnchantLvl(W, TE.pickupItemStackOrigin, Enchantments.CHANNELING)
        if (W is ServerLevel && Lvl >= 2) {
            StrikeLightning(W, Vec3.atBottomCenterOf(BHR.blockPos), TE)
            TE.playSound(SoundEvents.TRIDENT_THUNDER.value(), 5f, 1.0f)
        }
    }

    /** Handle Channeling II on entity hit. */
    @JvmStatic
    fun ActOnEntityHit(TE: ThrownTrident, EHR: EntityHitResult) {
        var SE = SoundEvents.TRIDENT_HIT
        var Volume = 1.0f

        // Check if it’s thundering or if we have Channeling II.
        //
        // This means we strike lightning *twice* if it’s also thundering,
        // which seems fine.
        val W = TE.level()
        val Thunder = W.isThundering
        val Lvl = EnchantLvl(W, TE.pickupItemStackOrigin, Enchantments.CHANNELING)
        if (W is ServerLevel && Lvl > 0 && (Thunder || Lvl >= 2)) {
            EHR.entity.invulnerableTime = 0
            val Where = EHR.entity.blockPosition()
            if (Lvl >= 2 || W.canSeeSky(Where)) {
                StrikeLightning(W, EHR.entity.position(), TE)
                SE = SoundEvents.TRIDENT_THUNDER.value()
                Volume = 5.0f
            }
        }

        TE.playSound(SE, Volume, 1.0f)
    }

    /** Handle multishot tridents. */
    @JvmStatic
    fun ActOnTridentThrown(W: Level, PE: Player, S: ItemStack, Extra: Int = 0) {
        val Lvl = EnchantLvl(W, S, Enchantments.MULTISHOT)
        val K = W.getRandom().nextFloat() / 10f // Random value I picked that works well enough.
        val Yaw = PE.yRot
        val Pitch = PE.xRot

        // Enter hypershot context, if applicable.
        val HS = MaybeEnterHypershotContext(PE, PE.usedItemHand, S, listOf(), 2.5F, 1F, false)

        // Launch tridents.
        for (I in 0 until Lvl + Extra) {
            val TE = ThrownTrident(W, PE, S)
            TE.shootFromRotation(PE, Pitch, Yaw, 0F, 2.5F + K * .5F, 1F + .1F * I)

            // Mark that this trident is a copy; this disables item pickup, makes it
            // despawn after 5 seconds, and tells that client that it doesn’t have
            // loyalty so the copies don’t try to return to the owner.
            (TE as TridentEntityAccessor).`Nguhcraft$SetCopy`()
            if (HS) (TE as ProjectileEntityAccessor).MakeHypershotProjectile()
            W.addFreshEntity(TE)
        }
    }
}