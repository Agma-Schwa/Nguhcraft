package org.nguh.nguhcraft.entity

import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.vehicle.AbstractMinecartEntity
import net.minecraft.particle.ParticleTypes
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundEvents
import org.nguh.nguhcraft.NguhDamageTypes
import kotlin.math.abs

object MinecartUtils {
    private const val COLLISION_THRESHOLD = 0.5
    private const val DAMAGE_PER_BLOCK_PER_SEC = 4

    /** Check if we can damage a player. */
    private fun Damage(
        Level: ServerWorld,
        SP: ServerPlayerEntity?,
        Dmg: Float,
        InCart: Boolean,
        Attacker: ServerPlayerEntity?
    ): Boolean {
        if (
            SP == null ||
           !SP.isAlive ||
            SP.isCreative ||
            SP.isSpectator ||
            SP.hurtTime != 0
        ) return false

        val DS = GetDamageSource(Level, InCart, Attacker)
        SP.damage(Level, DS, Dmg)
        return true
    }

    /** Drop a minecart at a location. */
    private fun DropMinecart(Level: ServerWorld, C: AbstractMinecartEntity) {
        Level.spawnEntity(ItemEntity(
            Level,
            C.x + C.random.nextFloat(),
            C.y,
            C.z + C.random.nextFloat(),
            C.pickBlockStack
        ))
    }

    /** Predicate that tests whether an entity can collide with a minecart. */
    private fun CollisionCheckPredicate(E: Entity): Boolean {
        if (E.isRemoved) return false
        if (E is AbstractMinecartEntity) return true
        if (E !is ServerPlayerEntity) return false
        return !E.isSpectator && !E.isCreative
    }

    /** Get the damage source to use for a collision.  */
    private fun GetDamageSource(
        W: ServerWorld,
        InCart: Boolean,
        OtherPlayer: ServerPlayerEntity?
    ) = if (InCart) NguhDamageTypes.MinecartCollision(W, OtherPlayer)
    else NguhDamageTypes.MinecartRunOverBy(W, OtherPlayer)

    /** Perform minecart collisions. Returns 'true' if we handled a collision. */
    @JvmStatic
    fun HandleCollisions(C: AbstractMinecartEntity): Boolean {
        // Don’t collide if we’re not on a track or too slow, or if
        // our controlling passenger is dead.
        //
        // Two minecarts that don’t have a player are going to be moving
        // too slowly for collisions anyway, so skip them; if another cart
        // that we are colliding with does have a player, but we don’t, we’ll
        // register the collision whenever the other cart is ticked.
        val OurPlayer = C.firstPassenger as? ServerPlayerEntity
        val HasPlayer = OurPlayer != null && OurPlayer.isAlive
        val OurSpeed = C.velocity.horizontalLength()
        if (
            C.world.isClient ||
           !C.isOnRail ||
            OurSpeed < COLLISION_THRESHOLD ||
           !HasPlayer
        ) return HasPlayer

        // Calculate bounding box at the target position.
        val BB = C.boundingBox.union(OurPlayer.boundingBox)

        // Check for colliding entities.
        val Level = C.world as ServerWorld
        val Entities = Level.getOtherEntities(C, BB, MinecartUtils::CollisionCheckPredicate)
        for (E in Entities) {
            // Don’t collide with our own passenger.
            if (E == OurPlayer) continue

            // Extract the minecart and the player.
            var OtherMC: AbstractMinecartEntity? = null
            var OtherPlayer: ServerPlayerEntity? = null
            if (E is ServerPlayerEntity) {
                OtherPlayer = E
                val V = E.vehicle
                if (V is AbstractMinecartEntity) OtherMC = V
            } else if (E is AbstractMinecartEntity) {
                OtherMC = E
                val P = E.firstPassenger
                if (P is ServerPlayerEntity) OtherPlayer = P
            }

            // Deal damage to the poor soul we just ran over. If they’re also in a
            // minecart, use the combined speed of both carts to calculate the
            // damage if they’re moving in opposing directions, and the difference
            // if they’re moving in the same direction.
            val CombinedSpeed = if (OtherMC != null) {
                val OtherSpeed = OtherMC.velocity.horizontalLength().toFloat()
                val OtherDir = OtherMC.velocity.normalize()
                val OurDir = C.velocity.normalize()
                if (OtherDir.dotProduct(OurDir) < 0) OurSpeed + OtherSpeed
                else abs(OurSpeed - OtherSpeed)
            } else {
                OurSpeed
            }

            val Where = C.pos
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
                C.playSound(SoundEvents.ITEM_TOTEM_USE, 2f, 1f)
                Level.spawnParticles(
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