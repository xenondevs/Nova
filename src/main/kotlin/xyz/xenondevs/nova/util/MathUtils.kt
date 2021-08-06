package xyz.xenondevs.nova.util

val Boolean.intValue: Int
    get() = if (this) 1 else 0

object MathUtils {
    
    fun convertBooleanArrayToInt(array: BooleanArray): Int {
        var i = 0
        for (element in array)
            i = (i shl 1) or if (element) 1 else 0
        
        return i
    }
    
}