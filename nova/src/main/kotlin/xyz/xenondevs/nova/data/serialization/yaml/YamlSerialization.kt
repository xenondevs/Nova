@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.data.serialization.yaml

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.serialization.ConfigurationSerializable
import org.bukkit.configuration.serialization.ConfigurationSerialization
import xyz.xenondevs.cbf.CBF
import xyz.xenondevs.nova.data.serialization.yaml.serializer.PotionEffectSerializer
import xyz.xenondevs.nova.util.reflection.representedKClass
import xyz.xenondevs.nova.util.reflection.type
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf

private val NUMBER_CONVERTER_MAP: Map<KClass<*>, (Number) -> Number> = mapOf(
    Byte::class to { it.toByte() },
    Short::class to { it.toShort() },
    Int::class to { it.toInt() },
    Long::class to { it.toLong() },
    Float::class to { it.toFloat() },
    Double::class to { it.toDouble() }
)

internal fun ConfigurationSection.setLazilyEvaluated(path: String, value: Any) {
    set(path, YamlSerialization.serialize(value) ?: value)
}

internal inline fun <reified T : Any> ConfigurationSection.getLazilyEvaluated(path: String): T? {
    return getLazilyEvaluated(path, type<T>())
}

internal fun <T : Any> ConfigurationSection.getLazilyEvaluated(path: String, type: Type): T? {
    val value = get(path) ?: return null
    return YamlSerialization.deserialize(value, type)
}

internal object YamlSerialization {
    
    private val serializers = HashMap<KClass<*>, YamlSerializer<*>>()
    
    init {
        registerSerializer(PotionEffectSerializer)
    }
    
    inline fun <reified T> registerSerializer(serializer: YamlSerializer<T>) {
        serializers[T::class] = serializer
    }
    
    fun <T : Any> serialize(value: T): Map<String, Any>? {
        val serializer = getSerializer<T>(value::class)
        if (serializer != null) {
            return serializer.serialize(value)
        }
        
        if (value is ConfigurationSerializable) {
            return value.serialize()
        }
        
        return null
    }
    
    inline fun <reified T : Any> deserialize(value: Any): T {
        return deserialize(value, type<T>())
    }
    
    fun <T : Any> deserialize(value: Any, type: Type): T {
        val clazz = type.representedKClass as KClass<T>
        
        // if value is a collection, also deserialize the elements
        if (value is Collection<*> && clazz.isSubclassOf(List::class)) {
            return deserializeCollectionEntries(value, type as ParameterizedType)
        }
        
        // if value is a map, also deserialize the entries
        if (value is Map<*, *> && clazz.isSubclassOf(Map::class)) {
            return deserializeMapEntries(value, type as ParameterizedType)
        }
        
        // value and type are the same
        if (value::class == clazz) {
            return value as T
        }
        
        // value is a different number type
        if (value::class.isSubclassOf(Number::class) && clazz.isSubclassOf(Number::class)) {
            val numberConverter = NUMBER_CONVERTER_MAP[clazz] as (Number) -> T
            return numberConverter.invoke(value as Number)
        }
        
        // value requires deserializer
        if (value is Map<*, *>) {
            value as Map<String, Any>
            
            val novaSerializer = getSerializer<T>(clazz)
            if (novaSerializer != null)
                return novaSerializer.deserialize(value)
            
            if (clazz.isSubclassOf(ConfigurationSerializable::class))
                return ConfigurationSerialization.deserializeObject(value, clazz.java as Class<out ConfigurationSerializable>) as T
        }
        
        // failed to deserialize value
        throw IllegalArgumentException("Value $value cannot be deserialized to type $type")
    }
    
    private fun <T> deserializeCollectionEntries(value: Collection<*>, type: ParameterizedType): T {
        val listType = type.actualTypeArguments[0]
        val dest = CBF.createInstance<MutableCollection<Any?>>(type) ?: ArrayList()
        value.forEach { if (it != null) dest += deserialize<Any>(it, listType) else dest += null }
        return dest as T
    }
    
    private fun <T> deserializeMapEntries(value: Map<*, *>, type: ParameterizedType): T {
        val typeArgs = type.actualTypeArguments
        val keyType = typeArgs[0]
        val valueType = typeArgs[1]
        
        val dest = CBF.createInstance<MutableMap<Any?, Any?>>(type) ?: HashMap()
        value.forEach { (key, value) ->
            val newKey = if (key != null) deserialize<Any>(key, keyType) else null
            val newValue = if (value != null) deserialize<Any>(value, valueType) else null
            
            dest[newKey] = newValue
        }
        
        return dest as T
    }
    
    private fun <T : Any> getSerializer(clazz: KClass<*>): YamlSerializer<T>? {
        return serializers[clazz] as? YamlSerializer<T>
    }
    
}

internal interface YamlSerializer<T> {
    
    fun serialize(value: T): MutableMap<String, Any>
    
    fun deserialize(map: Map<String, Any>): T
    
}