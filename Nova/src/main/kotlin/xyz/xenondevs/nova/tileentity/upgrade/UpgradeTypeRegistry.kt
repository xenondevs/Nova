package xyz.xenondevs.nova.tileentity.upgrade

import com.google.gson.JsonElement
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.material.ItemNovaMaterial

object UpgradeTypeRegistry {
    
    private val _types = ArrayList<UpgradeType<*>>()
    val types: List<UpgradeType<*>>
        get() = _types
    
    init {
        UpgradeType.init() // Loads the default upgrade types
    }
    
    fun <T> register(
        addon: Addon, name: String,
        item: ItemNovaMaterial, icon: ItemNovaMaterial,
        configLoader: (JsonElement) -> T
    ): UpgradeType<T> =
        UpgradeType(NamespacedId(addon.description.id, name), item, icon, configLoader).also(_types::add)
    
    internal fun <T> register(
        name: String,
        item: ItemNovaMaterial, icon: ItemNovaMaterial,
        configLoader: (JsonElement) -> T
    ): UpgradeType<T> =
        UpgradeType(NamespacedId("nova", name), item, icon, configLoader).also(_types::add)
    
    fun of(id: String): UpgradeType<*>? =
        types.firstOrNull { it.id.toString() == id }
    
    fun of(item: ItemNovaMaterial): UpgradeType<*>? =
        types.firstOrNull { it.item == item }
    
}