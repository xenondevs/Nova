package xyz.xenondevs.nova.serialization.json.serializer

import com.google.gson.InstanceCreator
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

internal object EnumMapInstanceCreator : InstanceCreator<EnumMap<*, *>> {

    override fun createInstance(type: Type) =
        createEnumMap((type as ParameterizedType).actualTypeArguments[0] as Class<*>)
    
    @Suppress("UNCHECKED_CAST")
    private fun <E : Enum<E>> createEnumMap(clazz: Class<*>) =
        EnumMap<E, Any>(clazz as Class<E>)
    
}