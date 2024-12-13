package org.nguh.nguhcraft

import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.damage.DamageType
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.world.World
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhDamageTypes {
    val MINECART_COLLISION = register("minecart_collision")
    val MINECART_RUN_OVER = register("minecart_run_over")
    val MINECART_POOR_TRACK_DESIGN = register("minecart_poor_track_design")
    val OBLITERATED = register("obliterated")

    fun MinecartRunOverBy(W: World, P: PlayerEntity? = null) = DamageSource(entry(W, MINECART_RUN_OVER), P)
    fun MinecartCollision(W: World, P: PlayerEntity? = null) = DamageSource(entry(W, MINECART_COLLISION), P)
    fun MinecartPoorTrackDesign(W: World, P: PlayerEntity? = null) = DamageSource(entry(W, MINECART_POOR_TRACK_DESIGN), P)
    fun Obliterated(W: World) = DamageSource(entry(W, OBLITERATED), null as Entity?)

    private fun register(Key: String) = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, Id(Key))
    private fun entry(W: World, Key: RegistryKey<DamageType>) =
        W.registryManager.getOrThrow(RegistryKeys.DAMAGE_TYPE).getOrThrow(Key)
}