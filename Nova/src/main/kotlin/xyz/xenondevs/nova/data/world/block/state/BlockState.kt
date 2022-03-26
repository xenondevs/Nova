package xyz.xenondevs.nova.data.world.block.state

import io.netty.buffer.ByteBuf

sealed interface BlockState {
    
    val id: String
    
    fun read(buf: ByteBuf)
    
    fun write(buf: ByteBuf)
    
}