package org.nguh.nguhcraft.mixin.protect.server;

import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import org.nguh.nguhcraft.server.accessors.PlayerInteractEntityC2SPacketAccessor;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerboundInteractPacket.class)
public abstract class ServerboundInteractPacketMixin implements PlayerInteractEntityC2SPacketAccessor {
    @Shadow @Final private ServerboundInteractPacket.Action action;

    /**
    * Fuck you whoever named this packet for calling this an ‘interact’
    * packet EVEN THOUGH IT ALSO HANDLES ATTACKING YOU MORON, and fuck
    * you Minecraft for making this damnable information so hard to access
    * that it needs 4 access wideners and for making the handler an
    * anonymous local class.
    */
    public boolean IsAttack() {
        return action.getType() == ServerboundInteractPacket.ActionType.ATTACK;
    }
}
