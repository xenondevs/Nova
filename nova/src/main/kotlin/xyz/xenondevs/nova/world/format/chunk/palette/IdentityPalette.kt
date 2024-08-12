package xyz.xenondevs.nova.world.format.chunk.palette

import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.format.IdResolver

internal class IdentityPalette<T>(private val idResolver: IdResolver<T>) : Palette<T> {
    
    override val size: Int
        get() = idResolver.size
    
    override fun write(writer: ByteWriter) = Unit
    override fun getValue(id: Int): T? = idResolver.fromId(id)
    override fun getId(value: T?): Int = idResolver.toId(value)
    override fun putValue(value: T): Int = throw UnsupportedOperationException()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as IdentityPalette<*>
        
        return idResolver == other.idResolver
    }
    
    override fun hashCode(): Int {
        return idResolver.hashCode()
    }
    
}