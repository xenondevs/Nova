package xyz.xenondevs.nova.util.data

fun <T> ArrayKey(vararg items: T): ArrayKey<T> = ArrayKey(items)

class ArrayKey<out T>(val array: Array<out T>) {
    
    override fun equals(other: Any?): Boolean =
        other is ArrayKey<*> && array.contentEquals(other.array)
    
    override fun hashCode(): Int =
        array.contentHashCode()
    
}