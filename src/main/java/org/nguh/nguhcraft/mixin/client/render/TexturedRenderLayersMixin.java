package org.nguh.nguhcraft.mixin.client.render;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.enums.ChestType;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.util.SpriteIdentifier;
import org.nguh.nguhcraft.accessors.ChestBlockEntityAccessor;
import org.nguh.nguhcraft.block.ChestTextureOverride;
import org.nguh.nguhcraft.item.KeyItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TexturedRenderLayers.class)
public abstract class TexturedRenderLayersMixin {
    /** Render a lock on locked chests and handle chest variants. */
    @Inject(
        method = "getChestTextureId(Lnet/minecraft/block/entity/BlockEntity;Lnet/minecraft/block/enums/ChestType;Z)Lnet/minecraft/client/util/SpriteIdentifier;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$getChestTextureId(
        BlockEntity BE,
        ChestType Ty,
        boolean Christmas,
        CallbackInfoReturnable<SpriteIdentifier> CIR
    ) {
        if (BE instanceof ChestBlockEntity CBE) {
            var CV = ((ChestBlockEntityAccessor)CBE).Nguhcraft$GetChestVariant();
            var Locked = KeyItem.IsChestLocked(BE);
            CIR.setReturnValue(ChestTextureOverride.GetTexture(CV, Ty, Locked));
        }
    }
}
