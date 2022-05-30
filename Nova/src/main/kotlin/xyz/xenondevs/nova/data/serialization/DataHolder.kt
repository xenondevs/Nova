package xyz.xenondevs.nova.data.serialization

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.Compound
import xyz.xenondevs.nova.tileentity.TileEntity

abstract class DataHolder(includeGlobal: Boolean) {
    
    abstract val data: Compound
    val globalData: Compound by lazy {
        val global = data.get<Compound>("global")
        global ?: Compound().also { if (includeGlobal) data["global"] = it }
    }
    
    /**
     * Retrieves data from the data [Compound] of this TileEntity.
     * If it can't find anything under the given key, the result of the
     * [getAlternative] lambda is returned.
     */
    inline fun <reified T> retrieveData(key: String, getAlternative: () -> T): T {
        return retrieveOrNull(key) ?: getAlternative()
    }
    
    /**
     * Retrieves data using CBF deserialization from the data [Compound].
     * If neither [data] nor [globalData] contains the given key, ``null`` is returned
     */
    inline fun <reified T> retrieveOrNull(key: String): T? {
        return data[key] ?: globalData[key]
    }
    
    /**
     * Serializes objects using CBF and stores them under the given key in
     * the data object (Supports Enum constants)
     *
     * @param global If the data should also be stored in the [ItemStack]
     * of this [TileEntity].
     */
    fun storeData(key: String, value: Any?, global: Boolean = false) {
        if (global) {
            require(!data.contains(key)) { "$key is already a non-global value" }
            if (value != null) globalData[key] = value
            else globalData.remove(key)
        } else {
            require(!globalData.contains(key)) { "$key is already a global value" }
            if (value != null) data[key] = value
            else data.remove(key)
        }
    }
    
}