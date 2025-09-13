package org.nguh.nguhcraft.mixin.protect;

import com.mojang.serialization.Codec;
import kotlin.Unit;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.ComponentsAccess;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.ContainerLock;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
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

@Mixin(LockableContainerBlockEntity.class)
public abstract class LockableContainerBlockEntityMixin extends BlockEntity implements LockableBlockEntity {
    public LockableContainerBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Shadow private ContainerLock lock;
    @Shadow public abstract Text getDisplayName();

    @Unique private static final String TAG_NGUHCRAFT_LOCK = "NguhcraftLock";
    @Unique @Nullable private String NguhcraftLock;

    @Override public @Nullable String Nguhcraft$GetLock() { return NguhcraftLock; }
    @Override public void Nguhcraft$SetLockInternal(@Nullable String NewLock) { NguhcraftLock = NewLock; }
    @Override public @NotNull Text Nguhcraft$GetName() { return getDisplayName(); }

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
    public static boolean checkUnlocked(PlayerEntity PE, ContainerLock L, Text ContainerName) {
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
    public boolean checkUnlocked(PlayerEntity PE) {
        return CheckCanOpen(this, PE, PE.getMainHandStack());
    }

    @Inject(method = "readData", at = @At("TAIL"))
    void inject$readData(ReadView RV, CallbackInfo CI) {
        NguhcraftLock = DeserialiseLock(RV, TAG_NGUHCRAFT_LOCK);
        lock = ContainerLock.EMPTY; // Delete legacy lock.
    }

    @Inject(method = "writeData", at = @At("TAIL"))
    void inject$writeData(WriteView WV, CallbackInfo CI) {
        WV.putNullable(TAG_NGUHCRAFT_LOCK, Codec.STRING, NguhcraftLock);
    }

    @Inject(method = "readComponents", at = @At("TAIL"))
    void inject$readComponents(ComponentsAccess CA, CallbackInfo CI) {
        NguhcraftLock = CA.get(KeyItem.COMPONENT);
    }

    @Inject(method = "addComponents", at = @At("TAIL"))
    void inject$addComponents(ComponentMap.Builder CB, CallbackInfo CI) {
        if (NguhcraftLock != null) CB.add(KeyItem.COMPONENT, NguhcraftLock);
    }

    @Inject(method = "removeFromCopiedStackData", at = @At("TAIL"))
    void inject$removeFromCopiedStackData(WriteView WV, CallbackInfo CI) {
        WV.remove(TAG_NGUHCRAFT_LOCK);
    }

    /** Send lock in initial chunk data. */
    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup WL) {
        return CreateUpdateBlockEntityUpdatePacket(Tag -> {
            Tag.putNullable(TAG_NGUHCRAFT_LOCK, Codec.STRING, NguhcraftLock);
            return Unit.INSTANCE;
        });
    }

    /** Actually send the packet. */
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}
