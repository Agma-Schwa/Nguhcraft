package org.nguh.nguhcraft.tags

import net.minecraft.core.registries.Registries
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.EntityType
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhTags {
    val CAN_EQUIP_HOTSPOT_GLASSES: TagKey<EntityType<*>> =
        TagKey.create(Registries.ENTITY_TYPE, Id("can_equip_hotspot_glasses"))
}