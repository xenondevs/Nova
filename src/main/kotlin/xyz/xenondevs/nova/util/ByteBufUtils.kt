package xyz.xenondevs.nova.util

import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled

fun ByteBuf.writeByte(byte: Byte): ByteBuf = writeByte(byte.toInt())

fun ByteBuf.writeString(string: String): ByteBuf {
    val encoded = string.encodeToByteArray()
    require(encoded.size <= 65535) { "String is too large!" }
    writeShort(encoded.size)
    writeBytes(encoded)
    return this
}

fun ByteBuf.readString(): String {
    val bytes = ByteArray(readUnsignedShort())
    readBytes(bytes)
    return bytes.decodeToString()
}

fun ByteBuf.toByteArray(): ByteArray {
    markReaderIndex()
    readerIndex(0)
    val bytes = ByteArray(readableBytes())
    readBytes(bytes)
    resetReaderIndex()
    return bytes
}

fun ByteArray.toByteBuf(allocator: () -> ByteBuf = Unpooled::buffer): ByteBuf {
    val buf = allocator()
    buf.writeBytes(this)
    return buf
}