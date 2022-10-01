package xyz.xenondevs.nova.world.generation.inject.codec

import com.mojang.serialization.Codec
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.lang.reflect.Field

internal abstract class CodecOverride {
    
    abstract fun replace()
    
    fun replace(field: Field, codec: Codec<*>) =
        ReflectionUtils.setStaticFinalField(field, codec)
    
}