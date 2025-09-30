package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.PackMcMeta
import xyz.xenondevs.nova.util.data.writeJson

private val PACK_DESCRIPTION by MAIN_CONFIG.entry<String>("resource_pack", "generation", "description")

/**
 * Generates the `pack.mcmeta` file.
 */
class PackMcMetaTask(private val builder: ResourcePackBuilder) : PackTask {
    
    override suspend fun run() {
        val format = PackMcMeta.PackFormatConstraint(
            ResourcePackBuilder.PACK_MAJOR_VERSION, 
            ResourcePackBuilder.PACK_MINOR_VERSION
        )
        builder.resolve("pack.mcmeta").writeJson(PackMcMeta(
            pack = PackMcMeta.Pack(
                description = PACK_DESCRIPTION.format(builder.id),
                minFormat = format,
                maxFormat = format,
            )
        ))
    }
    
}