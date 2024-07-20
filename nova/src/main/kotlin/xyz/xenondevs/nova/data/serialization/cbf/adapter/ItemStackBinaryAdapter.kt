package xyz.xenondevs.nova.data.serialization.cbf.adapter

import com.mojang.serialization.Dynamic
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtOps
import net.minecraft.util.datafix.DataFixers
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.item.ItemStack
import xyz.xenondevs.cbf.adapter.ComplexBinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.util.DATA_VERSION
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.unwrap
import java.io.ByteArrayInputStream
import kotlin.reflect.KType
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

internal object ItemStackSerializer {
    
    fun read(id: UByte, reader: ByteReader): ItemStack {
        if (id == 1.toUByte())
            return readLegacy(reader)
        
        val dataVersion = reader.readVarInt()
        var nbt = NbtIo.read(reader.asDataInput())
        nbt = tryFix(nbt, dataVersion, DATA_VERSION)
        
        return ItemStack.parse(REGISTRY_ACCESS, nbt).get()
    }
    
    private fun readLegacy(reader: ByteReader): ItemStack {
        val data = reader.readBytes(reader.readVarInt())
        var nbt = NbtIo.readCompressed(ByteArrayInputStream(data), NbtAccounter.unlimitedHeap())
        nbt = tryFix(nbt, 3700, DATA_VERSION)
        return ItemStack.parse(REGISTRY_ACCESS, nbt).get()
    }
    
    fun write(obj: ItemStack, writer: ByteWriter) {
        writer.writeUnsignedByte(2.toUByte())
        
        writer.writeVarInt(DATA_VERSION)
        val nbt = obj.save(REGISTRY_ACCESS) as CompoundTag
        NbtIo.write(nbt, writer.asDataOutput())
    }
    
    fun tryFix(tag: CompoundTag, fromVersion: Int, toVersion: Int): CompoundTag {
        return DataFixers.getDataFixer().update(
            References.ITEM_STACK,
            Dynamic(NbtOps.INSTANCE, tag),
            fromVersion, toVersion
        ).value as CompoundTag
    }
    
}

internal object BukkitItemStackBinaryAdapter : ComplexBinaryAdapter<BukkitStack> {
    
    override fun read(type: KType, id: UByte, reader: ByteReader): BukkitStack {
        return ItemStackSerializer.read(id, reader).asBukkitMirror()
    }
    
    override fun write(obj: BukkitStack, type: KType, writer: ByteWriter) {
        return ItemStackSerializer.write(obj.unwrap(), writer)
    }
    
    override fun copy(obj: BukkitStack, type: KType): BukkitStack {
        return obj.clone()
    }
    
}

internal object MojangItemStackBinaryAdapter : ComplexBinaryAdapter<MojangStack> {
    
    override fun read(type: KType, id: UByte, reader: ByteReader): ItemStack {
        return ItemStackSerializer.read(id, reader)
    }
    
    override fun write(obj: ItemStack, type: KType, writer: ByteWriter) {
        ItemStackSerializer.write(obj, writer)
    }
    
    override fun copy(obj: ItemStack, type: KType): ItemStack {
        return obj.copy()
    }
    
}