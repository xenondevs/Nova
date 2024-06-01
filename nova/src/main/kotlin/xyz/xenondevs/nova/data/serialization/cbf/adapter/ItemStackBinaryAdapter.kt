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
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.nmsVersion
import kotlin.reflect.KType
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack

internal object ItemStackSerializer {
    
    fun read(id: UByte, reader: ByteReader): ItemStack {
        if (id == 1.toUByte())
            return readLegacy(reader)
        
        val dataVersion = reader.readVarInt()
        var nbt = NbtIo.read(reader.asDataInput())
        nbt = tryFix(nbt, dataVersion, NMSUtils.DATA_VERSION)
        
        return ItemStack.of(nbt)
    }
    
    private fun readLegacy(reader: ByteReader): ItemStack {
        var nbt = NbtIo.readCompressed(reader.asInputStream(), NbtAccounter.unlimitedHeap())
        nbt = tryFix(nbt, 3700, NMSUtils.DATA_VERSION)
        return ItemStack.of(nbt)
    }
    
    fun write(obj: ItemStack, writer: ByteWriter) {
        writer.writeUnsignedByte(2.toUByte())
        
        writer.writeVarInt(NMSUtils.DATA_VERSION)
        val nbt = obj.save(CompoundTag())
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
        return ItemStackSerializer.read(id, reader).bukkitMirror
    }
    
    override fun write(obj: BukkitStack, type: KType, writer: ByteWriter) {
        return ItemStackSerializer.write(obj.nmsVersion, writer)
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