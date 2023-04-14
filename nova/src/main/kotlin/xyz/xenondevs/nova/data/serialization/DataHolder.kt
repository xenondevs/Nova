package xyz.xenondevs.nova.data.serialization

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.provider.mutable.MutableProvider
import xyz.xenondevs.nova.tileentity.TileEntity
import kotlin.reflect.KType
import kotlin.reflect.typeOf

abstract class DataHolder internal constructor(includeGlobal: Boolean) {
    
    @PublishedApi
    internal val dataAccessors = ArrayList<DataAccessor<*>>()
    
    @PublishedApi
    internal abstract val data: Compound
    
    @PublishedApi
    internal val globalData: Compound by lazy {
        val global = data.get<Compound>("global")
        global ?: Compound().also { if (includeGlobal) data["global"] = it }
    }
    
    /**
     * Retrieves data from the data [Compound] of this TileEntity.
     * If it can't find anything under the given key, the result of the
     * [getAlternative] lambda is returned.
     */
    inline fun <reified T : Any> retrieveData(key: String, getAlternative: () -> T): T {
        return retrieveDataOrNull(key) ?: getAlternative()
    }
    
    /**
     * Retrieves data using CBF deserialization from the data [Compound].
     * If neither [storedValue] nor [globalData] contains the given key, ``null`` is returned
     */
    @Deprecated("Inconsistent name", ReplaceWith("retrieveDataOrNull<T>(key)"))
    inline fun <reified T : Any> retrieveOrNull(key: String): T? {
        return retrieveDataOrNull(key)
    }
    
    /**
     * Retrieves data using CBF deserialization from the data [Compound].
     * If neither [storedValue] nor [globalData] contains the given key, ``null`` is returned
     */
    inline fun <reified T : Any> retrieveDataOrNull(key: String): T? {
        return data[key] ?: globalData[key]
    }
    
    /**
     * Retrieves data of the specified [type] from the data [Compound] of this TileEntity.
     * If it can't find anything under the given key, the result of the
     * [getAlternative] lambda is returned.
     */
    inline fun <T : Any> retrieveData(type: KType, key: String, getAlternative: () -> T): T {
        return retrieveDataOrNull(type, key) ?: getAlternative()
    }
    
    /**
     * Retrieves data of the specified [type] using CBF deserialization from the data [Compound].
     * If neither [storedValue] nor [globalData] contains the given key, ``null`` is returned
     */
    fun <T : Any> retrieveDataOrNull(type: KType, key: String): T? {
        return data.get(type, key) ?: globalData.get(type, key)
    }
    
    /**
     * Serializes [value] as the reified type [T] and stores it under the given [key] in the compound.
     *
     * @param global If the data should also be stored in the [ItemStack]
     * of this [TileEntity].
     */
    inline fun <reified T : Any> storeData(key: String, value: T?, global: Boolean = false) {
        storeData(typeOf<T>(), key, value, global)
    }
    
    /**
     *Serializes [value] as the given [type] and stores it under the given [key] in the compound.
     *
     * @param global If the data should also be stored in the [ItemStack]
     * of this [TileEntity].
     */
    fun <T : Any> storeData(type: KType, key: String, value: T?, global: Boolean = false) {
        if (global) {
            require(!data.contains(key)) { "$key is already a non-global value" }
            if (value != null) globalData.set(type, key, value)
            else globalData.remove(key)
        } else {
            require(!globalData.contains(key)) { "$key is already a global value" }
            if (value != null) data.set(type, key, value)
            else data.remove(key)
        }
    }
    
    /**
     * Creates a [DataAccessor] to which properties can delegate.
     *
     * The non-global value under the [key] is retrieved and [getAlternative] is called if there is no
     * value stored under that key.
     */
    inline fun <reified T : Any> storedValue(key: String, getAlternative: () -> T): DataAccessor<T> {
        return storedValue(key, false, getAlternative)
    }
    
    /**
     * Creates a [DataAccessor] to which properties can delegate.
     *
     * The value under the [key] is retrieved and [getAlternative] is called if there is no value
     * stored under that key.
     *
     * @param global If the data should also be stored in the [ItemStack] of this [TileEntity].
     */
    inline fun <reified T : Any> storedValue(key: String, global: Boolean, getAlternative: () -> T): DataAccessor<T> {
        val type = typeOf<T>()
        val initialValue = retrieveData(type, key, getAlternative)
        return DataAccessor(type, key, global, initialValue).also(dataAccessors::add)
    }
    
    /**
     * Creates a [DataAccessor] to which properties can delegate.
     *
     * The value under the [key] is retrieved and null is returned if there is no value stored under that key.
     *
     * @param global If the data should also be stored in the [ItemStack] of this [TileEntity].
     */
    inline fun <reified T : Any> storedValue(key: String, global: Boolean = false): DataAccessor<T?> {
        val type = typeOf<T>()
        val initialValue = retrieveDataOrNull<T>(type, key)
        return DataAccessor(type, key, global, initialValue).also(dataAccessors::add)
    }
    
    internal fun saveDataAccessors() {
        dataAccessors.forEach(DataAccessor<*>::save)
    }
    
    inner class DataAccessor<T> @PublishedApi internal constructor(
        private val type: KType,
        private val key: String,
        private val global: Boolean,
        private val initialValue: T,
    ) : MutableProvider<T>() {
        
        override fun loadValue(): T {
            return initialValue
        }
        
        override fun setValue(value: T) {
            this._value = value
        }
        
        fun save() {
            storeData(type, key, value, global)
        }
        
    }
    
}