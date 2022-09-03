package xyz.xenondevs.nova.world.fakeentity.metadata

import net.minecraft.network.FriendlyByteBuf
import kotlin.reflect.KProperty

internal open class MetadataEntry<T>(
    private val index: Int,
    private val serializer: MetadataSerializer<T>,
    private val default: T
) {
    
    var value = default
        protected set
    var dirty = false
    
    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return value
    }
    
    operator fun setValue(thisRef: Any, property: KProperty<*>, value: T) {
        this.value = value
        dirty = true
    }
    
    fun write(buf: FriendlyByteBuf) {
        buf.writeByte(index)
        serializer.write(value, buf)
    }
    
    fun isNotDefault(): Boolean =
        value != default
    
}