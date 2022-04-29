package xyz.xenondevs.nova.tileentity.upgrade

import com.google.gson.JsonElement
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.material.CoreGUIMaterial
import xyz.xenondevs.nova.material.CoreItems
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.tileentity.upgrade.UpgradeTypeRegistry.register

class UpgradeType<T> internal constructor(
    val id: NamespacedId,
    val item: ItemNovaMaterial,
    val icon: ItemNovaMaterial,
    val configLoader: (JsonElement) -> T
) {
    
    private val modifierCache = HashMap<ItemNovaMaterial, List<T>>()
    private val defaultConfig by lazy { NovaConfig["${id.namespace}:upgrade_values"] }
    
    fun getValue(material: ItemNovaMaterial, level: Int): T {
        val values = getUpgradeValues(material)
        return values[level.coerceIn(0..values.lastIndex)]
    }
    
    fun getUpgradeValues(material: ItemNovaMaterial): List<T> {
        return modifierCache.getOrPut(material) {
            val specificConfig = NovaConfig[material]
            
            val jsonArray = specificConfig.getArray("upgrade_values.${id.name}") ?: defaultConfig.getArray(id.name)
            checkNotNull(jsonArray) { "No upgrade values present for $id" }
            
            val list = ArrayList<T>()
            jsonArray.forEach { list += configLoader(it) }
            
            return@getOrPut list
        }
    }
    
    companion object {
        val SPEED = register("speed", CoreItems.SPEED_UPGRADE, CoreGUIMaterial.SPEED_UPGRADE) { it.asDouble }
        val EFFICIENCY = register("efficiency", CoreItems.EFFICIENCY_UPGRADE, CoreGUIMaterial.EFFICIENCY_UPGRADE) { it.asDouble }
        val ENERGY = register("energy", CoreItems.ENERGY_UPGRADE, CoreGUIMaterial.ENERGY_UPGRADE) { it.asDouble }
        val FLUID = register("fluid", CoreItems.FLUID_UPGRADE, CoreGUIMaterial.FLUID_UPGRADE) { it.asDouble }
        val RANGE = register("range", CoreItems.RANGE_UPGRADE, CoreGUIMaterial.RANGE_UPGRADE) { it.asInt }
        
        internal fun init() = Unit
    }
    
    
}