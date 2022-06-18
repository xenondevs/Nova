package xyz.xenondevs.nova.data.serialization.cbf.instancecreator

import xyz.xenondevs.nova.data.serialization.cbf.InstanceCreator
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

internal object EnumMapInstanceCreator : InstanceCreator<EnumMap<*, *>> {
    
    private val ENUM_MAP_CONSTRUCTOR = EnumMap::class.java.getConstructor(Class::class.java)
    
    override fun createInstance(type: Type): EnumMap<*, *> {
        return ENUM_MAP_CONSTRUCTOR.newInstance((type as ParameterizedType).actualTypeArguments[0] as Class<*>)
    }
    
}