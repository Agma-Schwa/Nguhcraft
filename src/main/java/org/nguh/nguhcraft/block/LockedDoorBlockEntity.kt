package org.nguh.nguhcraft.block;

import com.mojang.serialization.Codec
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentGetter
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.core.HolderLookup.Provider
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
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
    var CustomName: Component? = null

    override fun `Nguhcraft$GetLock`() = Lock
    override fun `Nguhcraft$GetName`() = CustomName ?: DOOR_TEXT

    override fun loadAdditional(RV: ValueInput) {
        super.loadAdditional(RV)
        CustomName = parseCustomNameSafe(RV, "CustomName")
        Lock = DeserialiseLock(RV)
    }

    override fun saveAdditional(WV: ValueOutput) {
        super.saveAdditional(WV)
        WV.storeNullable("CustomName", ComponentSerialization.CODEC, CustomName)
        WV.storeNullable("Lock", Codec.STRING, Lock)
    }

    override fun applyImplicitComponents(CA: DataComponentGetter) {
        super.applyImplicitComponents(CA)
        Lock = CA.get(KeyItem.COMPONENT)
        CustomName = CA.get(DataComponents.CUSTOM_NAME)
    }

    override fun collectImplicitComponents(B: DataComponentMap.Builder) {
        super.collectImplicitComponents(B)
        if (Lock != null) B.set(KeyItem.COMPONENT, Lock)
        if (CustomName != null) B.set(DataComponents.CUSTOM_NAME, CustomName)
    }

    /** Send lock in initial chunk data.  */
    override fun getUpdateTag(WL: Provider): CompoundTag = saveWithoutMetadata(WL)

    /** Actually send the packet.  */
    override fun getUpdatePacket(): Packet<ClientGamePacketListener> {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun removeComponentsFromTag(WV: ValueOutput) {
        WV.discard("lock")
        WV.discard("CustomName")
    }

    override fun `Nguhcraft$SetLockInternal`(NewLock: String?) {
        Lock = NewLock
        level?.let { UpdateBlockState(it) }
    }

    fun UpdateBlockState(W: Level) {
        W.setBlockAndUpdate(worldPosition, W.getBlockState(worldPosition).setValue(LockedDoorBlock.LOCKED, IsLocked()))
    }

    companion object {
        val DOOR_TEXT: Component = Component.translatable("nguhcraft.door") // Separate key so we don’t show ‘Locked Door is locked’.
    }
}
