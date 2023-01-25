package xyz.xenondevs.nova.data.serialization.cbf

import org.bukkit.NamespacedKey
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.adapter.BinaryAdapter
import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class NamespacedCompound internal constructor(
    private val map: HashMap<String, Compound>
) {
    
    val keys: Set<NamespacedId>
        get() = map.flatMapTo(HashSet()) { (namespace, compound) -> compound.keys.map { NamespacedId.of("$namespace:$it") } }
    
    constructor() : this(HashMap())
    
    operator fun set(namespace: String, key: String, value: Any?) {
        map.getOrPut(namespace, ::Compound)[key] = value
    }
    
    operator fun set(id: NamespacedId, value: Any?) {
        set(id.namespace, id.name, value)
    }
    
    operator fun set(key: NamespacedKey, value: Any?) {
        set(key.namespace, key.key, value)
    }
    
    operator fun set(addon: Addon, key: String, value: Any?) {
        set(addon.description.id, key, value)
    }
    
    
    fun <T> get(type: KType, namespace: String, key: String): T? {
        return map[namespace]?.get(type, key)
    }
    
    fun <T> get(type: KType, id: NamespacedId): T? {
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
    
    inline operator fun <reified T> get(id: NamespacedId): T? {
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
    
    inline fun <reified T> getOrPut(id: NamespacedId, defaultValue: () -> T): T {
        return getOrPut(id.namespace, id.name, defaultValue)
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
    
    fun remove(id: NamespacedId) {
        remove(id.namespace, id.name)
    }
    
    fun remove(key: NamespacedKey) {
        remove(key.namespace, key.key)
    }
    
    fun remove(addon: Addon, key: String) {
        remove(addon.description.id, key)
    }
    
    operator fun minusAssign(id: NamespacedId) {
        remove(id)
    }
    
    operator fun minusAssign(key: NamespacedKey) {
        remove(key)
    }
    
    
    @PublishedApi
    internal fun contains(namespace: String, key: String): Boolean {
        return map[namespace]?.contains(key) ?: false
    }
    
    fun contains(id: NamespacedId): Boolean {
        return contains(id.namespace, id.name)
    }
    
    fun contains(key: NamespacedKey): Boolean {
        return contains(key.namespace, key.key)
    }
    
    fun contains(addon: Addon, key: String): Boolean {
        return contains(addon.description.id, key)
    }
    
    
    fun isEmpty(): Boolean = map.isNotEmpty()
    
    fun isNotEmpty(): Boolean = map.isNotEmpty()
    
    fun copy(): NamespacedCompound {
        return NamespacedCompound(HashMap(map))
    }
    
    override fun toString(): String {
        val builder = StringBuilder()
        builder.append("{")
        
        map.entries.forEach { (key, value) ->
            builder.append("\n\"$key\": $value")
        }
        
        return builder.toString().replace("\n", "\n  ") + "\n}"
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
        
    }
    
}