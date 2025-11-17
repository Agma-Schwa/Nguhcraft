package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HopperBlockEntity.class)
public interface HopperBlockEntityAccessor {
    @Accessor
    Direction getFacing();
}
