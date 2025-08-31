package org.nguh.nguhcraft.mixin.entity;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.mob.CreeperEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(CreeperEntity.class)
public interface CreeperEntityAccessor {
    @Accessor("CHARGED")
    static TrackedData<Boolean> getCharged() { throw new AssertionError(); }

    @Accessor("fuseTime")
    void setFuseTime(int fuseTime);
}