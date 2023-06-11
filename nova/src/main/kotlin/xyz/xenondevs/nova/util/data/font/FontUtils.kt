package xyz.xenondevs.nova.util.data.font

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.util.data.useZip
import java.nio.file.Path
import java.util.*
import kotlin.io.path.extension
import kotlin.io.path.readText
import kotlin.io.path.walk

internal object FontUtils {
    
    val DEFAULT_FONT = ResourcePath("minecraft", "default")
    val DEFAULT_BITMAP_FONT = ResourcePath("minecraft", "include/default")
    
    /**
     * Creates a [Boolean Sequence][Sequence] from the bits of the given [hex string][hexStr].
     */
    fun getBitSequence(hexStr: String): Sequence<Boolean> = sequence {
        for (c in hexStr.chars()) {
            val value = HexFormat.fromHexDigit(c)
            for (i in 0..3) {
                yield(value shr (3 - i) and 1 == 1)
            }
        }
    }
    
    fun readUnihexFiles(zipFile: Path): List<String> =
        zipFile.useZip { zip ->
            zip.walk()
                .filter { it.extension == "hex" }
                .mapTo(ArrayList(), Path::readText)
        }
    
}