package xyz.xenondevs.nova.serialization.cbf

import io.netty.buffer.ByteBuf
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.serialization.cbf.element.other.ItemStackElement
import xyz.xenondevs.nova.serialization.cbf.element.other.UUIDElement
import xyz.xenondevs.nova.serialization.cbf.element.primitive.*
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface Element {
    
    fun getTypeId(): Byte
    
    fun write(buf: ByteBuf)
    
}

interface BackedElement<T> : Element {
    val value: T
    
    companion object BackedElementRegistry {
        
        val TYPE_TO_ELEMENT: Map<KClass<*>, KFunction<BackedElement<*>>> = mapOf(
            Boolean::class to ::BooleanElement,
            Byte::class to ::ByteElement,
            Int::class to ::IntElement,
            Char::class to ::CharElement,
            Float::class to ::FloatElement,
            Long::class to ::LongElement,
            Double::class to ::DoubleElement,
            String::class to ::StringElement,
            BooleanArray::class to ::BooleanArrayElement,
            IntArray::class to ::IntArrayElement,
            CharArray::class to ::CharArrayElement,
            FloatArray::class to ::FloatArrayElement,
            LongArray::class to ::LongArrayElement,
            DoubleArray::class to ::DoubleArrayElement,
            Array<String>::class to ::StringArrayElement,
            UUID::class to ::UUIDElement,
            ItemStack::class to ::ItemStackElement
        )
        
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T : Any> createElement(value: T): BackedElement<T> {
            return createElement(T::class, value)
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> createElement(clazz: KClass<T>, value: Any): BackedElement<T> {
            if (value is Enum<*>) {
                return StringElement(value.name) as BackedElement<T>
            }
            return TYPE_TO_ELEMENT[clazz]!!.call(value) as BackedElement<T>
        }
        
    }
    
}