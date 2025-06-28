package xyz.xenondevs.nova.resources

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

internal object AutoCopier {
    
    private val copyDestinations: List<String> by MAIN_CONFIG.entry("resource_pack", "auto_copy")
    
    fun copyToDestinations(file: Path) {
        for (destination in copyDestinations) {
            try {
                if (Path(destination).isDirectory()) {
                    file.copyTo(
                        Path(destination, file.name),
                        overwrite = true
                    )
                } else {
                    val destPath = Path(destination.format(file.nameWithoutExtension))
                    destPath.createParentDirectories()
                    file.copyTo(destPath, overwrite = true)
                }
            } catch(e: Exception) {
                LOGGER.warn("Failed to copy resource pack file '${file.name}' to destination '$destination'.", e)
            }
        }
    }
    
}