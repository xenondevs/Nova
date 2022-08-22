package xyz.xenondevs.nova.data.serialization

import kotlin.reflect.KProperty

interface DataAccessor<T> {
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T 
    
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T)
    
    fun save()
    
}

class NonNullDataAccessor<T>(
    private val holder: DataHolder,
    private val key: String,
    private val global: Boolean,
    private var value: T
) : DataAccessor<T> {
    
    
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return value
    }
    
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
    
    override fun save() {
        holder.storeData(key, value, global)
    }
    
}

class NullableDataAccessor<T>(
    private val holder: DataHolder,
    private val key: String,
    private val global: Boolean,
    private var value: T?
): DataAccessor<T?> {
    
    override operator fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        return value
    }
    
    override operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        this.value = value
    }
    
    override fun save() {
        holder.storeData(key, value, global)
    }
    
}