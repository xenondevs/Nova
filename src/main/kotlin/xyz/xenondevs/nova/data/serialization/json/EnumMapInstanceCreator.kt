package xyz.xenondevs.nova.data.serialization.json

import com.google.gson.InstanceCreator
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

object EnumMapInstanceCreator : InstanceCreator<EnumMap<*, *>> {
    
    private val ENUM_MAP_CONSTRUCTOR = EnumMap::class.java.getConstructor(Class::class.java)
    
    // https://stackoverflow.com/questions/54966118/how-to-deserialize-an-enummap
    override fun createInstance(type: Type): EnumMap<*, *> {
        return ENUM_MAP_CONSTRUCTOR.newInstance((type as ParameterizedType).actualTypeArguments[0] as Class<*>)
    }
    
}