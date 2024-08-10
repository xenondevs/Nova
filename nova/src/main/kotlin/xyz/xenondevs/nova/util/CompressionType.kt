package xyz.xenondevs.nova.util

import com.github.luben.zstd.ZstdInputStream
import com.github.luben.zstd.ZstdOutputStream
import net.jpountz.lz4.LZ4FrameInputStream
import net.jpountz.lz4.LZ4FrameOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

internal enum class CompressionType(
    val wrapInput: (InputStream) -> InputStream,
    val wrapOutput: (OutputStream) -> OutputStream
) {
    
    NONE({ it }, { it }),
    DEFLATE({ InflaterInputStream(it) }, { DeflaterOutputStream(it) }),
    LZ4({ LZ4FrameInputStream(it) }, { LZ4FrameOutputStream(it) }),
    ZSTD({ ZstdInputStream(it) }, { ZstdOutputStream(it) });
    
}