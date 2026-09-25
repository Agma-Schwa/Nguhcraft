package org.nguh.nguhcraft.mixin.client.render;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.feature.FeatureFrameContext;
import net.minecraft.client.renderer.feature.FlameFeatureRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.entity.EntityTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(FlameFeatureRenderer.class)
public abstract class FlameFeatureRendererMixin {
    @Unique private static final SpriteId SOUL_FIRE_0 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("soul_fire_0");
    @Unique private static final SpriteId SOUL_FIRE_1 = Sheets.BLOCKS_MAPPER.defaultNamespaceApply("soul_fire_1");

    /** Render blue fire for tridents instead. */
    @WrapOperation(
        method = "buildGroup",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/FlameFeatureRenderer;prepare(Lnet/minecraft/client/renderer/feature/FlameFeatureRenderer$Submit;Lcom/mojang/blaze3d/vertex/VertexConsumer;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V"
        )
    )
    private void inject$prepare(
        FlameFeatureRenderer Self,
        FlameFeatureRenderer.Submit S,
        VertexConsumer VC,
        TextureAtlasSprite S1,
        TextureAtlasSprite S2,
        Operation<Void> Op,
        FeatureFrameContext Ctx,
        List<FlameFeatureRenderer.Submit> Submits
    ) {
        if (S.entityRenderState().entityType == EntityTypes.TRIDENT) {
            S1 = Ctx.atlasManager().get(SOUL_FIRE_0);
            S2 = Ctx.atlasManager().get(SOUL_FIRE_1);
        }

        Op.call(Self, S, VC, S1, S2);
    }
}
