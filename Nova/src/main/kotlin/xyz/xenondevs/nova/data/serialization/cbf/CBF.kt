package xyz.xenondevs.nova.data.serialization.cbf

import de.studiocode.invui.virtualinventory.VirtualInventory
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.serialization.cbf.Compound.CompoundBinaryAdapter
import xyz.xenondevs.nova.data.serialization.cbf.adapter.*
import xyz.xenondevs.nova.data.serialization.cbf.instancecreator.EnumMapInstanceCreator
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.util.data.toByteArray
import xyz.xenondevs.nova.util.reflection.representedKClass
import xyz.xenondevs.nova.util.reflection.type
import java.awt.Color
import java.lang.reflect.Type
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

interface BinaryAdapter<T> {
    
    fun write(obj: T, buf: ByteBuf)
    
    fun read(type: Type, buf: ByteBuf): T
    
}

interface InstanceCreator<T> {
    
    fun createInstance(type: Type): T
    
}

@Suppress("UNCHECKED_CAST")
object CBF {
    
    private val binaryAdapters = HashMap<KClass<*>, BinaryAdapter<*>>()
    private val binaryHierarchyAdapters = HashMap<KClass<*>, BinaryAdapter<*>>()
    
    private val instanceCreators = HashMap<KClass<*>, InstanceCreator<*>>()
    
    init {
        // default binary adapters
        registerBinaryAdapter(Boolean::class, BooleanBinaryAdapter)
        registerBinaryAdapter(BooleanArray::class, BooleanArrayBinaryAdapter)
        registerBinaryAdapter(Byte::class, ByteBinaryAdapter)
        registerBinaryAdapter(ByteArray::class, ByteArrayBinaryAdapter)
        registerBinaryAdapter(Short::class, ShortBinaryAdapter)
        registerBinaryAdapter(ShortArray::class, ShortArrayBinaryAdapter)
        registerBinaryAdapter(Int::class, IntBinaryAdapter)
        registerBinaryAdapter(IntArray::class, IntArrayBinaryAdapter)
        registerBinaryAdapter(Long::class, LongBinaryAdapter)
        registerBinaryAdapter(LongArray::class, LongArrayBinaryAdapter)
        registerBinaryAdapter(Float::class, FloatBinaryAdapter)
        registerBinaryAdapter(FloatArray::class, FloatArrayBinaryAdapter)
        registerBinaryAdapter(Double::class, DoubleBinaryAdapter)
        registerBinaryAdapter(DoubleArray::class, DoubleArrayBinaryAdapter)
        registerBinaryAdapter(Char::class, CharBinaryAdapter)
        registerBinaryAdapter(CharArray::class, CharArrayBinaryAdapter)
        registerBinaryAdapter(String::class, StringBinaryAdapter)
        registerBinaryAdapter(Array<String>::class, StringArrayBinaryAdapter)
        registerBinaryAdapter(UUID::class, UUIDBinaryAdapter)
        registerBinaryAdapter(Pair::class, PairBinaryAdapter)
        registerBinaryAdapter(Triple::class, TripleBinaryAdapter)
        registerBinaryAdapter(Compound::class, CompoundBinaryAdapter)
        
        // binary adapters for bukkit / nova
        registerBinaryAdapter(Color::class, ColorBinaryAdapter)
        registerBinaryAdapter(ItemStack::class, ItemStackBinaryAdapter)
        registerBinaryAdapter(Location::class, LocationBinaryAdapter)
        registerBinaryAdapter(NamespacedKey::class, NamespacedKeyBinaryAdapter)
        registerBinaryAdapter(NamespacedId::class, NamespacedIdBinaryAdapter)
        registerBinaryAdapter(VirtualInventory::class, VirtualInventoryBinaryAdapter)
        registerBinaryAdapter(NetworkType::class, NetworkTypeBinaryAdapter)
        
        // binary hierarchy adapters
        registerBinaryHierarchyAdapter(Enum::class, EnumBinaryAdapter)
        registerBinaryHierarchyAdapter(Collection::class, CollectionBinaryAdapter)
        registerBinaryHierarchyAdapter(Map::class, MapBinaryAdapter)
        
        // instance creators
        registerInstanceCreator(EnumMap::class, EnumMapInstanceCreator)
    }
    
    fun <T : Any> registerBinaryAdapter(clazz: KClass<T>, adapter: BinaryAdapter<T>) {
        binaryAdapters[clazz] = adapter
    }
    
    fun <T : Any> registerBinaryHierarchyAdapter(clazz: KClass<T>, adapter: BinaryAdapter<T>) {
        binaryHierarchyAdapters[clazz] = adapter
    }
    
    fun <T : Any> registerInstanceCreator(clazz: KClass<*>, creator: InstanceCreator<T>) {
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
            val clazz = type.representedKClass
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
        val clazz = type.representedKClass
        
        val creator = instanceCreators[clazz]
        if (creator != null)
            return creator.createInstance(type) as T
        
        return clazz.constructors
            .firstOrNull { it.parameters.isEmpty() }
            ?.call() as T?
    }
    
    private fun <R> getBinaryAdapter(clazz: KClass<*>): BinaryAdapter<R> {
        val typeAdapter: BinaryAdapter<*>? =
            if (clazz in binaryAdapters)
                binaryAdapters[clazz]
            else binaryHierarchyAdapters.entries.firstOrNull { it.key.isSuperclassOf(clazz) }?.value
        
        if (typeAdapter == null)
            throw IllegalStateException("No binary adapter registered for $clazz")
        
        return typeAdapter as BinaryAdapter<R>
    }
    
}