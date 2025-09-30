package xyz.xenondevs.nova.resources

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.writeBytes

internal object AutoCopier {
    
    private val copyDestinations: List<String> by MAIN_CONFIG.entry("resource_pack", "auto_copy")
    
    suspend fun copyToDestinations(id: Key, bin: ByteArray) = withContext(Dispatchers.IO) {
        val sanitizedName = id.toString().replace(Regex("[:/]]"), "_")
        for (destination in copyDestinations) {
            try {
                if (Path(destination).isDirectory()) {
                    val destPath = Path(destination, "$sanitizedName.zip")
                    destPath.writeBytes(bin)
                } else {
                    val destPath = Path(destination.format(sanitizedName))
                    destPath.createParentDirectories()
                    destPath.writeBytes(bin)
                }
            } catch(e: Exception) {
                LOGGER.warn("Failed to copy resource pack file '$id' to destination '$destination'.", e)
            }
        }
    }
    
}