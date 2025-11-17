package org.nguh.nguhcraft.block

import com.mojang.serialization.MapCodec
import net.minecraft.world.level.block.*
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.ItemStack
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundSource
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.redstone.Orientation
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.Explosion
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockSetType
import org.nguh.nguhcraft.item.CheckCanOpen
import org.nguh.nguhcraft.item.KeyItem
import java.util.function.BiConsumer

class LockedDoorBlock(S: Properties) : DoorBlock(BlockSetType.IRON, S), EntityBlock {
    init { registerDefaultState(stateDefinition.any().setValue(LOCKED, false)) }

    override fun createBlockStateDefinition(B: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(B)
        B.add(LOCKED)
    }

    /** Create a block entity to hold the lock. */
    override fun newBlockEntity(Pos: BlockPos, St: BlockState) = LockedDoorBlockEntity(Pos, St)

    /**
    * Skip default door interactions w/ explosions.
    *
    * We can’t easily call AbstractBlock’s member in here, so just do
    * nothing since this should never explode anyway because of infinite
    * blast resistance.
    */
    override fun onExplosionHit(
        St: BlockState,
        W: ServerLevel,
        Pos: BlockPos,
        E: Explosion,
        SM: BiConsumer<ItemStack, BlockPos>
    ) {}

    /** Keep the door closed even if it receives a redstone signal. */
    override fun getStateForPlacement(Ctx: BlockPlaceContext) = super.getStateForPlacement(Ctx)
        ?.setValue(POWERED, false)
        ?.setValue(OPEN, false)

    /** This ignores redstone. */
    override fun neighborChanged(
        state: BlockState,
        world: Level,
        pos: BlockPos,
        sourceBlock: Block,
        sourcePos: Orientation?,
        notify: Boolean
    ) {}

    /** It also can’t be opened w/o an item if locked. */
    override fun useWithoutItem(
        OldState: BlockState,
        W: Level,
        Pos: BlockPos,
        PE: Player,
        Hit: BlockHitResult
    ): InteractionResult {
        val BE = KeyItem.GetLockableEntity(W, Pos)

        // Somehow, this is not a locked door. Ignore.
        if (BE !is LockedDoorBlockEntity) return InteractionResult.PASS

        // Check if this block can be opened.
        if (!BE.CheckCanOpen(PE, PE.mainHandItem)) return InteractionResult.SUCCESS

        // Actually open the door.
        //
        // Ugly code duplication from onUse(), but the canOpenByHand() check
        // is really messing w/ how these work here, so we have no choice but
        // to duplicate the section we actually want to use here.
        val St = OldState.cycle(OPEN)
        W.setBlock(Pos, St, UPDATE_CLIENTS or UPDATE_IMMEDIATE)
        W.playSound(
            PE,
            Pos,
            if (isOpen(St)) type().doorOpen() else type().doorClose(),
            SoundSource.BLOCKS,
            1.0f,
            W.random.nextFloat() * 0.1f + 0.9f
        )

        W.gameEvent(PE, if (isOpen(St)) GameEvent.BLOCK_OPEN else GameEvent.BLOCK_CLOSE, Pos)
        return InteractionResult.SUCCESS
    }

    override fun codec() = CODEC
    companion object {
        val CODEC: MapCodec<LockedDoorBlock> = simpleCodec(::LockedDoorBlock)
        val LOCKED: BooleanProperty = BooleanProperty.create("nguhcraft_locked") // Property to render a locked door.
    }
}