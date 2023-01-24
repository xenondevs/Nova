package xyz.xenondevs.nova.tileentity.upgrade

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.reflection.ParameterizedTypeImpl
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.Reloadable
import xyz.xenondevs.nova.data.serialization.yaml.getLazilyEvaluated
import xyz.xenondevs.nova.material.ItemNovaMaterial
import java.lang.reflect.Type

class UpgradeType<T> internal constructor(
    val id: NamespacedId,
    val item: ItemNovaMaterial,
    val icon: ItemNovaMaterial,
    valueType: Type
) : Reloadable {
    
    private val listValueType = ParameterizedTypeImpl(null, List::class.java, valueType)
    private val valueListProviders = HashMap<ItemNovaMaterial, ValueListProvider>()
    private val valueProviders = HashMap<ItemNovaMaterial, HashMap<Int, ValueProvider>>()
    
    fun getValue(material: ItemNovaMaterial, level: Int): T {
        val values = getValueList(material)
        return values[level.coerceIn(0..values.lastIndex)]
    }
    
    fun getValueProvider(material: ItemNovaMaterial, level: Int): Provider<T> {
        return valueProviders
            .getOrPut(material, ::HashMap)
            .getOrPut(level) { ValueProvider(getValueListProvider(material), level) }
    }
    
    fun getValueList(material: ItemNovaMaterial): List<T> {
        return getValueListProvider(material).value
    }
    
    fun getValueListProvider(material: ItemNovaMaterial): Provider<List<T>> {
        return valueListProviders.getOrPut(material) { ValueListProvider(material) }
    }
    
    override fun reload() {
        valueListProviders.values.forEach(Provider<*>::update)
    }
    
    private inner class ValueListProvider(
        private val material: ItemNovaMaterial
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