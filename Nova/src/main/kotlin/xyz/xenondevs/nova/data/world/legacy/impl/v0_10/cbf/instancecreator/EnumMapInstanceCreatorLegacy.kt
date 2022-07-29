package xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.instancecreator

import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.cbf.InstanceCreatorLegacy
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

internal object EnumMapInstanceCreatorLegacy : InstanceCreatorLegacy<EnumMap<*, *>> {
    
    private val ENUM_MAP_CONSTRUCTOR = EnumMap::class.java.getConstructor(Class::class.java)
    
    override fun createInstance(type: Type): EnumMap<*, *> {
        return ENUM_MAP_CONSTRUCTOR.newInstance((type as ParameterizedType).actualTypeArguments[0] as Class<*>)
    }
    
}