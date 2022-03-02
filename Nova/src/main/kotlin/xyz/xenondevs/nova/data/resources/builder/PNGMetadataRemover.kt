package xyz.xenondevs.nova.data.resources.builder

import xyz.xenondevs.nova.util.data.transferTo
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

object PNGMetadataRemover {
    
    private const val IDAT_TYPE = 0x49444154
    
    fun remove(input: InputStream, output: OutputStream) {
        val inp = DataInputStream(input)
        val out = DataOutputStream(output)
        inp.transferTo(out, 33)
        processMetadataChunks(inp, out)
        inp.transferTo(out)
        inp.close()
        out.flush()
        out.close()
    }
    
    private fun processMetadataChunks(input: DataInputStream, output: DataOutputStream) {
        while (true) {
            val length = input.readInt()
            val type = input.readInt()
            if (type == IDAT_TYPE) {
                output.writeInt(length)
                output.writeInt(type)
                break
            }
            input.skipBytes(length + 4) // extra 4 bytes for CRC
        }
    }
    
}