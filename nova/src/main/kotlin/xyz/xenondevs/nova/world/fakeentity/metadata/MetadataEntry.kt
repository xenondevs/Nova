package xyz.xenondevs.nova.world.fakeentity.metadata

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import java.util.*
import kotlin.reflect.KProperty

internal interface MetadataEntry<T> {
    
    val value: T
    var dirty: Boolean
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): T
    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T)
    
    fun write(buf: FriendlyByteBuf)
    
    fun isNotDefault(): Boolean
    
}

internal open class NonNullMetadataEntry<T>(
    private val index: Int,
    private val serializer: EntityDataSerializer<T>,
    private val default: T
) : MetadataEntry<T> {
    
    private val serializerId = EntityDataSerializers.getSerializedId(serializer)
    
    override var value: T = default
        protected set
    override var dirty: Boolean = false
    
    override operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return value
    }
    
    override operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
        dirty = true
    }
    
    override fun write(buf: FriendlyByteBuf) {
        buf.writeByte(index)
        buf.writeVarInt(serializerId)
        serializer.write(buf, value)
    }
    
    override fun isNotDefault(): Boolean =
        value != default
    
}

internal class MappedNonNullMetadataEntry<C, T>(
    private val index: Int,
    private val serializer: EntityDataSerializer<T>,
    private val mapper: (C) -> T,
    private val default: C
) : MetadataEntry<C> {
    
    private val serializerId = EntityDataSerializers.getSerializedId(serializer)
    
    override var value: C = default
        private set
    private var mappedValue: T? = null
    
    override var dirty: Boolean = false
    
    override operator fun getValue(thisRef: Any, property: KProperty<*>): C {
        return value
    }
    
    override operator fun setValue(thisRef: Any, property: KProperty<*>, value: C) {
        this.value = value
        dirty = true
        mappedValue = null
    }
    
    override fun write(buf: FriendlyByteBuf) {
        if (mappedValue == null)
            mappedValue = mapper(value)
        
        buf.writeByte(index)
        buf.writeVarInt(serializerId)
        serializer.write(buf, mappedValue!!)
    }
    
    override fun isNotDefault(): Boolean =
        value != default
    
}

internal class NullableMetadataEntry<T>(
    private val index: Int,
    private val serializer: EntityDataSerializer<Optional<T & Any>>,
    private val default: T?
) : MetadataEntry<T?> {
    
    private val serializerId = EntityDataSerializers.getSerializedId(serializer)
    
    override var value: T? = default
        private set
    override var dirty: Boolean = false
    
    override fun getValue(thisRef: Any, property: KProperty<*>): T? {
        return value
    }
    
    override fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
        this.value = value
        dirty = true
    }
    
    override fun write(buf: FriendlyByteBuf) {
        buf.writeByte(index)
        buf.writeVarInt(serializerId)
        serializer.write(buf, Optional.ofNullable(value))
    }
    
    override fun isNotDefault(): Boolean =
        value != default
    
}

internal class SharedFlagsMetadataEntry(
    index: Int
) : NonNullMetadataEntry<Byte>(index, EntityDataSerializers.BYTE, 0.toByte()) {
    
    private val flags = Array(8, ::SharedFlag)
    
    operator fun get(bit: Int) = flags[bit]
    
    fun getState(bit: Int) = flags[bit].booleanValue
    
    fun setState(bit: Int, state: Boolean) {
        flags[bit].booleanValue = state
    }
    
    inner class SharedFlag(private val bit: Int) {

        var booleanValue: Boolean
            get() = (value.toInt() and (1 shl bit)) != 0
            set(booleanValue) {
                val intValue = if (booleanValue)
                    value.toInt() or (1 shl bit)
                else value.toInt() and (1 shl bit).inv()
    
                value = intValue.toByte()
            }
        
        operator fun getValue(thisRef: Any, property: KProperty<*>) = booleanValue
        
        operator fun setValue(thisRef: Any, property: KProperty<*>, newValue: Boolean) {
            booleanValue = newValue
        }
        
    }
    
}