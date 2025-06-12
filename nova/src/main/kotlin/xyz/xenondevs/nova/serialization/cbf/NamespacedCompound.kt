package xyz.xenondevs.nova.serialization.cbf

import net.kyori.adventure.key.Key
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.UncheckedApi
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.cbf.io.byteWriter
import xyz.xenondevs.cbf.serializer.BinarySerializer
import xyz.xenondevs.cbf.serializer.UnversionedBinarySerializer
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.id
import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class NamespacedCompound internal constructor(
    private val map: MutableMap<String, Compound>
) {
    
    val keys: Set<Key>
        get() = map.flatMapTo(HashSet()) { (namespace, compound) -> compound.keys.map { Key.key(namespace, it) } }
    
    constructor() : this(HashMap())
    
    @UncheckedApi
    fun set(type: KType, namespace: String, key: String, value: Any?) {
        val compound = map.getOrPut(namespace, ::Compound)
        compound.set(type, key, value)
    }
    
    @UncheckedApi
    fun set(type: KType, key: Key, value: Any?) {
        set(type, key.namespace(), key.value(), value)
    }
    
    @UncheckedApi
    fun set(type: KType, addon: Addon, key: String, value: Any?) {
        set(type, addon.id, key, value)
    }
    
    
    @OptIn(UncheckedApi::class)
    inline operator fun <reified T> set(namespace: String, key: String, value: T) {
        set(typeOf<T>(), namespace, key, value)
    }
    
    @OptIn(UncheckedApi::class)
    inline operator fun <reified T> set(key: Key, value: T) {
        set(typeOf<T>(), key, value)
    }
    
    @OptIn(UncheckedApi::class)
    inline operator fun <reified T> set(addon: Addon, key: String, value: T) {
        set(typeOf<T>(), addon, key, value)
    }
    
    
    @UncheckedApi
    fun <T> get(type: KType, namespace: String, key: String): T? {
        return map[namespace]?.get(type, key)
    }
    
    @UncheckedApi
    fun <T> get(type: KType, key: Key): T? {
        return get(type, key.namespace(), key.value())
    }
    
    @UncheckedApi
    fun <T> get(type: KType, addon: Addon, key: String): T? {
        return get(type, addon.id, key)
    }
    
    
    @OptIn(UncheckedApi::class)
    inline operator fun <reified T> get(namespace: String, key: String): T? {
        return get(typeOf<T>(), namespace, key)
    }
    
    @OptIn(UncheckedApi::class)
    inline operator fun <reified T> get(key: Key): T? {
        return get(typeOf<T>(), key)
    }
    
    @OptIn(UncheckedApi::class)
    inline operator fun <reified T> get(addon: Addon, key: String): T? {
        return get(typeOf<T>(), addon, key)
    }
    
    
    inline fun <reified T> getOrPut(namespace: String, key: String, defaultValue: () -> T): T {
        return get(namespace, key) ?: defaultValue().also { set(namespace, key, it) }
    }
    
    inline fun <reified T> getOrPut(key: Key, defaultValue: () -> T): T {
        return getOrPut(key.namespace(), key.value(), defaultValue)
    }
    
    inline fun <reified T> getOrPut(addon: Addon, key: String, defaultValue: () -> T): T {
        return getOrPut(addon.id, key, defaultValue)
    }
    
    
    fun remove(namespace: String, key: String) {
        val compound = map[namespace] ?: return
        compound[key] = null
        if (compound.isEmpty()) map.remove(namespace)
    }
    
    fun remove(key: Key) {
        remove(key.namespace(), key.value())
    }
    
    fun remove(addon: Addon, key: String) {
        remove(addon.id, key)
    }
    
    operator fun minusAssign(key: Key) {
        remove(key)
    }
    
    
    @PublishedApi
    internal fun contains(namespace: String, key: String): Boolean {
        return map[namespace]?.contains(key) == true
    }
    
    fun contains(key: Key): Boolean {
        return contains(key.namespace(), key.value())
    }
    
    fun contains(addon: Addon, key: String): Boolean {
        return contains(addon.id, key)
    }
    
    
    fun putAll(other: NamespacedCompound) {
        for ((namespace, otherCompound) in other.map) {
            val compound = map.getOrPut(namespace, ::Compound)
            compound.putAll(otherCompound)
        }
    }
    
    fun copy(): NamespacedCompound = NamespacedCompound(map.mapValuesTo(HashMap()) { (_, value) -> value.copy() })
    
    fun isEmpty(): Boolean = map.isEmpty()
    
    fun isNotEmpty(): Boolean = map.isNotEmpty()
    
    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("{")
        
        map.entries.forEach { (key, value) ->
            builder.append("\n\"$key\": $value")
        }
        
        return builder.toString().replace("\n", "\n  ") + "\n}"
    }
    
    companion object {
        
        val EMPTY = NamespacedCompound(Collections.unmodifiableMap(HashMap()))
        
    }
    
    internal object NamespacedCompoundBinarySerializer : UnversionedBinarySerializer<NamespacedCompound>() {
        
        private val compoundSerializer: BinarySerializer<Compound> = Cbf.getSerializer()
        
        override fun writeUnversioned(obj: NamespacedCompound, writer: ByteWriter) {
            // count serialized elements instead of obj.size to prevent concurrent modifications from causing corrupted data
            var size = 0
            val temp = byteWriter {
                for ((key, data) in obj.map) {
                    writeString(key)
                    compoundSerializer.write(data, this)
                    size++
                }
            }
            
            writer.writeVarInt(size)
            writer.writeBytes(temp)
        }
        
        override fun readUnversioned(reader: ByteReader): NamespacedCompound {
            val mapSize = reader.readVarInt()
            val map = HashMap<String, Compound>(mapSize)
            
            repeat(mapSize) {
                val key = reader.readString()
                val compound = compoundSerializer.read(reader)!!
                map[key] = compound
            }
            
            return NamespacedCompound(map)
        }
        
        override fun copyNonNull(obj: NamespacedCompound): NamespacedCompound {
            return obj.copy()
        }
        
    }
    
}