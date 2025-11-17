package org.nguh.nguhcraft.mixin.server;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.accessors.ProjectileEntityAccessor;
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments;
import org.nguh.nguhcraft.server.ServerUtils;
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.function.Consumer;

import static org.nguh.nguhcraft.Utils.EnchantLvl;

@Mixin(ProjectileWeaponItem.class)
public abstract class ProjectileWeaponItemMixin {
    /** At the start of the function, compute homing and hypershot info. */
    @Inject(method = "shoot", at = @At("HEAD"))
    private void inject$shootAll$0(
        ServerLevel W,
        LivingEntity Shooter,
        InteractionHand Hand,
        ItemStack Weapon,
        List<ItemStack> Projectiles,
        float Speed,
        float Div,
        boolean Crit,
        @Nullable LivingEntity Tgt,
        CallbackInfo CI,
        @Share("HomingTarget") LocalRef<LivingEntity> HomingTarget,
        @Share("Hypershot") LocalRef<Boolean> IsHypershot,
        @Share("DisallowItemPickup") LocalRef<Boolean> DisallowItemPickup
    ) {
        // Apply homing.
        if (EnchantLvl(W, Weapon, NguhcraftEnchantments.HOMING) != 0)
            HomingTarget.set(ServerUtils.MaybeMakeHomingArrow(W, Shooter));

        // If we’re not already in a hypershot context, apply hypershot. In
        // any case, make the arrow a hypershot arrow if we end up in a hypershot
        // context. This is so we can cancel invulnerability time for the entity
        // hit by this arrow.
        var AlreadyInHypershotContext = ((LivingEntityAccessor)Shooter).getHypershotContext() != null;
        var HS = ServerUtils.MaybeEnterHypershotContext(Shooter, Hand, Weapon, Projectiles, Speed, Div, Crit);
        IsHypershot.set(HS);

        // We need to disallow item pickup if this is not the first arrow shot
        // from a hypershot bow.
        DisallowItemPickup.set(HS && !AlreadyInHypershotContext);
    }

    /** Then, when we shoot an arrow, set the target appropriately. */
    @Redirect(
        method = "shoot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectile(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Ljava/util/function/Consumer;)Lnet/minecraft/world/entity/projectile/Projectile;",
            ordinal = 0
        )
    )
    private Projectile inject$shootAll$1(
        Projectile Proj,
        ServerLevel SW,
        ItemStack Stack,
        Consumer<Projectile> OnBeforeSpawn,
        @Share("HomingTarget") LocalRef<LivingEntity> HomingTarget,
        @Share("Hypershot") LocalRef<Boolean> IsHypershot,
        @Share("DisallowItemPickup") LocalRef<Boolean> DisallowItemPickup
    ) {
        // Apply settings computed above to the projectiles.
        var PPE = (ProjectileEntityAccessor)Proj;
        PPE.SetHomingTarget(HomingTarget.get());
        if (IsHypershot.get()) PPE.MakeHypershotProjectile();
        if (DisallowItemPickup.get() && Proj instanceof AbstractArrow E)
            E.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;

        // And call the original method.
        return Projectile.spawnProjectile(Proj, SW, Stack, OnBeforeSpawn);
    }
}
