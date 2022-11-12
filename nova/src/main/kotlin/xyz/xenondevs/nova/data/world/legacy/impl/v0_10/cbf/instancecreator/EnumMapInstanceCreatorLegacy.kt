package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.instancecreator

import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.InstanceCreatorLegacy
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.ENUM_MAP_CONSTRUCTOR
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

internal object EnumMapInstanceCreatorLegacy : InstanceCreatorLegacy<EnumMap<*, *>> {
    
    override fun createInstance(type: Type): EnumMap<*, *> {
        return ENUM_MAP_CONSTRUCTOR.newInstance((type as ParameterizedType).actualTypeArguments[0] as Class<*>)
    }
    
}