package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ProjectileWeaponItem.class)
public interface ProjectileWeaponItemAccessor {
    @Invoker("shoot")
    void InvokeShootAll(
        ServerLevel SW,
        LivingEntity Shooter,
        InteractionHand Hand,
        ItemStack Weapon,
        List<ItemStack> Projectiles,
        float Speed,
        float Div,
        boolean Crit,
        LivingEntity Tgt
    );
}
