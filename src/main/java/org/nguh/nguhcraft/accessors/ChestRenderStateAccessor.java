package org.nguh.nguhcraft.accessors;

import net.minecraft.client.resources.model.sprite.SpriteId;
import org.jetbrains.annotations.Nullable;

public interface ChestRenderStateAccessor {
    @Nullable SpriteId Nguhcraft$GetOverrideSprite();
    void Nguhcraft$SetOverrideSprite(@Nullable SpriteId sprite);
}
