package xyz.xenondevs.nova.data.serialization

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.provider.mutable.MutableProvider
import kotlin.reflect.KType
import kotlin.reflect.typeOf

abstract class DataHolder internal constructor(includePersistent: Boolean) {
    
    @PublishedApi
    internal val dataAccessors = ArrayList<DataAccessor<*>>()
    
    @PublishedApi
    internal abstract val data: Compound
    
    @PublishedApi
    internal val persistentData: Compound by lazy {
        data["global"] // legacy name TODO: remove legacy support at some point
            ?: data["persistent"]
            ?: Compound().also { if (includePersistent) data["persistent"] = it }
    }
    
    /**
     * Retrieves data from the data [Compound] of this TileEntity.
     * If it can't find anything under the given key, the result of the
     * [defaultValue] lambda is returned.
     *
     * Prefer using [DataAccessors][DataAccessor] via [storedValue] instead.
     */
    inline fun <reified T> retrieveData(key: String, defaultValue: () -> T): T {
        return retrieveDataOrNull(key) ?: defaultValue()
    }
    
    /**
     * Retrieves data of type [T] or returns `null` if there is no data stored under the given [key].
     *
     * Prefer using [DataAccessors][DataAccessor] via [storedValue] instead.
     */
    inline fun <reified T> retrieveDataOrNull(key: String): T? {
        return data[key] ?: persistentData[key]
    }
    
    /**
     * Retrieves data of the specified [type] or invokes the [defaultValue] lambda if there is no data stored under the given [key].
     *
     * Prefer using [DataAccessors][DataAccessor] via [storedValue] instead.
     */
    inline fun <T> retrieveData(type: KType, key: String, defaultValue: () -> T): T {
        return retrieveDataOrNull(type, key) ?: defaultValue()
    }
    
    /**
     * Retrieves data of the specified [type] or returns `null` if there is no data stored under the given [key].
     *
     * Prefer using [DataAccessors][DataAccessor] via [storedValue] instead.
     */
    fun <T> retrieveDataOrNull(type: KType, key: String): T? {
        return data.get(type, key) ?: persistentData.get(type, key)
    }
    
    internal fun hasData(key: String): Boolean =
        data.contains(key) || persistentData.contains(key)
    
    internal fun removeData(key: String) {
        data.remove(key)
        persistentData.remove(key)
    }
    
    /**
     * Stores [value] as the reified type [T] under the given [key].
     *
     * Prefer using [DataAccessors][DataAccessor] via [storedValue] instead.
     *
     * @param persistent If the data should also be stored in the [ItemStack].
     */
    inline fun <reified T> storeData(key: String, value: T?, persistent: Boolean = false) {
        storeData(typeOf<T>(), key, value, persistent)
    }
    
    /**
     * Stores [value] as the given [type] under the given [key].
     *
     * Prefer using [DataAccessors][DataAccessor] via [storedValue] instead.
     *
     * @param persistent If the data should also be stored in the [ItemStack].
     */
    fun <T> storeData(type: KType, key: String, value: T?, persistent: Boolean = false) {
        if (persistent) {
            require(!data.contains(key)) { "$key is already a non-persistent value" }
            if (value != null) persistentData.set(type, key, value)
            else persistentData.remove(key)
        } else {
            require(!persistentData.contains(key)) { "$key is already a persistent value" }
            if (value != null) data.set(type, key, value)
            else data.remove(key)
        }
    }
    
    /**
     * Creates a [DataAccessor] to which properties can delegate.
     *
     * The [DataAccessor] will contain the non-persistent data of type [T] under [key] or
     * the result of [defaultValue] if there is no data stored under that [key].
     */
    inline fun <reified T> storedValue(key: String, defaultValue: () -> T): MutableProvider<T> {
        return storedValue(key, false, defaultValue)
    }
    
    /**
     * Creates a [DataAccessor] to which properties can delegate.
     *
     * The [DataAccessor] will contain the data of type [T] under [key] or the result of [defaultValue] if there is no data stored under that [key].
     *
     * @param persistent If the data should also be stored in the [ItemStack].
     */
    inline fun <reified T> storedValue(key: String, persistent: Boolean, defaultValue: () -> T): MutableProvider<T> {
        val type = typeOf<T>()
        val initialValue = retrieveData(type, key, defaultValue)
        return DataAccessor(type, key, persistent, initialValue).also(dataAccessors::add)
    }
    
    /**
     * Creates a [DataAccessor] to which properties can delegate.
     *
     * The [DataAccessor] will contain the data of type [T] under [key] or `null` if there is no data stored under that [key].
     *
     * @param persistent If the data should also be stored in the [ItemStack].
     */
    inline fun <reified T> storedValue(key: String, persistent: Boolean = false): MutableProvider<T?> {
        val type = typeOf<T>()
        val initialValue = retrieveDataOrNull<T>(type, key)
        return DataAccessor(type, key, persistent, initialValue).also(dataAccessors::add)
    }
    
    internal fun saveDataAccessors() {
        dataAccessors.forEach(DataAccessor<*>::save)
    }
    
    @PublishedApi
    internal inner class DataAccessor<T>(
        private val type: KType,
        private val key: String,
        private val persistent: Boolean,
        private val initialValue: T,
    ) : MutableProvider<T>() {
        
        override fun loadValue(): T {
            return initialValue
        }
        
        fun save() {
            storeData(type, key, value, persistent)
        }
        
    }
    
}