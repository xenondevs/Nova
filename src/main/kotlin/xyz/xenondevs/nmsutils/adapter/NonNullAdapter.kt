package xyz.xenondevs.nmsutils.adapter

abstract class NonNullAdapter<V, R>(private val defaultValue: R) {
    
    protected abstract fun convert(value: V): R
    
    fun toNMS(value: V?): R {
        return if (value != null) convert(value)
        else defaultValue
    }
    
}