package xyz.xenondevs.nova.world.fakeentity.metadata

import kotlin.reflect.KProperty

internal class SharedFlagsMetadataEntry(
    index: Int
) : MetadataEntry<Int>(index, MetadataSerializers.BYTE, 0) {
    
    private val flags = Array(8, ::SharedFlag)
    
    operator fun get(bit: Int) = flags[bit]
    
    inner class SharedFlag(private val bit: Int) {
        
        operator fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
            return (value and (1 shl bit)) != 0
        }
        
        operator fun setValue(thisRef: Any, property: KProperty<*>, newValue: Boolean) {
            value = if (newValue)
                value or (1 shl bit)
            else value and (1 shl bit).inv()
        }
        
    }
    
}