package org.nguh.nguhcraft.mixin.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ChestRenderer;
import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.accessors.ChestBlockEntityAccessor;
import org.nguh.nguhcraft.accessors.ChestRenderStateAccessor;
import org.nguh.nguhcraft.block.ChestTextureOverride;
import org.nguh.nguhcraft.item.KeyItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for the new chest rendering system. It checks how the chest should be rendered during extractRenderState and
 * stashes that information using ChestRenderStateAccessor until the submit stage where it picks the appropriate sprite
 * to render.
 */
@Mixin(ChestRenderer.class)
public abstract class ChestRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void inject$computeOverrideSprite(
        BlockEntity BE,
        ChestRenderState State,
        float PartialTicks,
        Vec3 CameraPosition,
        ModelFeatureRenderer.@Nullable CrumblingOverlay BreakProgress,
        CallbackInfo CI
    ) {
        SpriteId Override = null;
        if (BE instanceof ChestBlockEntity CBE) {
            var CV = ((ChestBlockEntityAccessor) CBE).Nguhcraft$GetChestVariant();
            var Locked = KeyItem.IsChestLocked(BE);
            if (CV != null || Locked) Override = ChestTextureOverride.GetTexture(
                BE.getBlockState().getBlock(),
                CV,
                State.type,
                Locked
            );
        }

        ((ChestRenderStateAccessor) State).Nguhcraft$SetOverrideSprite(Override);
    }

    @Redirect(
        method = "submit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Sheets;chooseSprite(Lnet/minecraft/client/renderer/blockentity/state/ChestRenderState$ChestMaterialType;Lnet/minecraft/world/level/block/state/properties/ChestType;)Lnet/minecraft/client/resources/model/sprite/SpriteId;"
        )
    )
    private SpriteId redirect$chooseSprite(
        ChestRenderState.ChestMaterialType MaterialType,
        ChestType Type,
        ChestRenderState State,
        PoseStack PS,
        SubmitNodeCollector SNC,
        CameraRenderState Camera
    ) {
        SpriteId Override = ((ChestRenderStateAccessor) State).Nguhcraft$GetOverrideSprite();
        return Override != null ? Override : Sheets.chooseSprite(MaterialType, Type);
    }
}
