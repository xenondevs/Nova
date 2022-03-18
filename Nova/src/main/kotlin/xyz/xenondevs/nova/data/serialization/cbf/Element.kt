package xyz.xenondevs.nova.data.serialization.cbf

import de.studiocode.invui.virtualinventory.VirtualInventory
import io.netty.buffer.ByteBuf
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.craftbukkit.v1_18_R2.inventory.CraftItemStack
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.*
import xyz.xenondevs.nova.data.serialization.cbf.element.primitive.*
import xyz.xenondevs.nova.util.reflection.*
import java.awt.Color
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface Element {
    
    fun getTypeId(): Int
    
    fun write(buf: ByteBuf)
    
}

abstract class BackedElement<T> : Element {
    
    abstract val value: T
    
    override fun equals(other: Any?): Boolean {
        return other is BackedElement<*> && value == other.value
    }
    
    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }
    
    override fun toString(): String {
        return value.toString()
    }
    
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
            ItemStack::class to ::ItemStackElement,
            CraftItemStack::class to ::ItemStackElement,
            Location::class to ::LocationElement,
            NamespacedKey::class to ::NamespacedKeyElement,
            CompoundElement::class to ::returnSelf,
            VirtualInventory::class to ::VirtualInventoryElement,
            Color::class to ::ColorElement,
            Pair::class to ::PairElement
        )
        
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T : Any> createElement(value: T): BackedElement<T> {
            if (value is Enum<*>) {
                return StringElement(value.name) as BackedElement<T>
            }
            val constructor = TYPE_TO_ELEMENT[value::class]
                ?: throw IllegalArgumentException("Couldn't find BackedElement type for " + value::class)
            return constructor.call(value) as BackedElement<T>
        }
        
        @Suppress("UNCHECKED_CAST")
        fun <T : Any> createElement(clazz: KClass<T>, value: Any): BackedElement<T> {
            if (value is Enum<*>) {
                return StringElement(value.name) as BackedElement<T>
            }
            val constructor = TYPE_TO_ELEMENT[clazz]
                ?: throw IllegalArgumentException("Couldn't find BackedElement type for $clazz")
            return constructor.call(value) as BackedElement<T>
        }
        
        fun returnSelf(element: BackedElement<*>) = element
        
    }
    
}