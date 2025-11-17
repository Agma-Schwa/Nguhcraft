package org.nguh.nguhcraft.mixin.protect;

import com.mojang.serialization.Codec;
import kotlin.Unit;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.LockCode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.item.KeyItem;
import org.nguh.nguhcraft.item.LockableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nguh.nguhcraft.item.LockableBlockEntityKt.CheckCanOpen;
import static org.nguh.nguhcraft.item.LockableBlockEntityKt.DeserialiseLock;
import static org.nguh.nguhcraft.server.ExtensionsKt.CreateUpdateBlockEntityUpdatePacket;

@Mixin(BaseContainerBlockEntity.class)
public abstract class BaseContainerBlockEntityMixin extends BlockEntity implements LockableBlockEntity {
    public BaseContainerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow private LockCode lockKey;
    @Shadow public abstract Component getDisplayName();

    @Unique private static final String TAG_NGUHCRAFT_LOCK = "NguhcraftLock";
    @Unique @Nullable private String NguhcraftLock;

    @Override public @Nullable String Nguhcraft$GetLock() { return NguhcraftLock; }
    @Override public void Nguhcraft$SetLockInternal(@Nullable String NewLock) { NguhcraftLock = NewLock; }
    @Override public @NotNull Component Nguhcraft$GetName() { return getDisplayName(); }

    /**
     * Disallow legacy locks.
     * <p>
     * This is used to implement the member function of the same name (which we
     * also replace); BeaconBlockEntity also uses it for some ungodly reason, so
     * we replace it there as well.
     * @author Sirraide
     * @reason See above.
     */
    @Overwrite
    public static boolean canUnlock(Player PE, LockCode L, Component ContainerName) {
        throw new IllegalStateException(
            "Nguhcraft: Function 'checkUnlocked' should have been replaced (container: '%s')".formatted(ContainerName.getString())
        );
    }

    /**
     * Redirect lock check to use our custom locks.
     * @author Sirraide
     * @reason See above.
     */
    @Overwrite
    public boolean canOpen(Player PE) {
        return CheckCanOpen(this, PE, PE.getMainHandItem());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    void inject$readData(ValueInput RV, CallbackInfo CI) {
        NguhcraftLock = DeserialiseLock(RV, TAG_NGUHCRAFT_LOCK);
        lockKey = LockCode.NO_LOCK; // Delete legacy lock.
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    void inject$writeData(ValueOutput WV, CallbackInfo CI) {
        WV.storeNullable(TAG_NGUHCRAFT_LOCK, Codec.STRING, NguhcraftLock);
    }

    @Inject(method = "applyImplicitComponents", at = @At("TAIL"))
    void inject$readComponents(DataComponentGetter CA, CallbackInfo CI) {
        NguhcraftLock = CA.get(KeyItem.COMPONENT);
    }

    @Inject(method = "collectImplicitComponents", at = @At("TAIL"))
    void inject$addComponents(DataComponentMap.Builder CB, CallbackInfo CI) {
        if (NguhcraftLock != null) CB.set(KeyItem.COMPONENT, NguhcraftLock);
    }

    @Inject(method = "removeComponentsFromTag", at = @At("TAIL"))
    void inject$removeFromCopiedStackData(ValueOutput WV, CallbackInfo CI) {
        WV.discard(TAG_NGUHCRAFT_LOCK);
    }

    /** Send lock in initial chunk data. */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider WL) {
        return CreateUpdateBlockEntityUpdatePacket(Tag -> {
            Tag.storeNullable(TAG_NGUHCRAFT_LOCK, Codec.STRING, NguhcraftLock);
            return Unit.INSTANCE;
        });
    }

    /** Actually send the packet. */
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
