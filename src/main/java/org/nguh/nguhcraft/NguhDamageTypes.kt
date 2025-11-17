package org.nguh.nguhcraft

import net.minecraft.world.entity.Entity
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.player.Player
import net.minecraft.data.worldgen.BootstrapContext
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhDamageTypes {
    val ARCANE = Register("arcane")
    val MINECART_COLLISION = Register("minecart_collision")
    val MINECART_RUN_OVER = Register("minecart_run_over")
    val MINECART_POOR_TRACK_DESIGN = Register("minecart_poor_track_design")
    val OBLITERATED = Register("obliterated")
    val STONECUTTER = Register("stonecutter")
    val BYPASSES_RESISTANCES = arrayOf(MINECART_COLLISION, MINECART_RUN_OVER, MINECART_POOR_TRACK_DESIGN, OBLITERATED)

    fun Bootstrap(R: BootstrapContext<DamageType>) {
        R.register(ARCANE, DamageType("arcane", 0.0f))
        R.register(MINECART_COLLISION, DamageType("minecart_collision", 0.0f))
        R.register(MINECART_POOR_TRACK_DESIGN, DamageType("minecart_poor_track_design", 0.0f))
        R.register(MINECART_RUN_OVER, DamageType("minecart_run_over", 0.0f))
        R.register(OBLITERATED, DamageType("obliterated", 0.0f))
        R.register(STONECUTTER, DamageType("stonecutter", 0.0f))
    }

    @JvmStatic fun Arcane(W: Level, Attacker: Entity) = DamageSource(Entry(W, ARCANE), Attacker)
    @JvmStatic fun MinecartRunOverBy(W: Level, P: Player? = null) = DamageSource(Entry(W, MINECART_RUN_OVER), P)
    @JvmStatic fun MinecartCollision(W: Level, P: Player? = null) = DamageSource(Entry(W, MINECART_COLLISION), P)
    @JvmStatic fun MinecartPoorTrackDesign(W: Level, P: Player? = null) = DamageSource(Entry(W, MINECART_POOR_TRACK_DESIGN), P)
    @JvmStatic fun Obliterated(W: Level) = DamageSource(Entry(W, OBLITERATED), null as Entity?)
    @JvmStatic fun Stonecutter(W: Level) = DamageSource(Entry(W, STONECUTTER), null as Entity?)

    private fun Register(Key: String) = ResourceKey.create(Registries.DAMAGE_TYPE, Id(Key))
    private fun Entry(W: Level, Key: ResourceKey<DamageType>) =
        W.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(Key)
}