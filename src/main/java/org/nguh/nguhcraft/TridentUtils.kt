package org.nguh.nguhcraft

import com.mojang.logging.LogUtils
import net.minecraft.core.BlockPos
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.tags.BlockTags
import net.minecraft.world.entity.projectile.arrow.ThrownTrident
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.EntityHitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import org.nguh.nguhcraft.Utils.EnchantLvl
import org.nguh.nguhcraft.accessors.ProjectileEntityAccessor
import org.nguh.nguhcraft.accessors.TridentEntityAccessor
import org.nguh.nguhcraft.server.ServerUtils.MaybeEnterHypershotContext
import org.nguh.nguhcraft.server.ServerUtils.StrikeLightning

object TridentUtils {
    val LOGGER = LogUtils.getLogger()

    /**
     * Determine if we should strike lightning.
     *
     * This both implements Channelling II and reimplements Channelling I. We disable
     * the builtin Channelling behaviour by overwriting 'channeling.json' in our data
     * directory.
     */
    private fun ShouldStrikeLightning(TE: ThrownTrident, HR: HitResult): Boolean {
        val L = TE.level()
        assert(L is ServerLevel) { "ShouldStrikeLightning() should only run on the server" }

        // Don’t do anything if this has already struck lightning once to prevent a
        // trident with no loyalty from striking lightning both when an entity is hit
        // and when it falls to the ground afterward.
        if ((TE as TridentEntityAccessor).`Nguhcraft$GetStruckLightning`()) return false

        // Is this a Channelling trident?
        val Lvl = EnchantLvl(L, TE.pickupItemStackOrigin, Enchantments.CHANNELING)
        if (Lvl < 1) return false

        // Always strike lightning if this is Channelling II.
        if (Lvl >= 2) return true

        // For Channelling I, the hit position must be exposed to the sky in thundering weather.
        if (!L.isThundering || !L.canSeeSky(BlockPos.containing(HR.location))) return false

        // Strike lightning if we hit an entity or a lightning rod.
        if (HR is EntityHitResult) return true
        val Pos = (HR as BlockHitResult).blockPos
        return L.getBlockState(Pos).`is`(BlockTags.LIGHTNING_RODS)
    }

    /** Strike lighting if appropriate. */
    private fun MaybeStrikeLighting(TE: ThrownTrident, HR: HitResult, Where: Vec3): Boolean {
        // Check if we should do this to begin with.
        val L = TE.level()
        if (L !is ServerLevel || !ShouldStrikeLightning(TE, HR)) return false

        // Strike lightning. This may fail if the entity can’t be created.
        val LE = StrikeLightning(L, Where) ?: return false

        // If we succeeded, mark that we’ve dealt damage.
        LE.cause = TE.owner as? ServerPlayer
        (TE as TridentEntityAccessor).`Nguhcraft$SetStruckLightning`()
        TE.playSound(SoundEvents.TRIDENT_THUNDER.value(), 5f, 1.0f)
        return true
    }

    /** Called when a trident hits a block. */
    @JvmStatic
    fun ActOnBlockHit(TE: ThrownTrident, BHR: BlockHitResult) {
        MaybeStrikeLighting(TE, BHR, Vec3.atBottomCenterOf(BHR.blockPos))
    }

    /** Called when a trident hits an entity. */
    @JvmStatic
    fun ActOnEntityHit(TE: ThrownTrident, EHR: EntityHitResult) {
        // FIXME: For some reason, we seem to end up striking lightning twice
        //        if we hit en entity with a Channelling II trident while it is
        //        thundering, but I can’t be bothered to fix that.
        if (MaybeStrikeLighting(TE, EHR, EHR.entity.position())) {
            // Reset this to make multishot tridents work; otherwise, once multiple
            // tridents hit an entity in quick succession, only one of them would
            // deal damage, which would make multishot tridents a bit pointless.
            EHR.entity.invulnerableTime = 0
            return
        }

        // Play the normal hit sound. We disable the sound that would normally play
        // via a mixin, so we need to do this even if we don't strike lightning; if
        // we do, we play the sound in MaybeStrikeLightning().
        TE.playSound(SoundEvents.TRIDENT_HIT, 1.0f, 1.0f)
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