package xyz.xenondevs.nova.tileentity.upgrade

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.commons.provider.immutable.orElse
import xyz.xenondevs.commons.provider.immutable.requireNonNull
import xyz.xenondevs.commons.reflection.createType
import xyz.xenondevs.nova.data.config.ConfigProvider
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.data.config.optionalEntry
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.name
import kotlin.reflect.KType

class UpgradeType<T> internal constructor(
    val id: ResourceLocation,
    val item: NovaItem,
    val icon: NovaItem,
    valueType: KType
) {
    
    private val listValueType = createType(List::class, valueType)
    private val globalConfig = Configs["${id.namespace}:upgrade_values"]
    private val valueListProviders = HashMap<ConfigProvider, Provider<List<T>>>()
    private val valueProviders = HashMap<ConfigProvider, HashMap<Int, Provider<T>>>()
    
    fun getValue(config: ConfigProvider, level: Int): T =
        getValueProvider(config, level).value
    
    fun getValueProvider(config: ConfigProvider, level: Int): Provider<T> =
        valueProviders
            .getOrPut(config, ::HashMap)
            .getOrPut(level) { getValueListProvider(config).map { list -> list[level.coerceIn(0..list.lastIndex)] } }
    
    fun getValueList(config: ConfigProvider): List<T> =
        getValueListProvider(config).value
    
    fun getValueListProvider(config: ConfigProvider): Provider<List<T>> =
        valueListProviders.getOrPut(config) {
            config.optionalEntry<List<T>>(listValueType, "upgrade_values", id.name)
                .orElse(globalConfig.optionalEntry(listValueType, id.name))
                .requireNonNull("No upgrade values present for $id")
        }
    
    companion object {
        
        @Suppress("UNCHECKED_CAST")
        fun <T> of(item: NovaItem): UpgradeType<T>? =
            NovaRegistries.UPGRADE_TYPE.firstOrNull { it.item == item } as? UpgradeType<T>
        
    }
    
}