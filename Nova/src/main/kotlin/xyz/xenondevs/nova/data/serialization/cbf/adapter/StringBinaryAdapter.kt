package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import xyz.xenondevs.nova.util.data.readString
import xyz.xenondevs.nova.util.data.writeString
import java.lang.reflect.Type

internal object StringBinaryAdapter : BinaryAdapter<String> {
    
    override fun write(obj: String, buf: ByteBuf) {
        buf.writeString(obj)
    }
    
    override fun read(type: Type, buf: ByteBuf): String {
        return buf.readString()
    }
    
}

internal object StringArrayBinaryAdapter : BinaryAdapter<Array<String>> {
    
    override fun write(obj: Array<String>, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach(buf::writeString)
    }
    
    override fun read(type: Type, buf: ByteBuf): Array<String> {
        return Array(buf.readInt()) {buf.readString()}
    }
    
}