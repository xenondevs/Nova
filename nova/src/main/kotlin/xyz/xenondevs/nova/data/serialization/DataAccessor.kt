package xyz.xenondevs.nova.data.serialization

import xyz.xenondevs.nova.data.provider.MutableProvider

class DataAccessor<T>(
    private val holder: DataHolder,
    private val key: String,
    private val global: Boolean,
    private val initialValue: T,
) : MutableProvider<T>() {
    
    override fun loadValue(): T {
        return initialValue
    }
    
    override fun setValue(value: T) {
        this.value = value
    }
    
    fun save() {
        holder.storeData(key, value, global)
    }
    
}