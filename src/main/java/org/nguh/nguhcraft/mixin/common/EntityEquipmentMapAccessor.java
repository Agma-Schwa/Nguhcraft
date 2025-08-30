package org.nguh.nguhcraft.mixin.common;

import net.minecraft.entity.EntityEquipment;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.EnumMap;

@Mixin(EntityEquipment.class)
public interface EntityEquipmentMapAccessor {
    @Accessor EnumMap<EquipmentSlot, ItemStack> getMap();
}
