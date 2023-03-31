package xyz.xenondevs.nova.tileentity.upgrade

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.reflection.createType
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.material.NovaTileEntityBlock
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.data.serialization.yaml.getDeserialized
import kotlin.reflect.KType

class UpgradeType<T> internal constructor(
    val id: ResourceLocation,
    val item: NovaItem,
    val icon: NovaItem,
    valueType: KType
) : Reloadable {
    
    private val listValueType = createType(List::class, valueType)
    private val valueListProviders = HashMap<NovaTileEntityBlock, ValueListProvider>()
    private val valueProviders = HashMap<NovaTileEntityBlock, HashMap<Int, ValueProvider>>()
    
    fun getValue(material: NovaTileEntityBlock, level: Int): T {
        val values = getValueList(material)
        return values[level.coerceIn(0..values.lastIndex)]
    }
    
    fun getValueProvider(material: NovaTileEntityBlock, level: Int): Provider<T> {
        return valueProviders
            .getOrPut(material, ::HashMap)
            .getOrPut(level) { ValueProvider(getValueListProvider(material), level) }
    }
    
    fun getValueList(material: NovaTileEntityBlock): List<T> {
        return getValueListProvider(material).value
    }
    
    fun getValueListProvider(material: NovaTileEntityBlock): Provider<List<T>> {
        return valueListProviders.getOrPut(material) { ValueListProvider(material) }
    }
    
    override fun reload() {
        valueListProviders.values.forEach(Provider<*>::update)
    }
    
    private inner class ValueListProvider(
        private val material: NovaTileEntityBlock
    ) : Provider<List<T>>() {
        
        override fun loadValue(): List<T> {
            return NovaConfig[material].getDeserialized("upgrade_values.${id.name}", listValueType)
                ?: NovaConfig["${id.namespace}:upgrade_values"].getDeserialized(id.name, listValueType)
                ?: throw IllegalStateException("No upgrade values present for $id")
        }
        
    }
    
    private inner class ValueProvider(
        private val listProvider: Provider<List<T>>,
        private val level: Int
    ) : Provider<T>() {
        
        init {
            listProvider.addChild(this)
        }
        
        override fun loadValue(): T {
            val valueList = listProvider.value
            return valueList[level.coerceIn(0..valueList.lastIndex)]
        }
        
    }
    
    companion object {
        
        @Suppress("UNCHECKED_CAST")
        fun <T> of(item: NovaItem): UpgradeType<T>? =
            NovaRegistries.UPGRADE_TYPE.firstOrNull { it.item == item } as? UpgradeType<T>
        
    }
    
}