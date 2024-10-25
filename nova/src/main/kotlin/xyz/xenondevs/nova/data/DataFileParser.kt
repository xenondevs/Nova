package xyz.xenondevs.nova.data

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.ResourcePath
import java.io.File
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk

@InternalInit(stage = InternalInitStage.PRE_WORLD)
internal object DataFileParser {
    
    @InitFun
    private fun extract() {
        UpdatableFile.extractIdNamedFromAllAddons("worldgen")
    }
    
    fun processFiles(dirName: String, fileProcessor: (ResourceLocation, File) -> Unit) {
        for (addon in AddonBootstrapper.addons) {
            addon.dataFolder.toPath().resolve(dirName).walk()
                .filter { it.isRegularFile() && it.extension == "json" && ResourcePath.NON_NAMESPACED_ENTRY.matches(it.name) }
                .forEach { file ->
                    val id = ResourceLocation.fromNamespaceAndPath(addon.id, file.nameWithoutExtension)
                    fileProcessor.invoke(id, file.toFile())
                }
        }
    }
    
}