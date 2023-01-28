package xyz.xenondevs.nova.tileentity.upgrade

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.material.ItemNovaMaterial
import kotlin.reflect.KType
import kotlin.reflect.typeOf

@Suppress("UNCHECKED_CAST")
object UpgradeTypeRegistry {
    
    private val _types = HashMap<NamespacedId, UpgradeType<*>>()
    val types: Collection<UpgradeType<*>>
        get() = _types.values
    
    fun <T> register(
        addon: Addon, name: String,
        item: ItemNovaMaterial, icon: ItemNovaMaterial,
        valueType: KType
    ): UpgradeType<T> {
        val id = NamespacedId(addon.description.id, name)
        val type = UpgradeType<T>(id, item, icon, valueType)
        _types[id] = type
        return type
    }
    
    inline fun <reified T> register(
        addon: Addon, name: String,
        item: ItemNovaMaterial, icon: ItemNovaMaterial
    ) = register<T>(addon, name, item, icon, typeOf<T>())
    
    fun <T> of(id: NamespacedId): UpgradeType<T>? =
        _types[id] as? UpgradeType<T>
    
    fun <T> of(item: ItemNovaMaterial): UpgradeType<T>? =
        types.firstOrNull { it.item == item } as? UpgradeType<T>
    
}