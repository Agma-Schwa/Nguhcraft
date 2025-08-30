package org.nguh.nguhcraft.block

import com.mojang.serialization.MapCodec
import net.minecraft.block.*
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.server.world.ServerWorld
import net.minecraft.sound.SoundCategory
import net.minecraft.state.StateManager
import net.minecraft.state.property.BooleanProperty
import net.minecraft.util.ActionResult
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraft.world.block.WireOrientation
import net.minecraft.world.event.GameEvent
import net.minecraft.world.explosion.Explosion
import org.nguh.nguhcraft.item.CheckCanOpen
import org.nguh.nguhcraft.item.KeyItem
import java.util.function.BiConsumer

class LockedDoorBlock(S: Settings) : DoorBlock(BlockSetType.IRON, S), BlockEntityProvider {
    init { defaultState = stateManager.defaultState.with(LOCKED, false) }

    override fun appendProperties(B: StateManager.Builder<Block, BlockState>) {
        super.appendProperties(B)
        B.add(LOCKED)
    }

    /** Create a block entity to hold the lock. */
    override fun createBlockEntity(Pos: BlockPos, St: BlockState) = LockedDoorBlockEntity(Pos, St)

    /**
    * Skip default door interactions w/ explosions.
    *
    * We can’t easily call AbstractBlock’s member in here, so just do
    * nothing since this should never explode anyway because of infinite
    * blast resistance.
    */
    override fun onExploded(
        St: BlockState,
        W: ServerWorld,
        Pos: BlockPos,
        E: Explosion,
        SM: BiConsumer<ItemStack, BlockPos>
    ) {}

    /** Keep the door closed even if it receives a redstone signal. */
    override fun getPlacementState(Ctx: ItemPlacementContext) = super.getPlacementState(Ctx)
        ?.with(POWERED, false)
        ?.with(OPEN, false)

    /** This ignores redstone. */
    override fun neighborUpdate(
        state: BlockState?,
        world: World?,
        pos: BlockPos?,
        sourceBlock: Block?,
        sourcePos: WireOrientation?,
        notify: Boolean
    ) {}

    /** It also can’t be opened w/o an item if locked. */
    override fun onUse(
        OldState: BlockState,
        W: World,
        Pos: BlockPos,
        PE: PlayerEntity,
        Hit: BlockHitResult
    ): ActionResult {
        val BE = KeyItem.GetLockableEntity(W, Pos)

        // Somehow, this is not a locked door. Ignore.
        if (BE !is LockedDoorBlockEntity) return ActionResult.PASS

        // Check if this block can be opened.
        if (!BE.CheckCanOpen(PE, PE.mainHandStack)) return ActionResult.SUCCESS

        // Actually open the door.
        //
        // Ugly code duplication from onUse(), but the canOpenByHand() check
        // is really messing w/ how these work here, so we have no choice but
        // to duplicate the section we actually want to use here.
        val St = OldState.cycle(OPEN)
        W.setBlockState(Pos, St, NOTIFY_LISTENERS or REDRAW_ON_MAIN_THREAD)
        W.playSound(
            PE,
            Pos,
            if (isOpen(St)) blockSetType.doorOpen() else blockSetType.doorClose(),
            SoundCategory.BLOCKS,
            1.0f,
            W.random.nextFloat() * 0.1f + 0.9f
        )

        W.emitGameEvent(PE, if (isOpen(St)) GameEvent.BLOCK_OPEN else GameEvent.BLOCK_CLOSE, Pos)
        return ActionResult.SUCCESS
    }

    override fun getCodec() = CODEC
    companion object {
        val CODEC: MapCodec<LockedDoorBlock> = createCodec(::LockedDoorBlock)
        val LOCKED: BooleanProperty = BooleanProperty.of("nguhcraft_locked") // Property to render a locked door.
    }
}