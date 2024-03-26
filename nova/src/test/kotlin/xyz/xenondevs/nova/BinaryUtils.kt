package xyz.xenondevs.nova

import xyz.xenondevs.cbf.io.ByteWriter
import java.io.ByteArrayOutputStream

fun byteWriter(write: ByteWriter.() -> Unit): ByteArray {
    val out = ByteArrayOutputStream()
    val writer = ByteWriter.fromStream(out)
    writer.write()
    return out.toByteArray()
}