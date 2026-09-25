package org.nguh.nguhcraft.mixin.client.render;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import net.minecraft.client.renderer.DynamicGpuData;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.nguh.nguhcraft.client.render.RenderLayerMultiPhaseShaderColourAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderType.class)
public abstract class RenderTypeMixin implements RenderLayerMultiPhaseShaderColourAccessor {
    @Unique private static final Vector3fc NO_OFFSET = new Vector3f();
    @Unique private Vector4fc ColourModulator = new Vector4f(1.0f);
    @Override public void Nguhcraft$SetShaderColour(@NotNull Vector4fc Colour) {
        this.ColourModulator = Colour;
    }

    /** * Allow customising the shader colour. */
    @Redirect(
        method = "writeDynamicTransforms",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"
        )
    )
    private GpuBufferSlice inject$writeDynamicTransforms(
        DynamicGpuData Uniforms,
        Matrix4f MV,
        Matrix4f MTX
    ) {
        return Uniforms.writeTransform(new DynamicGpuData.Transform(MV, ColourModulator, NO_OFFSET, MTX));
    }
}
