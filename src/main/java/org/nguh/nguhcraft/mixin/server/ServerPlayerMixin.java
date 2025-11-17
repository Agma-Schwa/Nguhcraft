package org.nguh.nguhcraft.mixin.server;

import com.mojang.authlib.GameProfile;
import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.server.PlayerData;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin extends Player implements PlayerData.Access {
    public ServerPlayerMixin(Level world, GameProfile profile) {
        super(world, profile);
    }

    @Unique private PlayerData Data = new PlayerData();
    @Unique static private final Logger LOGGER = LogUtils.getLogger();

    @Override
    public Component getDisplayName() {
        if (Data.NguhcraftDisplayName != null) return Data.NguhcraftDisplayName;
        return super.getDisplayName();
    }

    @Override
    public @NotNull PlayerData Nguhcraft$GetPlayerData() {
        return Data;
    }

    /**
    * Copy over player data from a to-be-deleted instance.
    * <p>
    * For SOME UNGODLY REASON, Minecraft creates a NEW PLAYER ENTITY when the player
    * dies and basically does the equivalent of reconnecting the player, but on the
    * same network connexion etc.
    * <p>
    * This function copies our custom data over to the new player entity.
    */
    @Inject(
        method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V",
        at = @At("TAIL")
    )
    private void inject$copyFrom(ServerPlayer Old, boolean Alive, CallbackInfo CI) {
        Data = ((ServerPlayerMixin)(Object)Old).Data;
    }

    /** Save Nbt data to the player file. */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void inject$saveData(ValueOutput WV, CallbackInfo CI) {
        Data.Save(WV);
    }

    /** Read Nbt data from the player file. */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void inject$saveData(ValueInput RV, CallbackInfo CI) {
        PlayerData.Load(RV).ifPresent(D -> Data = D);
    }

    /**
     * Make sure we *never* send signed messages under any circumstances.
     * <p>
     * @author Sirraide
     * @reason Last line of defence. This should *never* be called in the first place. If
     *         it ever is, that means we forgot to override something else.
     */
     @Overwrite
     public void sendChatMessage(@NotNull OutgoingChatMessage message, boolean filterMaskEnabled, ChatType.Bound params) {
         LOGGER.error("Refusing to send signed message to '{}': {}", getScoreboardName(), message.content());
     }
}
