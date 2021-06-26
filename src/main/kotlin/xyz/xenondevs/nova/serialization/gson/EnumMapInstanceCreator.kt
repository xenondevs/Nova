package xyz.xenondevs.nova.serialization.gson

import com.google.gson.InstanceCreator
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.util.*

private val ENUMMAP_CONSTRUCTOR = EnumMap::class.java.getConstructor(Class::class.java)

class EnumMapInstanceCreator : InstanceCreator<EnumMap<*, *>> {
    
    // https://stackoverflow.com/questions/54966118/how-to-deserialize-an-enummap
    override fun createInstance(type: Type): EnumMap<*, *> {
        return ENUMMAP_CONSTRUCTOR.newInstance((type as ParameterizedType).actualTypeArguments[0] as Class<*>)
    }
    
}