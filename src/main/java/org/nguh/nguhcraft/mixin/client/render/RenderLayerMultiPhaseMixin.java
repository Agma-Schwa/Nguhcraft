package org.nguh.nguhcraft.mixin.client.render;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.minecraft.client.gl.DynamicUniforms;
import net.minecraft.client.render.RenderLayer;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.nguh.nguhcraft.client.render.RenderLayerMultiPhaseShaderColourAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(RenderLayer.MultiPhase.class)
public abstract class RenderLayerMultiPhaseMixin implements RenderLayerMultiPhaseShaderColourAccessor {
    @Unique private Vector4fc ColourModulator = new Vector4f(1.0f);
    @Override public void Nguhcraft$SetShaderColour(@NotNull Vector4fc Colour) {
        this.ColourModulator = Colour;
    }

    /** * Allow customising the shader colour. */
    @Redirect(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gl/DynamicUniforms;write(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;F)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"
        )
    )
    private GpuBufferSlice inject$draw(
        DynamicUniforms Uniforms,
        Matrix4fc MV,
        Vector4fc Colour,
        Vector3fc ModelOffs,
        Matrix4fc MTX,
        float LineWd
    ) {
        return Uniforms.write(MV, ColourModulator, ModelOffs, MTX, LineWd);
    }
}
