package xyz.xenondevs.nova.data.serialization.cbf.adapter

import io.netty.buffer.ByteBuf
import xyz.xenondevs.nova.data.serialization.cbf.BinaryAdapter
import java.lang.reflect.Type

internal object ByteBinaryAdapter : BinaryAdapter<Byte> {
    
    override fun write(obj: Byte, buf: ByteBuf) {
        buf.writeByte(obj.toInt())
    }
    
    override fun read(type: Type, buf: ByteBuf): Byte {
        return buf.readByte()
    }
    
}

internal object ByteArrayBinaryAdapter : BinaryAdapter<ByteArray> {
    
    override fun write(obj: ByteArray, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach { buf.writeByte(it.toInt()) }
    }
    
    override fun read(type: Type, buf: ByteBuf): ByteArray {
        return ByteArray(buf.readInt()) { buf.readByte() }
    }
    
}

internal object ShortBinaryAdapter : BinaryAdapter<Short> {
    
    override fun write(obj: Short, buf: ByteBuf) {
        buf.writeShort(obj.toInt())
    }
    
    override fun read(type: Type, buf: ByteBuf): Short {
        return buf.readShort()
    }
    
}

internal object ShortArrayBinaryAdapter : BinaryAdapter<ShortArray> {
    
    override fun write(obj: ShortArray, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach { buf.writeShort(it.toInt()) }
    }
    
    override fun read(type: Type, buf: ByteBuf): ShortArray {
        return ShortArray(buf.readInt()) { buf.readShort() }
    }
    
}

internal object IntBinaryAdapter : BinaryAdapter<Int> {
    
    override fun write(obj: Int, buf: ByteBuf) {
        buf.writeInt(obj)
    }
    
    override fun read(type: Type, buf: ByteBuf): Int {
        return buf.readInt()
    }
    
}

internal object IntArrayBinaryAdapter : BinaryAdapter<IntArray> {
    
    override fun write(obj: IntArray, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach(buf::writeInt)
    }
    
    override fun read(type: Type, buf: ByteBuf): IntArray {
        return IntArray(buf.readInt()) { buf.readInt() }
    }
    
}

internal object LongBinaryAdapter : BinaryAdapter<Long> {
    
    override fun write(obj: Long, buf: ByteBuf) {
        buf.writeLong(obj)
    }
    
    override fun read(type: Type, buf: ByteBuf): Long {
        return buf.readLong()
    }
    
}

internal object LongArrayBinaryAdapter : BinaryAdapter<LongArray> {
    
    override fun write(obj: LongArray, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach(buf::writeLong)
    }
    
    override fun read(type: Type, buf: ByteBuf): LongArray {
        return LongArray(buf.readInt()) { buf.readLong() }
    }
    
}

internal object FloatBinaryAdapter : BinaryAdapter<Float> {
    
    override fun write(obj: Float, buf: ByteBuf) {
        buf.writeFloat(obj)
    }
    
    override fun read(type: Type, buf: ByteBuf): Float {
        return buf.readFloat()
    }
    
}

internal object FloatArrayBinaryAdapter : BinaryAdapter<FloatArray> {
    
    override fun write(obj: FloatArray, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach(buf::writeFloat)
    }
    
    override fun read(type: Type, buf: ByteBuf): FloatArray {
        return FloatArray(buf.readInt()) { buf.readFloat() }
    }
    
}

internal object DoubleBinaryAdapter : BinaryAdapter<Double> {
    
    override fun write(obj: Double, buf: ByteBuf) {
        buf.writeDouble(obj)
    }
    
    override fun read(type: Type, buf: ByteBuf): Double {
        return buf.readDouble()
    }
    
}

internal object DoubleArrayBinaryAdapter : BinaryAdapter<DoubleArray> {
    
    override fun write(obj: DoubleArray, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach(buf::writeDouble)
    }
    
    override fun read(type: Type, buf: ByteBuf): DoubleArray {
        return DoubleArray(buf.readInt()) { buf.readDouble() }
    }
    
}

internal object BooleanBinaryAdapter : BinaryAdapter<Boolean> {
    
    override fun write(obj: Boolean, buf: ByteBuf) {
        buf.writeBoolean(obj)
    }
    
    override fun read(type: Type, buf: ByteBuf): Boolean {
        return buf.readBoolean()
    }
    
}

internal object BooleanArrayBinaryAdapter : BinaryAdapter<BooleanArray> {
    
    override fun write(obj: BooleanArray, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach(buf::writeBoolean)
    }
    
    override fun read(type: Type, buf: ByteBuf): BooleanArray {
        return BooleanArray(buf.readInt()) { buf.readBoolean() }
    }
    
}

internal object CharBinaryAdapter : BinaryAdapter<Char> {
    
    override fun write(obj: Char, buf: ByteBuf) {
        buf.writeChar(obj.code)
    }
    
    override fun read(type: Type, buf: ByteBuf): Char {
        return buf.readChar()
    }
    
}

internal object CharArrayBinaryAdapter : BinaryAdapter<CharArray> {
    
    override fun write(obj: CharArray, buf: ByteBuf) {
        buf.writeInt(obj.size)
        obj.forEach { buf.writeChar(it.code) }
    }
    
    override fun read(type: Type, buf: ByteBuf): CharArray {
        return CharArray(buf.readInt()) { buf.readChar() }
    }
    
}