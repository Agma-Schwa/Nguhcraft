package org.nguh.nguhcraft.accessors;

import net.minecraft.entity.LivingEntity;

public interface ProjectileEntityAccessor {
    void SetHomingTarget(LivingEntity Target);
    void MakeHypershotProjectile();
}
