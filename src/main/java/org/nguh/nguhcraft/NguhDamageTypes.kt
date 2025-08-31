package org.nguh.nguhcraft

import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.Registerable
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.world.World
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhDamageTypes {
    val ARCANE = Register("arcane")
    val MINECART_COLLISION = Register("minecart_collision")
    val MINECART_RUN_OVER = Register("minecart_run_over")
    val MINECART_POOR_TRACK_DESIGN = Register("minecart_poor_track_design")
    val OBLITERATED = Register("obliterated")
    val BYPASSES_RESISTANCES = arrayOf(MINECART_COLLISION, MINECART_RUN_OVER, MINECART_POOR_TRACK_DESIGN, OBLITERATED)

    fun Bootstrap(R: Registerable<DamageType>) {
        R.register(ARCANE, DamageType("arcane", 0.0f))
        R.register(MINECART_COLLISION, DamageType("minecart_collision", 0.0f))
        R.register(MINECART_POOR_TRACK_DESIGN, DamageType("minecart_poor_track_design", 0.0f))
        R.register(MINECART_RUN_OVER, DamageType("minecart_run_over", 0.0f))
        R.register(OBLITERATED, DamageType("obliterated", 0.0f))
    }

    @JvmStatic fun Arcane(W: World, Attacker: Entity) = DamageSource(Entry(W, ARCANE), Attacker)
    @JvmStatic fun MinecartRunOverBy(W: World, P: PlayerEntity? = null) = DamageSource(Entry(W, MINECART_RUN_OVER), P)
    @JvmStatic fun MinecartCollision(W: World, P: PlayerEntity? = null) = DamageSource(Entry(W, MINECART_COLLISION), P)
    @JvmStatic fun MinecartPoorTrackDesign(W: World, P: PlayerEntity? = null) = DamageSource(Entry(W, MINECART_POOR_TRACK_DESIGN), P)
    @JvmStatic fun Obliterated(W: World) = DamageSource(Entry(W, OBLITERATED), null as Entity?)

    private fun Register(Key: String) = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Id(Key))
    private fun Entry(W: World, Key: RegistryKey<DamageType>) =
        W.registryManager.getOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(Key)
}