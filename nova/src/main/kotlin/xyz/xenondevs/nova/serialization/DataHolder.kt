package xyz.xenondevs.nova.serialization

import net.minecraft.server.commands.data.DataAccessor
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.cbf.UncheckedApi
import xyz.xenondevs.commons.provider.MutableProvider
import kotlin.reflect.KType

abstract class DataHolder internal constructor(includePersistent: Boolean) {
    
    @PublishedApi
    internal abstract val data: Compound
    
    @PublishedApi
    internal val persistentData: Compound by lazy {
        data.rename("global", "persistent") // legacy conversion
        data["persistent"] ?: Compound().also { if (includePersistent) data["persistent"] = it }
    }
    
    /**
     * Retrieves data from the data [Compound] of this TileEntity.
     * If it can't find anything under the given key, the result of the
     * [defaultValue] lambda is returned.
     *
     * Prefer using [MutableProviders][MutableProvider] via [storedValue] instead.
     */
    inline fun <reified T> retrieveData(key: String, defaultValue: () -> T): T {
        return retrieveDataOrNull(key) ?: defaultValue()
    }
    
    /**
     * Retrieves data of type [T] or returns `null` if there is no data stored under the given [key].
     *
     * Prefer using [MutableProviders][MutableProvider] via [storedValue] instead.
     */
    inline fun <reified T> retrieveDataOrNull(key: String): T? {
        return data[key] ?: persistentData[key]
    }
    
    /**
     * Retrieves data of the specified [type] or invokes the [defaultValue] lambda if there is no data stored under the given [key].
     *
     * Prefer using [DataAccessors][DataAccessor] via [storedValue] instead.
     */
    @UncheckedApi
    inline fun <T> retrieveData(type: KType, key: String, defaultValue: () -> T): T {
        return retrieveDataOrNull(type, key) ?: defaultValue()
    }
    
    /**
     * Retrieves data of the specified [type] or returns `null` if there is no data stored under the given [key].
     *
     * Prefer using [MutableProviders][MutableProvider] via [storedValue] instead.
     */
    @UncheckedApi
    fun <T> retrieveDataOrNull(type: KType, key: String): T? {
        return data.get(type, key) ?: persistentData.get(type, key)
    }
    
    /**
     * Checks whether there is data stored under the given [key],
     * regardless of whether it is persistent or not.
     */
    fun hasData(key: String): Boolean =
        key in data || key in persistentData
    
    /**
     * Removes the data stored under the given [key],
     * regardless of whether it is persistent or not.
     * 
     * @throws IllegalArgumentException If the entry cannot be removed due to a non-null provider being bound to it.
     */
    fun removeData(key: String) {
        data[key] = null
        persistentData[key] = null
    }
    
    /**
     * Stores [value] as the reified type [T] under the given [key].
     *
     * Prefer using [MutableProviders][MutableProvider] via [storedValue] instead.
     *
     * @param persistent If the data should also be stored in the [ItemStack].
     */
    inline fun <reified T> storeData(key: String, value: T, persistent: Boolean = false) {
        if (persistent) {
            require(!data.contains(key)) { "$key is already a non-persistent value" }
            persistentData[key] = value
        } else {
            require(!persistentData.contains(key)) { "$key is already a persistent value" }
            data[key] = value
        }
    }
    
    /**
     * Stores [value] as the given [type] under the given [key].
     *
     * Prefer using [MutableProviders][MutableProvider] via [storedValue] instead.
     *
     * @param persistent If the data should also be stored in the [ItemStack].
     */
    @UncheckedApi
    fun <T> storeData(type: KType, key: String, value: T, persistent: Boolean = false) {
        if (persistent) {
            require(!data.contains(key)) { "$key is already a non-persistent value" }
            persistentData.set(type, key, value)
        } else {
            require(!persistentData.contains(key)) { "$key is already a persistent value" }
            data.set(type, key, value)
        }
    }
    
    /**
     * Creates a [MutableProvider] to which properties can delegate.
     *
     * The [MutableProvider] will contain the non-persistent data of type [T] under [key] or
     * the result of [defaultValue] if there is no data stored under that [key].
     */
    inline fun <reified T : Any> storedValue(key: String, noinline defaultValue: () -> T): MutableProvider<T> =
        storedValue(key, false, defaultValue)
    
    /**
     * Creates a [MutableProvider] to which properties can delegate.
     *
     * The [MutableProvider] will contain the data of type [T] under [key] or the result of [defaultValue] if there is no data stored under that [key].
     * For mutable data types, it is required that [defaultValue] returns a new instance every time it is called,
     * and that all instances are [equal][Any.equals] to each other.
     *
     * @param persistent If the data should also be stored in the [ItemStack].
     */
    inline fun <reified T : Any> storedValue(key: String, persistent: Boolean, noinline defaultValue: () -> T): MutableProvider<T> =
        if (persistent) persistentData.entry(key, defaultValue) else data.entry(key, defaultValue)
    
    /**
     * Creates a [MutableProvider] to which properties can delegate.
     *
     * The [MutableProvider] will contain the data of type [T] under [key] or `null` if there is no data stored under that [key].
     *
     * @param persistent If the data should also be stored in the [ItemStack].
     */
    inline fun <reified T : Any> storedValue(key: String, persistent: Boolean = false): MutableProvider<T?> =
        if (persistent) persistentData.entry(key) else data.entry(key)
    
}