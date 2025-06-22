package xyz.xenondevs.nova.serialization.cbf

import com.mojang.serialization.Dynamic
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.util.datafix.DataFixers
import net.minecraft.util.datafix.fixes.References
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.nova.util.DATA_VERSION
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.unwrap
import java.io.ByteArrayInputStream
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

// 0: null, 1: legacy format, 2: empty item stack, 3: non-empty item stack
internal object ItemStackSerializer {
    
    fun read(id: UByte, reader: ByteReader): MojangStack? {
        return when (id) {
            0.toUByte() -> null
            1.toUByte() -> readV1(reader)
            2.toUByte() -> MojangStack.EMPTY
            3.toUByte() -> readV2(reader)
            else -> throw UnsupportedOperationException()
        }
    }
    
    private fun readV1(reader: ByteReader): MojangStack? {
        val data = reader.readBytes(reader.readVarInt())
        var nbt = NbtIo.readCompressed(ByteArrayInputStream(data), NbtAccounter.unlimitedHeap())
        nbt = tryFix(nbt, 3700, DATA_VERSION)
        return MojangStack.CODEC.parse(REGISTRY_ACCESS.createSerializationContext(NbtOps.INSTANCE), nbt).resultOrPartial().get()
    }
    
    private fun readV2(reader: ByteReader): MojangStack? {
        val dataVersion = reader.readVarInt()
        var nbt = NbtIo.read(reader.asDataInput())
        nbt = tryFix(nbt, dataVersion, DATA_VERSION)
        
        return MojangStack.CODEC.parse(REGISTRY_ACCESS.createSerializationContext(NbtOps.INSTANCE), nbt).resultOrPartial().get()
    }
    
    fun write(obj: MojangStack?, writer: ByteWriter) {
        if (obj == null) {
            writer.writeUnsignedByte(0U)
        } else if (obj.isEmpty) {
            writer.writeUnsignedByte(2U)
        } else {
            writer.writeUnsignedByte(3U)
            
            writer.writeVarInt(DATA_VERSION)
            val nbt = MojangStack.CODEC.encodeStart(
                REGISTRY_ACCESS.createSerializationContext(NbtOps.INSTANCE),
                obj,
            ).resultOrPartial().get() as CompoundTag
            NbtIo.write(nbt, writer.asDataOutput())
        }
    }
    
    fun tryFix(tag: CompoundTag, fromVersion: Int, toVersion: Int): CompoundTag {
        return DataFixers.getDataFixer().update(
            References.ITEM_STACK,
            Dynamic(NbtOps.INSTANCE, tag),
            fromVersion, toVersion
        ).value as CompoundTag
    }
    
}

internal object BukkitItemStackBinarySerializer : BinarySerializer<BukkitStack> {
    
    override fun read(reader: ByteReader): BukkitStack? {
        val id = reader.readUnsignedByte()
        return ItemStackSerializer.read(id, reader)?.asBukkitMirror()
    }
    
    override fun write(obj: BukkitStack?, writer: ByteWriter) {
        return ItemStackSerializer.write(obj?.unwrap(), writer)
    }
    
    override fun copy(obj: BukkitStack?): BukkitStack? {
        return obj?.clone()
    }
    
}

internal object MojangItemStackBinarySerializer : BinarySerializer<MojangStack> {
    
    override fun read(reader: ByteReader): MojangStack? {
        val id = reader.readUnsignedByte()
        return ItemStackSerializer.read(id, reader)
    }
    
    override fun write(obj: MojangStack?, writer: ByteWriter) {
        ItemStackSerializer.write(obj, writer)
    }
    
    override fun copy(obj: MojangStack?): MojangStack? {
        return obj?.copy()
    }
    
}