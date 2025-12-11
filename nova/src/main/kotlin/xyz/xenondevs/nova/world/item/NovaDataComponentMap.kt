package xyz.xenondevs.nova.world.item

import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentType
import xyz.xenondevs.nova.LOGGER

// this delegating structure is necessary to allow config reloading
internal class NovaDataComponentMap(private val novaItem: NovaItem) : DataComponentMap {
    
    override fun <T : Any> get(type: DataComponentType<out T>): T? {
        try {
            return novaItem.baseDataComponents.handle.get(type)
        } catch (e: Exception) {
            LOGGER.error("Failed to retrieve base data components for $novaItem", e)
        }
        
        return null
    }
    
    override fun keySet(): Set<DataComponentType<*>> {
        try {
            return novaItem.baseDataComponents.handle.keySet()
        } catch (e: Exception) {
            LOGGER.error("Failed to retrieve base data components for $novaItem", e)
        }
        
        return emptySet()
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as NovaDataComponentMap
        
        return novaItem == other.novaItem
    }
    
    override fun hashCode(): Int {
        return novaItem.hashCode()
    }
    
}