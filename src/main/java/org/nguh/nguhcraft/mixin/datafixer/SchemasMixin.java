package org.nguh.nguhcraft.mixin.datafixer;

import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.DataFixers;
import org.nguh.nguhcraft.datafix.KeyLockItemComponentisationFix;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.BiFunction;

@Mixin(DataFixers.class)
public abstract class SchemasMixin {
    /**
    * There are over 200 calls to the function we’re redirecting
    * here, and I don’t trust Mojang to not insert extra calls
    * earlier on, so we are injecting into certain schemas, *the
    * dumb way*.
    */
    @Redirect(
        method = "addFixers",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/datafixers/DataFixerBuilder;addSchema(ILjava/util/function/BiFunction;)Lcom/mojang/datafixers/schemas/Schema;",
            remap = false
        )
    )
    private static Schema inject$build(
        DataFixerBuilder Builder,
        int Version,
        BiFunction<Integer, Schema, Schema> Factory
    ) {
        var Schema = Builder.addSchema(Version, Factory);
        switch (Version) {
            case 4068:
                Builder.addFixer(new KeyLockItemComponentisationFix(Schema));
                break;
        }
        return Schema;
    }
}
