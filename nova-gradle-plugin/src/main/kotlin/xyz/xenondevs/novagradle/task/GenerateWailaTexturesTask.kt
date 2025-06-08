package xyz.xenondevs.novagradle.task

import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.TaskAction
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.novagradle.util.AddonResourcePack
import xyz.xenondevs.novagradle.util.TaskUtils
import xyz.xenondevs.renderer.MinecraftModelRenderer
import xyz.xenondevs.renderer.model.resource.ZipResourcePack
import java.io.File

abstract class GenerateWailaTexturesTask : DefaultTask() {
    
    @get:Input
    abstract val addonId: Property<String>
    
    @get:InputDirectory
    abstract val resourcesDir: DirectoryProperty
    
    @get:Input
    abstract val filter: Property<(File) -> Boolean>
    
    @TaskAction
    fun run() {
        // download minecraft models and textures
        val mcAssetsDir = File(project.layout.buildDirectory.get().asFile, "mcassets")
        if (!mcAssetsDir.exists()) {
            runBlocking {
                MinecraftAssetsDownloader(outputDirectory = mcAssetsDir, mode = ExtractionMode.MOJANG_API_CLIENT).downloadAssets()
            }
        }
        
        // setup model renderer
        val renderer = MinecraftModelRenderer(
            512, 512,
            128, 128,
            listOf(mcAssetsDir.toPath()),
            true
        )
        
        // add nova resource pack
        renderer.loader.resourcePacks += ZipResourcePack(TaskUtils.findNovaArtifact(project).file.toPath())
        
        // add addon resource pack
        renderer.loader.resourcePacks += AddonResourcePack(resourcesDir.get().asFile, addonId.get())
        
        // render block models
        val wailaTexturesDir = resourcesDir.dir("assets/textures/waila/").get().asFile.apply(File::mkdirs)
        val blockModelsDir = resourcesDir.dir("assets/models/block").get().asFile
        blockModelsDir.walkTopDown()
            .filter { it.isFile && it.extension == "json" }
            .forEach { file ->
                val modelName = file.relativeTo(blockModelsDir.parentFile).path
                    .replace('\\', '/')
                    .removeSuffix(".json")
                val textureName = file.relativeTo(blockModelsDir).path
                    .replace('\\', '/')
                    .replace('/', '_')
                    .substringBeforeLast('.') + ".png"
                val textureFile = File(wailaTexturesDir, textureName)
                
                if (textureFile.exists() || !filter.get().invoke(textureFile))
                    return@forEach
                
                println("Rendering model for $modelName")
                try {
                    renderer.renderModelToFile("${addonId.get()}:$modelName", textureFile)
                } catch (e: Exception) {
                    println("Failed to render model $modelName: ${e.message}")
                }
            }
    }
    
}