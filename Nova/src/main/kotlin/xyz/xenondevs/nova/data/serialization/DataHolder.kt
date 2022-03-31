package xyz.xenondevs.nova.data.serialization

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.Element
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.EnumMapElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.ListElement
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.reflection.type
import java.util.*

abstract class DataHolder(includeGlobal: Boolean) {
    
    abstract val data: CompoundElement
    val globalData by lazy {
        val global = data.getElement<CompoundElement>("global")
        global ?: CompoundElement().also { if (includeGlobal) data.putElement("global", it) }
    }
    
    // region element retrieval
    
    /**
     * Retrieves an element from the data [CompoundElement] of this TileEntity.
     */
    inline fun <reified T : Element> retrieveElementOrNull(key: String): T? {
        return data.getElement(key) ?: globalData.getElement(key)
    }
    
    // endregion
    
    //region BackedElement data retrieval
    
    /**
     * Retrieves data from the data [CompoundElement] of this TileEntity.
     * If it can't find anything under the given key, the result of the
     * [getAlternative] lambda is returned.
     * @see retrieveCollectionOrNull
     * @see retrieveEnumCollectionOrNull
     * @see retrieveEnum
     * @see retrieveEnumMap
     * @see retrieveDoubleEnumMap
     */
    inline fun <reified T> retrieveData(key: String, getAlternative: () -> T): T {
        return retrieveOrNull(key) ?: getAlternative()
    }
    
    /**
     * Retrieves data using CBF deserialization from the data [CompoundElement].
     * If neither [data] nor [globalData] contains the given key, ``null`` is returned
     * @see retrieveCollectionOrNull
     * @see retrieveEnumCollectionOrNull
     * @see retrieveEnumOrNull
     * @see retrieveEnumMapOrNull
     * @see retrieveDoubleEnumMapOrNull
     */
    inline fun <reified T> retrieveOrNull(key: String): T? {
        return data.get(key) ?: globalData.get(key)
    }
    
    //endregion
    
    //region Enum data retrieval
    
    /**
     * Retrieves an enum constant. If it can't find anything under the
     * given key, the result of the [getAlternative] lambda is returned.
     */
    inline fun <reified T : Enum<T>> retrieveEnum(key: String, getAlternative: () -> T) =
        retrieveEnumOrNull(key) ?: getAlternative()
    
    /**
     * Retrieves an enum constant. If [data] doesn't contains
     * the given key, ``null`` is returned
     */
    inline fun <reified T : Enum<T>> retrieveEnumOrNull(key: String): T? {
        return data.getEnumConstant<T>(key)
    }
    
    /**
     * Retrieves an [EnumMap]. If it can't find anything under the
     * given key, the result of the [getAlternative] lambda is returned.
     */
    inline fun <reified K : Enum<K>, reified V> retrieveEnumMap(key: String, getAlternative: () -> EnumMap<K, V>): EnumMap<K, V> =
        retrieveEnumMapOrNull(key) ?: getAlternative()
    
    /**
     * Retrieves an [EnumMap]. If [data] doesn't contains the given key,
     * ``null`` is returned
     */
    inline fun <reified K : Enum<K>, reified V> retrieveEnumMapOrNull(key: String): EnumMap<K, V>? {
        val mapElement = data.getElement<EnumMapElement>(key) ?: return null
        return mapElement.toEnumMap()
    }
    
    //endregion
    
    // region Collection data retrieval
    
    /**
     * Retrieves a [ListElement] and maps Its values to the given [destination][dest]
     */
    inline fun <reified T, C : MutableCollection<in T>> retrieveCollectionOrNull(key: String, dest: C): C? {
        val listElement = data.getElement<ListElement>(key) ?: return null
        return listElement.toCollection(dest)
    }
    
    /**
     * Retrieves a [ListElement] and maps Its values to enum constants and adds them the given [destination][dest]
     */
    inline fun <reified E : Enum<E>, C : MutableCollection<in E>> retrieveEnumCollectionOrNull(key: String, dest: C): C? {
        val listElement = data.getElement<ListElement>(key) ?: return null
        return listElement.toEnumCollection(dest)
    }
    
    // endregion
    
    // region Data storage
    
    /**
     * Serializes objects using CBF and stores them under the given key in
     * the data object (Supports Enum constants)
     *
     * @param global If the data should also be stored in the [ItemStack]
     * of this [TileEntity].
     * @see storeEnumMap
     * @see storeList
     */
    fun storeData(key: String, value: Any?, global: Boolean = false) {
        if (global) {
            require(!data.contains(key)) { "$key is already a non-global value" }
            if (value != null) globalData.put(key, value)
            else globalData.remove(key)
        } else {
            require(!globalData.contains(key)) { "$key is already a global value" }
            if (value != null) data.put(key, value)
            else data.remove(key)
        }
    }
    
    /**
     * Serializes [EnumMaps][EnumMap] using CBF and stores them in the [data][CompoundElement]
     */
    inline fun <K : Enum<K>, reified V> storeEnumMap(key: String, map: Map<K, V>?, global: Boolean = false) {
        if (map != null)
            storeData(key, EnumMapElement.of(map, type<V>()), global)
        else storeData(key, null, global)
    }
    
    /**
     * Serializes [EnumMaps][EnumMap] using CBF and stores them in the [data][CompoundElement]
     * while mapping the values using the given [valueMapper].
     */
    inline fun <reified K : Enum<K>, reified V, reified R> storeEnumMap(key: String, map: Map<K, V>?, global: Boolean = false, valueMapper: (V) -> R) {
        if (map != null) {
            val enumMap = map.mapValuesTo(emptyEnumMap<K, R>()) { valueMapper(it.value) }
            storeData(key, EnumMapElement.of(enumMap, type<V>()), global)
        } else storeData(key, null, global)
    }
    
    /**
     * Serializes [Collections][Collection] using CBF and stores them in the [data] [CompoundElement]
     */
    inline fun <reified V> storeList(key: String, list: Collection<V>?, global: Boolean = false) {
        if (list != null) {
            val listElement = ListElement()
            list.forEach { listElement.add(it) }
            storeData(key, listElement, global)
        } else storeData(key, null, global)
    }
    
    // endregion
    
}