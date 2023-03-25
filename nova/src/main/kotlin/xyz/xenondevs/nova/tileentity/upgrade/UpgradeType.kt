package xyz.xenondevs.nova.tileentity.upgrade

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.reflection.createType
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.data.serialization.yaml.getLazilyEvaluated
import xyz.xenondevs.nova.material.NovaItem
import xyz.xenondevs.nova.util.name
import kotlin.reflect.KType

class UpgradeType<T> internal constructor(
    val id: ResourceLocation,
    val item: NovaItem,
    val icon: NovaItem,
    valueType: KType
) : Reloadable {
    
    private val listValueType = createType(List::class, valueType)
    private val valueListProviders = HashMap<NovaItem, ValueListProvider>()
    private val valueProviders = HashMap<NovaItem, HashMap<Int, ValueProvider>>()
    
    fun getValue(material: NovaItem, level: Int): T {
        val values = getValueList(material)
        return values[level.coerceIn(0..values.lastIndex)]
    }
    
    fun getValueProvider(material: NovaItem, level: Int): Provider<T> {
        return valueProviders
            .getOrPut(material, ::HashMap)
            .getOrPut(level) { ValueProvider(getValueListProvider(material), level) }
    }
    
    fun getValueList(material: NovaItem): List<T> {
        return getValueListProvider(material).value
    }
    
    fun getValueListProvider(material: NovaItem): Provider<List<T>> {
        return valueListProviders.getOrPut(material) { ValueListProvider(material) }
    }
    
    override fun reload() {
        valueListProviders.values.forEach(Provider<*>::update)
    }
    
    private inner class ValueListProvider(
        private val material: NovaItem
    ) : Provider<List<T>>() {
        
        override fun loadValue(): List<T> {
            return NovaConfig[material].getLazilyEvaluated("upgrade_values.${id.name}", listValueType)
                ?: NovaConfig["${id.namespace}:upgrade_values"].getLazilyEvaluated(id.name, listValueType)
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
    
}