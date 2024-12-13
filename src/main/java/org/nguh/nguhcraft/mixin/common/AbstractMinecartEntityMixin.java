package org.nguh.nguhcraft.mixin.common;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.entity.vehicle.VehicleEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AbstractMinecartEntity.class)
public abstract class AbstractMinecartEntityMixin extends VehicleEntity {
    public AbstractMinecartEntityMixin(EntityType<?> entityType, World world) {
        super(entityType, world);
    }

    /**
     * If this minecart is being ridden by a player, prevent collisions
     * with anything that is not a player or minecart.
     *
     * @reason Complete replacement.
     * @author Sirraide
     */
    @Overwrite
    public boolean collidesWith(Entity E) {
        if (!(getFirstPassenger() instanceof PlayerEntity))
            return AbstractBoatEntity.canCollide(this, E);
        return false;
    }
}
