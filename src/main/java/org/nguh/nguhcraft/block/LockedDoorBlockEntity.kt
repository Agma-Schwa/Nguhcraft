package org.nguh.nguhcraft.block;

import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.component.ComponentMap
import net.minecraft.component.DataComponentTypes
import net.minecraft.inventory.ContainerLock
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.item.LockItem
import org.nguh.nguhcraft.server.CreateUpdate

class LockedDoorBlockEntity(
    Pos: BlockPos,
    St: BlockState
) : BlockEntity(NguhBlocks.LOCKED_DOOR_BLOCK_ENTITY, Pos, St), LockableBlockEntity {
    // This is a field to prevent a mangling clash w/ getLock().
    @JvmField var Lock: ContainerLock = ContainerLock.EMPTY

    override fun readNbt(Tag: NbtCompound, RL: WrapperLookup) {
        super.readNbt(Tag, RL)

        // I’m done dealing with stupid data fixer nonsense to try
        // and rename this field properly, so we’re doing this the
        // dumb way.
        //
        // This field was previously called 'Lock' and was a string;
        // it is now called 'lock' and is an item predicate; deal
        // with this accordingly.
        //
        // Do not use SetLock() here as that will crash during loading.
        val OldStyleLock = Tag.get("Lock")
        Lock = if (OldStyleLock != null) LockItem.CreateContainerLock(OldStyleLock.asString())
        else ContainerLock.fromNbt(Tag, RL)
    }

    override fun writeNbt(Tag: NbtCompound, RL: WrapperLookup) {
        super.writeNbt(Tag, RL)
        Lock.writeNbt(Tag, RL)
    }

    override fun readComponents(CA: ComponentsAccess) {
        super.readComponents(CA)
        Lock = CA.getOrDefault(DataComponentTypes.LOCK, ContainerLock.EMPTY)
    }

    override fun addComponents(B: ComponentMap.Builder) {
        super.addComponents(B)
        if (Lock != ContainerLock.EMPTY) B.add(DataComponentTypes.LOCK, Lock)
    }

    /** Send lock in initial chunk data.  */
    override fun toInitialChunkDataNbt(WL: WrapperLookup) = CreateUpdate {
        Lock.writeNbt(it, WL)
    }

    /** Actually send the packet.  */
    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> {
        return BlockEntityUpdateS2CPacket.create(this)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun removeFromCopiedStackNbt(nbt: NbtCompound) {
        nbt.remove("lock")
    }

    override fun getLock() = Lock
    override fun SetLockInternal(NewLock: ContainerLock) {
        Lock = NewLock
        world?.let { UpdateBlockState(it) }
    }

    fun UpdateBlockState(W: World) {
        W.setBlockState(pos, W.getBlockState(pos).with(LockedDoorBlock.LOCKED, Lock != ContainerLock.EMPTY))
    }
}
