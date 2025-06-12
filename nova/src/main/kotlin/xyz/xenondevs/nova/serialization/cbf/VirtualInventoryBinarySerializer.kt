package xyz.xenondevs.nova.serialization.cbf

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.VersionedBinarySerializer
import xyz.xenondevs.invui.inventory.VirtualInventory
import xyz.xenondevs.nova.util.DATA_VERSION
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.ceilDiv
import xyz.xenondevs.nova.util.item.isNullOrEmpty
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.unwrap
import java.util.*
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

internal object VirtualInventoryBinarySerializer : VersionedBinarySerializer<VirtualInventory>(2U) {
    
    override fun readVersioned(version: UByte, reader: ByteReader): VirtualInventory {
        return when (version) {
            1.toUByte() -> readV1(reader)
            2.toUByte() -> readV2(reader)
            else -> throw UnsupportedOperationException()
        }
    }
    
    private fun readV2(reader: ByteReader): VirtualInventory {
        val dataInput = reader.asDataInput()
        
        val dataVersion = reader.readVarInt()
        val uuid = reader.readUUID()
        val size = reader.readVarInt()
        
        val itemsMask = BitSet.valueOf(reader.readBytes(size.ceilDiv(8)))
        val items: Array<BukkitStack?> = Array(size) {
            if (itemsMask[it]) {
                val tag = ItemStackSerializer.tryFix(
                    NbtIo.read(dataInput),
                    dataVersion, DATA_VERSION
                )
                MojangStack.parse(REGISTRY_ACCESS, tag).get().asBukkitMirror().takeUnlessEmpty()
            } else null
        }
        
        val maxStackSizes: IntArray
        if (reader.readBoolean()) {
            maxStackSizes = IntArray(size) { reader.readVarInt() }
        } else {
            maxStackSizes = IntArray(size) { 64 }
        }
        
        return VirtualInventory(uuid, size, items, maxStackSizes)
    }
    
    private fun readV1(reader: ByteReader): VirtualInventory {
        val data = ByteArray(reader.readVarInt())
        reader.readBytes(data)
        return VirtualInventory.deserialize(data)
    }
    
    override fun writeVersioned(obj: VirtualInventory, writer: ByteWriter) {
        val dataOutput = writer.asDataOutput()
        
        val uuid = obj.uuid
        val size = obj.size
        val items = obj.unsafeItems
        val maxStackSizes = obj.maxStackSizes
        
        writer.writeVarInt(DATA_VERSION)
        writer.writeUUID(uuid)
        writer.writeVarInt(size)
        
        val itemsMask = BitSet(size)
        for ((slot, itemStack) in items.withIndex()) {
            if (!itemStack.isNullOrEmpty())
                itemsMask.set(slot)
        }
        writer.writeBytes(itemsMask.toByteArray().copyOf(size.ceilDiv(8)))
        
        for (itemStack in obj.items) {
            if (itemStack.isNullOrEmpty())
                continue
            
            val nmsStack = itemStack.unwrap()
            val nbt = nmsStack.save(REGISTRY_ACCESS) as CompoundTag
            NbtIo.write(nbt, dataOutput)
        }
        
        // write stack sizes if custom
        if (maxStackSizes.any { it != 64 }) {
            writer.writeBoolean(true)
            for (stackSize in obj.maxStackSizes) {
                writer.writeVarInt(stackSize)
            }
        } else {
            writer.writeBoolean(false)
        }
    }
    
    override fun copyNonNull(obj: VirtualInventory): VirtualInventory {
        return VirtualInventory(obj)
    }
    
}