package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.entity.EntityEquipment;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityEquipmentAccessor {
    @Accessor
    EntityEquipment getEquipment();
}
