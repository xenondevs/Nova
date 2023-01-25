@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf

import de.studiocode.invui.virtualinventory.VirtualInventory
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.reflection.rawType
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.LegacyCompound.CompoundBinaryAdapterLegacy
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.adapter.*
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.instancecreator.EnumMapInstanceCreatorLegacy
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.util.data.toByteArray
import java.awt.Color
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

interface BinaryAdapterLegacy<T> {
    
    fun write(obj: T, buf: ByteBuf)
    
    fun read(type: Type, buf: ByteBuf): T
    
}

interface InstanceCreatorLegacy<T> {
    
    fun createInstance(type: Type): T
    
}

@Suppress("UNCHECKED_CAST")
object CBFLegacy {
    
    private val binaryAdapters = HashMap<KClass<*>, BinaryAdapterLegacy<*>>()
    private val binaryHierarchyAdapters = HashMap<KClass<*>, BinaryAdapterLegacy<*>>()
    
    private val instanceCreators = HashMap<KClass<*>, InstanceCreatorLegacy<*>>()
    
    init {
        // default binary adapters
        registerBinaryAdapter(Boolean::class, BooleanBinaryAdapterLegacy)
        registerBinaryAdapter(BooleanArray::class, BooleanArrayBinaryAdapterLegacy)
        registerBinaryAdapter(Byte::class, ByteBinaryAdapterLegacy)
        registerBinaryAdapter(ByteArray::class, ByteArrayBinaryAdapterLegacy)
        registerBinaryAdapter(Short::class, ShortBinaryAdapterLegacy)
        registerBinaryAdapter(ShortArray::class, ShortArrayBinaryAdapterLegacy)
        registerBinaryAdapter(Int::class, IntBinaryAdapterLegacy)
        registerBinaryAdapter(IntArray::class, IntArrayBinaryAdapterLegacy)
        registerBinaryAdapter(Long::class, LongBinaryAdapterLegacy)
        registerBinaryAdapter(LongArray::class, LongArrayBinaryAdapterLegacy)
        registerBinaryAdapter(Float::class, FloatBinaryAdapterLegacy)
        registerBinaryAdapter(FloatArray::class, FloatArrayBinaryAdapterLegacy)
        registerBinaryAdapter(Double::class, DoubleBinaryAdapterLegacy)
        registerBinaryAdapter(DoubleArray::class, DoubleArrayBinaryAdapterLegacy)
        registerBinaryAdapter(Char::class, CharBinaryAdapterLegacy)
        registerBinaryAdapter(CharArray::class, CharArrayBinaryAdapterLegacy)
        registerBinaryAdapter(String::class, StringBinaryAdapterLegacy)
        registerBinaryAdapter(Array<String>::class, StringArrayBinaryAdapterLegacy)
        registerBinaryAdapter(UUID::class, UUIDBinaryAdapterLegacy)
        registerBinaryAdapter(Pair::class, PairBinaryAdapterLegacy)
        registerBinaryAdapter(Triple::class, TripleBinaryAdapterLegacy)
        registerBinaryAdapter(LegacyCompound::class, CompoundBinaryAdapterLegacy)
        
        // binary adapters for bukkit / nova
        registerBinaryAdapter(Color::class, ColorBinaryAdapterLegacy)
        registerBinaryAdapter(Location::class, LocationBinaryAdapterLegacy)
        registerBinaryAdapter(NamespacedKey::class, NamespacedKeyBinaryAdapterLegacy)
        registerBinaryAdapter(NamespacedId::class, NamespacedIdBinaryAdapterLegacy)
        registerBinaryAdapter(VirtualInventory::class, VirtualInventoryBinaryAdapterLegacy)
        registerBinaryAdapter(NetworkType::class, NetworkTypeBinaryAdapterLegacy)
        registerBinaryAdapter(ItemFilter::class, ItemFilterBinaryAdapterLegacy)
        
        // binary hierarchy adapters
        registerBinaryHierarchyAdapter(Enum::class, EnumBinaryAdapterLegacy)
        registerBinaryHierarchyAdapter(Collection::class, CollectionBinaryAdapterLegacy)
        registerBinaryHierarchyAdapter(Map::class, MapBinaryAdapterLegacy)
        
        // binary hierarchy adapters for bukkit / nova
        registerBinaryHierarchyAdapter(ItemStack::class, ItemStackBinaryAdapterLegacy)
    
        // instance creators
        registerInstanceCreator(EnumMap::class, EnumMapInstanceCreatorLegacy)
    }
    
    fun <T : Any> registerBinaryAdapter(clazz: KClass<T>, adapter: BinaryAdapterLegacy<T>) {
        binaryAdapters[clazz] = adapter
    }
    
    fun <T : Any> registerBinaryHierarchyAdapter(clazz: KClass<T>, adapter: BinaryAdapterLegacy<T>) {
        binaryHierarchyAdapters[clazz] = adapter
    }
    
    fun <T : Any> registerInstanceCreator(clazz: KClass<*>, creator: InstanceCreatorLegacy<T>) {
        instanceCreators[clazz] = creator
    }
    
    inline fun <reified T> read(buf: ByteBuf): T? {
        return read(type<T>(), buf)
    }
    
    inline fun <reified T> read(bytes: ByteArray): T? {
        return read(type<T>(), bytes)
    }
    
    fun <T> read(type: Type, buf: ByteBuf): T? {
        if (buf.readBoolean()) {
            val clazz = type.rawType.kotlin
            val typeAdapter = getBinaryAdapter<T>(clazz)
            return typeAdapter.read(type, buf)
        }
        
        return null
    }
    
    fun <T> read(type: Type, bytes: ByteArray): T? {
        val buf = Unpooled.wrappedBuffer(bytes)
        return read(type, buf)
    }
    
    fun write(obj: Any?, buf: ByteBuf) {
        if (obj != null) {
            buf.writeBoolean(true)
            
            val clazz = obj::class
            val typeAdapter = getBinaryAdapter<Any>(clazz)
            typeAdapter.write(obj, buf)
        } else buf.writeBoolean(false)
    }
    
    fun write(obj: Any?): ByteArray {
        val buf = Unpooled.buffer()
        write(obj, buf)
        return buf.toByteArray()
    }
    
    fun <T> createInstance(type: Type): T? {
        val clazz = type.rawType.kotlin
        
        val creator = instanceCreators[clazz]
        if (creator != null)
            return creator.createInstance(type) as T
        
        return clazz.constructors
            .firstOrNull { it.parameters.isEmpty() }
            ?.call() as T?
    }
    
    private fun <R> getBinaryAdapter(clazz: KClass<*>): BinaryAdapterLegacy<R> {
        val typeAdapter: BinaryAdapterLegacy<*>? =
            if (clazz in binaryAdapters)
                binaryAdapters[clazz]
            else binaryHierarchyAdapters.entries.firstOrNull { it.key.isSuperclassOf(clazz) }?.value
        
        if (typeAdapter == null)
            throw IllegalStateException("No binary adapter registered for $clazz")
        
        return typeAdapter as BinaryAdapterLegacy<R>
    }
    
}