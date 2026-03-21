package xyz.xenondevs.nova.world.item

import net.kyori.adventure.key.Key
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentType
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.registry.NovaRegistries

// this delegating structure is necessary to allow config- and registry reloading
internal class NovaDataComponentMap(
    private val novaKey: Key,
    private val fallback: DataComponentMap
) : DataComponentMap {
    
    private val optionalNovaItem: Provider<NovaItem?> =
        NovaRegistries.ITEM.getOptional(novaKey).flatten()
    
    override fun <T : Any> get(type: DataComponentType<out T>): T? {
        val novaItem = optionalNovaItem.get()
        if (novaItem != null) {
            try {
                return novaItem.baseDataComponents.handle.get(type)
            } catch (e: Exception) {
                LOGGER.error("Failed to retrieve base data components for $novaItem", e)
            }
        } else {
            return fallback.get(type)
        }
        
        return null
    }
    
    override fun keySet(): Set<DataComponentType<*>> {
        val novaItem = optionalNovaItem.get()
        if (novaItem != null) {
            try {
                return novaItem.baseDataComponents.handle.keySet()
            } catch (e: Exception) {
                LOGGER.error("Failed to retrieve base data components for $novaItem", e)
            }
        } else {
            return fallback.keySet()
        }
        
        return emptySet()
    }
    
    override fun equals(other: Any?): Boolean =
        this === other || (other is NovaDataComponentMap && novaKey == other.novaKey)
    
    override fun hashCode(): Int = novaKey.hashCode()
    
}