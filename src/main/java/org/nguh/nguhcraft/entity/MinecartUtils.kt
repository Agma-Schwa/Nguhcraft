package org.nguh.nguhcraft.entity

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.vehicle.AbstractMinecart
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import org.nguh.nguhcraft.NguhDamageTypes
import kotlin.math.abs

object MinecartUtils {
    private const val COLLISION_THRESHOLD = 0.5
    private const val DAMAGE_PER_BLOCK_PER_SEC = 4

    /** Check if we can damage a player. */
    private fun Damage(
        Level: ServerLevel,
        SP: ServerPlayer?,
        Dmg: Float,
        InCart: Boolean,
        Attacker: ServerPlayer?
    ): Boolean {
        if (
            SP == null ||
           !SP.isAlive ||
            SP.isCreative ||
            SP.isSpectator ||
            SP.hurtTime != 0
        ) return false

        val DS = GetDamageSource(Level, InCart, Attacker)
        SP.hurtServer(Level, DS, Dmg)
        return true
    }

    /** Drop a minecart at a location. */
    private fun DropMinecart(Level: ServerLevel, C: AbstractMinecart) {
        Level.addFreshEntity(ItemEntity(
            Level,
            C.x + C.random.nextFloat(),
            C.y,
            C.z + C.random.nextFloat(),
            C.pickResult!!
        ))
    }

    /** Predicate that tests whether an entity can collide with a minecart. */
    private fun CollisionCheckPredicate(E: Entity): Boolean {
        if (E.isRemoved) return false
        if (E is AbstractMinecart) return true
        if (E !is ServerPlayer) return false
        return !E.isSpectator && !E.isCreative
    }

    /** Get the damage source to use for a collision.  */
    private fun GetDamageSource(
        W: ServerLevel,
        InCart: Boolean,
        OtherPlayer: ServerPlayer?
    ) = if (InCart) NguhDamageTypes.MinecartCollision(W, OtherPlayer)
    else NguhDamageTypes.MinecartRunOverBy(W, OtherPlayer)

    /** Perform minecart collisions. Returns 'true' if we handled a collision. */
    @JvmStatic
    fun HandleCollisions(C: AbstractMinecart): Boolean {
        // Don’t collide if we’re not on a track or too slow, or if
        // our controlling passenger is dead.
        //
        // Two minecarts that don’t have a player are going to be moving
        // too slowly for collisions anyway, so skip them; if another cart
        // that we are colliding with does have a player, but we don’t, we’ll
        // register the collision whenever the other cart is ticked.
        val OurPlayer = C.firstPassenger as? ServerPlayer
        val HasPlayer = OurPlayer != null && OurPlayer.isAlive
        val OurSpeed = C.deltaMovement.horizontalDistance()
        if (
            C.level().isClientSide ||
           !C.isOnRails ||
            OurSpeed < COLLISION_THRESHOLD ||
           !HasPlayer
        ) return HasPlayer

        // Calculate bounding box at the target position.
        val BB = C.boundingBox.minmax(OurPlayer.boundingBox)

        // Check for colliding entities.
        val Level = C.level() as ServerLevel
        val Entities = Level.getEntities(C, BB, MinecartUtils::CollisionCheckPredicate)
        for (E in Entities) {
            // Don’t collide with our own passenger.
            if (E == OurPlayer) continue

            // Extract the minecart and the player.
            var OtherMC: AbstractMinecart? = null
            var OtherPlayer: ServerPlayer? = null
            if (E is ServerPlayer) {
                OtherPlayer = E
                val V = E.vehicle
                if (V is AbstractMinecart) OtherMC = V
            } else if (E is AbstractMinecart) {
                OtherMC = E
                val P = E.firstPassenger
                if (P is ServerPlayer) OtherPlayer = P
            }

            // Deal damage to the poor soul we just ran over. If they’re also in a
            // minecart, use the combined speed of both carts to calculate the
            // damage if they’re moving in opposing directions, and the difference
            // if they’re moving in the same direction.
            val CombinedSpeed = if (OtherMC != null) {
                val OtherSpeed = OtherMC.deltaMovement.horizontalDistance().toFloat()
                val OtherDir = OtherMC.deltaMovement.normalize()
                val OurDir = C.deltaMovement.normalize()
                if (OtherDir.dot(OurDir) < 0) OurSpeed + OtherSpeed
                else abs(OurSpeed - OtherSpeed)
            } else {
                OurSpeed
            }

            val Where = C.position()
            val Dmg = CombinedSpeed.toFloat() * DAMAGE_PER_BLOCK_PER_SEC
            var DealtDamage = Damage(Level, OtherPlayer, Dmg, OtherMC != null, OurPlayer)

            // If there is a minecart, kill it and us as well. Note that we can also
            // collide with players that aren’t riding a minecart, so there may not
            // be a minecart here.
            if (OtherMC != null) {
                DealtDamage = Damage(Level, OurPlayer, Dmg, true, OtherPlayer) || DealtDamage
                DropMinecart(Level, C)
                DropMinecart(Level, OtherMC)
                OtherMC.kill(Level)
                C.kill(Level)
            }

            // Play sound and particles.
            if (DealtDamage) {
                C.playSound(SoundEvents.TOTEM_USE, 2f, 1f)
                Level.sendParticles(
                    ParticleTypes.EXPLOSION_EMITTER,
                    Where.x,
                    Where.y,
                    Where.z,
                    1,
                    .0,
                    .0,
                    .0,
                    .0
                )
            }

            // Do not process any more collisions. It is extremely unlikely that more than
            // two parties are ever involved in one, and we don’t want to get into a weird
            // state where we kill the same entity more than once.
            break
        }

        return true
    }
}