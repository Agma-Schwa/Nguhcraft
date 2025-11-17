package org.nguh.nguhcraft.mixin.client.render;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.Material;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @SuppressWarnings("deprecation") @Unique private static final Material SOUL_FIRE_0 =
        new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/soul_fire_0"));

    @SuppressWarnings("deprecation") @Unique private static final Material SOUL_FIRE_1 =
        new Material(TextureAtlas.LOCATION_BLOCKS, ResourceLocation.withDefaultNamespace("block/soul_fire_1"));

    /** Hack to check if we’re rendering a trident. */
    @Unique private boolean RenderingTrident = false;

    /** Remember that we’re rendering a trident. */
    @Inject(
        method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
        at = @At("HEAD")
    )
    private  <E extends Entity, S extends EntityRenderState> void inject$render$0(
        E Entity,
        double x,
        double y,
        double z,
        float tickDelta,
        PoseStack matrices,
        MultiBufferSource vertexConsumers,
        int light,
        EntityRenderer<? super E, S> renderer,
        CallbackInfo ci
    ) { RenderingTrident = Entity instanceof ThrownTrident; }

    /** And clear the flag when we’re done. */
    @Inject(
        method = "render(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/client/renderer/entity/EntityRenderer;)V",
        at = @At("TAIL")
    )
    private <E extends Entity, S extends EntityRenderState> void inject$render$1(
        E Entity,
        double x,
        double y,
        double z,
        float tickDelta,
        PoseStack matrices,
        MultiBufferSource vertexConsumers,
        int light,
        EntityRenderer<? super E, S> renderer,
        CallbackInfo ci
    ) { RenderingTrident = false; }

    /** Render blue fire for tridents instead. */
    @Inject(
        method = "renderFlame",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V",
            ordinal = 0
        )
    )
    private void inject$renderFire(
        PoseStack MS,
        MultiBufferSource VC,
        EntityRenderState E,
        Quaternionf Rot,
        CallbackInfo CI,
        @Local(ordinal = 0) LocalRef<TextureAtlasSprite> S1,
        @Local(ordinal = 1) LocalRef<TextureAtlasSprite> S2
    ) {
        if (RenderingTrident) {
            S1.set(SOUL_FIRE_0.sprite());
            S2.set(SOUL_FIRE_1.sprite());
        }
    }
}
