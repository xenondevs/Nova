package xyz.xenondevs.nova.resources.builder

import xyz.xenondevs.nova.util.data.transferTo
import xyz.xenondevs.nova.util.data.use
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream

internal object PNGMetadataRemover {
    
    private const val IDAT_TYPE = 0x49444154
    private val OTHER_NEEDED_CHUNKS = setOf(
        0x504C5445, // PLTE
        0x74524E53, // tRNS
        0x67414D41, // gAMA
    )
    
    fun remove(input: InputStream, output: OutputStream) {
        val inp = DataInputStream(input)
        val out = DataOutputStream(output)
        use(inp, out) {
            try {
                inp.transferTo(out, 33)
                processMetadataChunks(inp, out)
                inp.transferTo(out)
                out.flush()
            } catch (e: EOFException) {
                throw IllegalStateException("Input is not a valid PNG file!", e)
            }
        }
    }
    
    private fun processMetadataChunks(input: DataInputStream, output: DataOutputStream) {
        while (true) {
            val length = input.readInt()
            val type = input.readInt()
            if (type == IDAT_TYPE) {
                output.writeInt(length)
                output.writeInt(type)
                break
            } else if (type in OTHER_NEEDED_CHUNKS) {
                output.writeInt(length)
                output.writeInt(type)
                input.transferTo(output, length)
                output.writeInt(input.readInt()) // CRC
            } else {
                input.skipBytes(length + 4) // extra 4 bytes for CRC
            }
        }
    }
    
}