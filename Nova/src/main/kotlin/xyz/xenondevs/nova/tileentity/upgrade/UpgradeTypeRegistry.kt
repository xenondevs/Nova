package xyz.xenondevs.nova.tileentity.upgrade

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.material.ItemNovaMaterial

@Suppress("UNCHECKED_CAST")
object UpgradeTypeRegistry {
    
    private val _types = HashMap<NamespacedId, UpgradeType<*>>()
    val types: Collection<UpgradeType<*>>
        get() = _types.values
    
    init {
        UpgradeType // Loads the default upgrade types
    }
    
    fun <T> register(
        addon: Addon, name: String,
        item: ItemNovaMaterial, icon: ItemNovaMaterial,
        configLoader: (Any) -> T = { it as T }
    ): UpgradeType<T> {
        val id = NamespacedId(addon.description.id, name)
        val type = UpgradeType(id, item, icon, configLoader)
        _types[id] = type
        return type
    }
    
    internal fun <T> register(
        name: String,
        item: ItemNovaMaterial, icon: ItemNovaMaterial,
        configLoader: (Any) -> T = { it as T }
    ): UpgradeType<T> {
        val id = NamespacedId("nova", name)
        val type = UpgradeType(id, item, icon, configLoader)
        _types[id] = type
        return type
    }
    
    fun <T : UpgradeType<*>> of(id: NamespacedId): UpgradeType<*>? =
        _types[id] as? T
    
    fun <T: UpgradeType<*>> of(item: ItemNovaMaterial): UpgradeType<*>? =
        types.firstOrNull { it.item == item } as? T
    
}