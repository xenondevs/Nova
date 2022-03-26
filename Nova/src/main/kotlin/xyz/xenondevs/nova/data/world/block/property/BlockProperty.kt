package xyz.xenondevs.nova.data.world.block.property

import io.netty.buffer.ByteBuf

interface BlockProperty {
    
    fun read(buf: ByteBuf)
    
    fun write(buf: ByteBuf)
    
}

interface BlockPropertyType<T : BlockProperty> {
    
    fun create() : T
    
}