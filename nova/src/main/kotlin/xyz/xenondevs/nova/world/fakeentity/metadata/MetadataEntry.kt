package xyz.xenondevs.nova.world.fakeentity.metadata

import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.syncher.EntityDataSerializer
import net.minecraft.network.syncher.EntityDataSerializers
import java.util.*
import kotlin.reflect.KProperty

internal interface MetadataEntry<T> {
    
    var dirty: Boolean
    
    fun write(buf: FriendlyByteBuf)
    
    fun isNotDefault(): Boolean
    
}

internal open class NonNullMetadataEntry<T : Any>(
    private val index: Int,
    private val serializer: EntityDataSerializer<T>,
    private val default: T
) : MetadataEntry<T> {
    
    private val serializerId = EntityDataSerializers.getSerializedId(serializer)
    
    protected var value: T = default
    override var dirty: Boolean = false
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return value
    }
    
    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
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

internal class MappedNonNullMetadataEntry<T, R>(
    private val index: Int,
    private val serializer: EntityDataSerializer<R>,
    private val toRaw: (T) -> R,
    private val fromRaw: (R) -> T,
    private val default: T
) : MetadataEntry<T> {
    
    private val serializerId = EntityDataSerializers.getSerializedId(serializer)
    
    private var mappedValue: T = default
    private var rawValue: R = toRaw(default)
    
    override var dirty: Boolean = false
    
    val rawDelegate = RawDelegate()
    val mappedDelegate = MappedDelegate()
    
    override fun write(buf: FriendlyByteBuf) {
        buf.writeByte(index)
        buf.writeVarInt(serializerId)
        serializer.write(buf, rawValue!!)
    }
    
    override fun isNotDefault(): Boolean =
        mappedValue != default
    
    inner class RawDelegate {
        
        operator fun getValue(thisRef: Any, property: KProperty<*>): R {
            return rawValue
        }
        
        operator fun setValue(thisRef: Any, property: KProperty<*>, value: R) {
            rawValue = value
            mappedValue = fromRaw(value)
            dirty = true
        }
        
    }
    
    inner class MappedDelegate {
        
        operator fun getValue(thisRef: Any, property: KProperty<*>): T {
            return mappedValue
        }
        
        operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
            this@MappedNonNullMetadataEntry.mappedValue = value
            rawValue = toRaw(value)
            dirty = true
        }
        
    }
    
}

internal class NullableMetadataEntry<T>(
    private val index: Int,
    private val serializer: EntityDataSerializer<Optional<T & Any>>,
    private val default: T?
) : MetadataEntry<T?> {
    
    private val serializerId = EntityDataSerializers.getSerializedId(serializer)
    
    private var value: T? = default
    override var dirty: Boolean = false
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): T? {
        return value
    }
    
    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T?) {
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