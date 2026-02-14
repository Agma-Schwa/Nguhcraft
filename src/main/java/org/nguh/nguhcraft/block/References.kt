package org.nguh.nguhcraft.block

import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

/**
 * Stupid fucking hack that is also used by vanilla Minecraft
 *
 * Block creation depending on items is very much not good and leads to horrible
 * problems due to class initialisation order being all wrong; thus, NguhBlocks
 * must NEVER reference Items or NguhItems in its class initialiser; this stupid
 * class is a work around for that and holds the resource keys for any stupid items
 * that need to be referenced in the NguhBlocks class initialiser.
 */
object References {
    val APPLE_ITEM = ResourceKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace("apple"))!!
    val CHERRY_ITEM = ResourceKey.create(Registries.ITEM, Id("cherry"))!!
}