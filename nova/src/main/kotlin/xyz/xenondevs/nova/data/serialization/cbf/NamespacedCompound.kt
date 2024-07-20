package xyz.xenondevs.nova.data.serialization.cbf

import net.minecraft.resources.ResourceLocation
import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.util.name
import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class NamespacedCompound internal constructor(
    private val map: MutableMap<String, Compound>
) {
    
    val keys: Set<ResourceLocation>
        get() = map.flatMapTo(HashSet()) { (namespace, compound) -> compound.keys.map { ResourceLocation.parse("$namespace:$it") } }
    
    constructor() : this(HashMap())
    
    fun set(type: KType, namespace: String, key: String, value: Any?) {
        val compound = map.getOrPut(namespace, ::Compound)
        compound.set(type, key, value)
    }
    
    fun set(type: KType, id: ResourceLocation, value: Any?) {
        set(type, id.namespace, id.name, value)
    }
    
    fun set(type: KType, key: NamespacedKey, value: Any?) {
        set(type, key.namespace, key.key, value)
    }
    
    fun set(type: KType, addon: Addon, key: String, value: Any?) {
        set(type, addon.description.id, key, value)
    }
    
    
    inline operator fun <reified T> set(namespace: String, key: String, value: T) {
        set(typeOf<T>(), namespace, key, value)
    }
    
    inline operator fun <reified T> set(id: ResourceLocation, value: T) {
        set(typeOf<T>(), id, value)
    }
    
    inline operator fun <reified T> set(key: NamespacedKey, value: T) {
        set(typeOf<T>(), key, value)
    }
    
    inline operator fun <reified T> set(addon: Addon, key: String, value: T) {
        set(typeOf<T>(), addon, key, value)
    }
    
    
    fun <T> get(type: KType, namespace: String, key: String): T? {
        return map[namespace]?.get(type, key)
    }
    
    fun <T> get(type: KType, id: ResourceLocation): T? {
        return get(type, id.namespace, id.name)
    }
    
    fun <T> get(type: KType, key: NamespacedKey): T? {
        return get(type, key.namespace, key.key)
    }
    
    fun <T> get(type: KType, addon: Addon, key: String): T? {
        return get(type, addon.description.id, key)
    }
    
    
    inline operator fun <reified T> get(namespace: String, key: String): T? {
        return get(typeOf<T>(), namespace, key)
    }
    
    inline operator fun <reified T> get(id: ResourceLocation): T? {
        return get(typeOf<T>(), id)
    }
    
    inline operator fun <reified T> get(key: NamespacedKey): T? {
        return get(typeOf<T>(), key)
    }
    
    inline operator fun <reified T> get(addon: Addon, key: String): T? {
        return get(typeOf<T>(), addon, key)
    }
    
    
    inline fun <reified T> getOrPut(namespace: String, key: String, defaultValue: () -> T): T {
        return get(namespace, key) ?: defaultValue().also { set(namespace, key, it) }
    }
    
    inline fun <reified T> getOrPut(id: ResourceLocation, defaultValue: () -> T): T {
        return getOrPut(id.namespace, id.path, defaultValue)
    }
    
    inline fun <reified T> getOrPut(key: NamespacedKey, defaultValue: () -> T): T {
        return getOrPut(key.namespace, key.key, defaultValue)
    }
    
    inline fun <reified T> getOrPut(addon: Addon, key: String, defaultValue: () -> T): T {
        return getOrPut(addon.description.id, key, defaultValue)
    }
    
    
    fun remove(namespace: String, key: String) {
        val compound = map[namespace] ?: return
        compound.remove(key)
        if (compound.isEmpty()) map.remove(namespace)
    }
    
    fun remove(id: ResourceLocation) {
        remove(id.namespace, id.name)
    }
    
    fun remove(key: NamespacedKey) {
        remove(key.namespace, key.key)
    }
    
    fun remove(addon: Addon, key: String) {
        remove(addon.description.id, key)
    }
    
    operator fun minusAssign(id: ResourceLocation) {
        remove(id)
    }
    
    operator fun minusAssign(key: NamespacedKey) {
        remove(key)
    }
    
    
    @PublishedApi
    internal fun contains(namespace: String, key: String): Boolean {
        return map[namespace]?.contains(key) ?: false
    }
    
    fun contains(id: ResourceLocation): Boolean {
        return contains(id.namespace, id.name)
    }
    
    fun contains(key: NamespacedKey): Boolean {
        return contains(key.namespace, key.key)
    }
    
    fun contains(addon: Addon, key: String): Boolean {
        return contains(addon.description.id, key)
    }
    
    
    fun putAll(other: NamespacedCompound) {
        for ((namespace, otherCompound) in other.map) {
            val compound = map.getOrPut(namespace, ::Compound)
            compound.putAll(otherCompound)
        }
    }
    
    fun copy(): NamespacedCompound = NamespacedCompound(map.mapValuesTo(HashMap()) { (_, value) -> value.copy() })
    
    fun isEmpty(): Boolean = map.isNotEmpty()
    
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
    
    internal object NamespacedCompoundBinaryAdapter : BinaryAdapter<NamespacedCompound> {
        
        override fun write(obj: NamespacedCompound, type: KType, writer: ByteWriter) {
            writer.writeVarInt(obj.map.size)
            
            obj.map.forEach { (key, data) ->
                writer.writeString(key)
                CBF.write(data, writer)
            }
        }
        
        override fun read(type: KType, reader: ByteReader): NamespacedCompound {
            val mapSize = reader.readVarInt()
            val map = HashMap<String, Compound>(mapSize)
            
            repeat(mapSize) {
                val key = reader.readString()
                val compound = CBF.read<Compound>(reader)!!
                map[key] = compound
            }
            
            return NamespacedCompound(map)
        }
        
        override fun copy(obj: NamespacedCompound, type: KType): NamespacedCompound {
            return obj.copy()
        }
        
    }
    
}