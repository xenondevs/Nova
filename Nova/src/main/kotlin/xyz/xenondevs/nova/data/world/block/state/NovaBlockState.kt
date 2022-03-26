package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.world.block.property.BlockProperty
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.material.BlockNovaMaterial

@Suppress("LeakingThis")
open class NovaBlockState(open val material: BlockNovaMaterial) : BlockState {
    
    override val id = material.id
    private val properties = material.properties.associateWithTo(LinkedHashMap(), BlockPropertyType<*>::create)
    
    @Suppress("UNCHECKED_CAST")
    fun <T : BlockProperty> getProperty(type: BlockPropertyType<T>): T? = properties[type] as? T
    
    override fun read(buf: ByteBuf) {
        properties.values.forEach { it.read(buf) }
    }
    
    override fun write(buf: ByteBuf) {
        properties.values.forEach { it.write(buf) }
    }
    
}