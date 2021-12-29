package xyz.xenondevs.nova.util.data

class WatchedMap<K, V>(
    private val map: MutableMap<K, V>,
    private val handleUpdate: () -> Unit
) : MutableMap<K, V> {
    
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = map.entries
    override val keys: MutableSet<K>
        get() = map.keys
    override val values: MutableCollection<V>
        get() = map.values
    override val size: Int
        get() = map.size
    
    override fun containsKey(key: K) = map.containsKey(key)
    override fun containsValue(value: V) = map.containsValue(value)
    override fun get(key: K) = map[key]
    override fun isEmpty() = map.isEmpty()
    
    override fun clear() {
        map.clear()
        handleUpdate()
    }
    
    override fun put(key: K, value: V): V? {
        val v = map.put(key, value)
        handleUpdate()
        return v
    }
    
    override fun putAll(from: Map<out K, V>) {
        map.putAll(from)
        handleUpdate()
    }
    
    override fun remove(key: K): V? {
        val v = map.remove(key)
        handleUpdate()
        return v
    }
    
}