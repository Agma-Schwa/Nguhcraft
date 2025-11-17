package org.nguh.nguhcraft.mixin.datafixer;

import com.google.common.escape.Escaper;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.LockComponentPredicateFix;
import org.nguh.nguhcraft.item.KeyItem;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LockComponentPredicateFix.class)
public abstract class LockComponentPredicateFixMixin {
    @Shadow @Final public static Escaper ESCAPER;

    /**
     * Instead of converting the 'lock' string to a predicate that tests
     * for the custom name of an item, rewrite it to instead check for our
     * custom key component and for whether it’s actually a key.
     *
     * @author Sirraide
     * @reason Complete replacement
     */
    @Overwrite
    public static Dynamic<?> fixLock(Dynamic<?> D) {
        var Key = D.asString().result();
        if (Key.isEmpty()) return D.emptyMap();
        return D.emptyMap()
            .set("items", D.createString(KeyItem.ID.toString()))
            .set("components", D.emptyMap().set(KeyItem.COMPONENT_ID.toString(), D));
    }
}
