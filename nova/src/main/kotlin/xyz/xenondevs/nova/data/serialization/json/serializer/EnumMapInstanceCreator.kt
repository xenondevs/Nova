package xyz.xenondevs.nova.data.serialization.json.serializer

import com.google.gson.InstanceCreator
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ENUM_MAP_CONSTRUCTOR
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

internal object EnumMapInstanceCreator : InstanceCreator<EnumMap<*, *>> {
    
    // https://stackoverflow.com/questions/54966118/how-to-deserialize-an-enummap
    override fun createInstance(type: Type): EnumMap<*, *> {
        return ENUM_MAP_CONSTRUCTOR.newInstance((type as ParameterizedType).actualTypeArguments[0] as Class<*>)
    }
    
}