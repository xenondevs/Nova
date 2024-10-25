package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.resources.builder.AssetPack
import xyz.xenondevs.nova.resources.builder.PNGMetadataRemover
import xyz.xenondevs.nova.resources.builder.ResourceFilter
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.useZip
import java.nio.file.Path
import kotlin.io.path.CopyActionResult
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo

class ExtractTask(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    @PackTask
    private fun extractAll() {
        extractMinecraftAssets()
        extractAssetPacks()
    }
    
    private fun extractMinecraftAssets() {
        val filters = builder.getResourceFilters(ResourceFilter.Stage.ASSET_PACK)
        NOVA.novaJar.useZip { zip ->
            zip.resolve("assets/minecraft/")
                .copyToRecursively(
                    ResourcePackBuilder.MINECRAFT_ASSETS_DIR,
                    followLinks = false,
                ) { source, target ->
                    if (source.isDirectory())
                        return@copyToRecursively CopyActionResult.CONTINUE
                    
                    val relPath = target.relativeTo(ResourcePackBuilder.MINECRAFT_ASSETS_DIR)
                    
                    if (!filters.all { filter -> filter.allows("minecraft/$relPath") })
                        return@copyToRecursively CopyActionResult.SKIP_SUBTREE
                    
                    source.inputStream().use { ins ->
                        target.parent.createDirectories()
                        target.outputStream().use { out ->
                            if (source.extension.equals("png", true))
                                PNGMetadataRemover.remove(ins, out)
                            else ins.transferTo(out)
                        }
                    }
                    
                    CopyActionResult.CONTINUE
                }
        }
    }
    
    private fun extractAssetPacks() {
        val filters = builder.getResourceFilters(ResourceFilter.Stage.ASSET_PACK)
        for (pack in builder.assetPacks) {
            val namespace = pack.namespace
            extractAssetPack(pack, ResourcePackBuilder.ASSETS_DIR.resolve(namespace)) { path -> filters.all { it.allows(path) } }
        }
    }
    
    private fun extractAssetPack(assetPack: AssetPack, namespaceDir: Path, fileFilter: (String) -> Boolean) {
        val namespace = assetPack.namespace
        fun extractDir(sourceDir: Path, dirName: String) {
            sourceDir.copyToRecursively(
                target = namespaceDir.resolve("$dirName/"),
                followLinks = false,
            ) { source, target ->
                if (source.isDirectory())
                    return@copyToRecursively CopyActionResult.CONTINUE
                
                val relPath = source.relativeTo(sourceDir).invariantSeparatorsPathString
                if (!fileFilter("$namespace/$dirName/$relPath"))
                    return@copyToRecursively CopyActionResult.SKIP_SUBTREE
                
                source.inputStream().use { ins ->
                    target.parent.createDirectories()
                    target.outputStream().use { out ->
                        if (source.extension.equals("png", true))
                            PNGMetadataRemover.remove(ins, out)
                        else ins.transferTo(out)
                    }
                }
                
                return@copyToRecursively CopyActionResult.CONTINUE
            }
        }
        
        assetPack.texturesDir?.let { extractDir(it, "textures") }
        assetPack.fontsDir?.let { extractDir(it, "font") }
        assetPack.soundsDir?.let { extractDir(it, "sounds") }
        assetPack.soundsFile?.copyTo(namespaceDir.resolve("sounds.json"))
    }
    
}