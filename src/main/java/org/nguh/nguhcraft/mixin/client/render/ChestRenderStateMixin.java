package org.nguh.nguhcraft.mixin.client.render;

import net.minecraft.client.renderer.blockentity.state.ChestRenderState;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.accessors.ChestRenderStateAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Adds the ability to store the sprite override in ChestRenderState. */
@Mixin(ChestRenderState.class)
public abstract class ChestRenderStateMixin implements ChestRenderStateAccessor {
    @Nullable @Unique private SpriteId OverrideSprite = null;

    @Override public @Nullable SpriteId Nguhcraft$GetOverrideSprite() { return OverrideSprite; }
    @Override public void Nguhcraft$SetOverrideSprite(@Nullable SpriteId sprite) { OverrideSprite = sprite; }
}
