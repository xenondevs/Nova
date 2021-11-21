package xyz.xenondevs.nova.data.serialization

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.EnumMapElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.ListElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.toElement
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.emptyEnumMap
import xyz.xenondevs.nova.util.toEnumMap
import java.util.*

open class DataHolder(val data: CompoundElement = CompoundElement(), includeGlobal: Boolean) {
    
    val globalData = data.getElement("global") ?: CompoundElement()
    
    init {
        if (includeGlobal && "global" !in data)
            data.putElement("global", globalData)
    }
    
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
    inline fun <reified K : Enum<K>, V> retrieveEnumMap(key: String, getAlternative: () -> MutableMap<K, V>): MutableMap<K, V> =
        retrieveEnumMapOrNull(key) ?: getAlternative()
    
    /**
     * Retrieves an [EnumMap]. If [data] doesn't contains the given key,
     * ``null`` is returned
     */
    inline fun <reified K : Enum<K>, V> retrieveEnumMapOrNull(key: String): MutableMap<K, V>? {
        val mapElement = data.getElement<EnumMapElement>(key) ?: return null
        return mapElement.toEnumMap()
    }
    
    /**
     * Retrieves an [EnumMap] where the value is an enum as well. If it
     * can't find anything under the given key, the result of the
     * [getAlternative] lambda is returned.
     */
    inline fun <reified K : Enum<K>, reified V : Enum<V>> retrieveDoubleEnumMap(key: String, getAlternative: () -> MutableMap<K, V>): MutableMap<K, V> =
        retrieveDoubleEnumMapOrNull(key) ?: getAlternative()
    
    /**
     * Retrieves an [EnumMap] where the value is an enum as well. If [data]
     * doesn't contains the given key, ``null`` is returned
     */
    inline fun <reified K : Enum<K>, reified V : Enum<V>> retrieveDoubleEnumMapOrNull(key: String): MutableMap<K, V>? {
        val mapElement = data.getElement<EnumMapElement>(key) ?: return null
        return mapElement.toDoubleEnumMap()
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
    inline fun <reified K : Enum<K>, reified V> storeEnumMap(key: String, map: Map<K, V>) {
        val enumMap = if (map is EnumMap) map else map.toEnumMap()
        data.putElement(key, enumMap.toElement(V::class))
    }
    
    /**
     * Serializes [EnumMaps][EnumMap] using CBF and stores them in the [data][CompoundElement]
     * while mapping the values using the given [valueMapper].
     */
    inline fun <reified K : Enum<K>, reified V, reified R> storeEnumMap(key: String, map: Map<K, V>, valueMapper: (V) -> R) {
        val enumMap = map.mapValuesTo(emptyEnumMap<K, R>()) { valueMapper(it.value) }
        data.putElement(key, enumMap.toElement(R::class))
    }
    
    /**
     * Serializes [Collections][Collection] using CBF and stores them in the [data] [CompoundElement]
     */
    inline fun <reified V> storeList(key: String, list: Collection<V>) {
        val listElement = ListElement()
        list.forEach { listElement.add(it) }
        data.putElement(key, listElement)
    }
    
    // endregion
    
}