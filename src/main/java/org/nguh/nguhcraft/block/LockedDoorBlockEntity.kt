package org.nguh.nguhcraft.block;

import com.mojang.serialization.Codec
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.component.ComponentMap
import net.minecraft.component.ComponentsAccess
import net.minecraft.component.DataComponentTypes
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket
import net.minecraft.registry.RegistryWrapper.WrapperLookup
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.text.Text
import net.minecraft.text.TextCodecs
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.item.DeserialiseLock
import org.nguh.nguhcraft.item.IsLocked
import org.nguh.nguhcraft.item.KeyItem
import org.nguh.nguhcraft.item.LockableBlockEntity

class LockedDoorBlockEntity(
    Pos: BlockPos,
    St: BlockState
) : BlockEntity(NguhBlocks.LOCKED_DOOR_BLOCK_ENTITY, Pos, St), LockableBlockEntity {
    // This is a field to prevent a mangling clash w/ getLock().
    @JvmField var Lock: String? = null
    var CustomName: Text? = null

    override fun `Nguhcraft$GetLock`() = Lock
    override fun `Nguhcraft$GetName`() = CustomName ?: DOOR_TEXT

    override fun readData(RV: ReadView) {
        super.readData(RV)
        CustomName = tryParseCustomName(RV, "CustomName")
        Lock = DeserialiseLock(RV)
    }

    override fun writeData(WV: WriteView) {
        super.writeData(WV)
        WV.putNullable("CustomName", TextCodecs.CODEC, CustomName)
        WV.putNullable("Lock", Codec.STRING, Lock)
    }

    override fun readComponents(CA: ComponentsAccess) {
        super.readComponents(CA)
        Lock = CA.getOrDefault(KeyItem.COMPONENT, null)
        CustomName = CA.getOrDefault(DataComponentTypes.CUSTOM_NAME, null)
    }

    override fun addComponents(B: ComponentMap.Builder) {
        super.addComponents(B)
        if (Lock != null) B.add(KeyItem.COMPONENT, Lock)
        if (CustomName != null) B.add(DataComponentTypes.CUSTOM_NAME, CustomName)
    }

    /** Send lock in initial chunk data.  */
    override fun toInitialChunkDataNbt(WL: WrapperLookup): NbtCompound = createNbt(WL)

    /** Actually send the packet.  */
    override fun toUpdatePacket(): Packet<ClientPlayPacketListener> {
        return BlockEntityUpdateS2CPacket.create(this)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun removeFromCopiedStackData(WV: WriteView) {
        WV.remove("lock")
        WV.remove("CustomName")
    }

    override fun `Nguhcraft$SetLockInternal`(NewLock: String?) {
        Lock = NewLock
        world?.let { UpdateBlockState(it) }
    }

    fun UpdateBlockState(W: World) {
        W.setBlockState(pos, W.getBlockState(pos).with(LockedDoorBlock.LOCKED, IsLocked()))
    }

    companion object {
        val DOOR_TEXT: Text = Text.translatable("nguhcraft.door") // Separate key so we don’t show ‘Locked Door is locked’.
    }
}
