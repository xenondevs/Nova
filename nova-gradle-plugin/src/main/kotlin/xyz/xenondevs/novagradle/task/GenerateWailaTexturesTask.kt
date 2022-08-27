package xyz.xenondevs.novagradle.task

import kotlinx.coroutines.runBlocking
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import xyz.xenondevs.downloader.ExtractionMode
import xyz.xenondevs.downloader.MinecraftAssetsDownloader
import xyz.xenondevs.novagradle.util.AddonResourcePack
import xyz.xenondevs.renderer.MinecraftModelRenderer
import xyz.xenondevs.renderer.model.resource.ZipResourcePack
import java.io.File

abstract class GenerateWailaTexturesTask : DefaultTask() {
    
    @get:Input
    abstract val novaVersion: Property<String>
    
    @get:Input
    abstract val addonId: Property<String>
    
    @get:Input
    abstract val resourcesDir: Property<String>
    
    @get:Input
    abstract val filter: Property<(File) -> Boolean>
    
    @TaskAction
    fun run() {
        // download minecraft models and textures
        val mcAssetsDir = File(project.buildDir, "mcassets")
        if (!mcAssetsDir.exists()) {
            runBlocking {
                MinecraftAssetsDownloader(outputDirectory = mcAssetsDir, mode = ExtractionMode.CLIENT).downloadAssets()
            }
        }
        
        // setup model renderer
        val renderer = MinecraftModelRenderer(
            512, 512,
            128, 128,
            listOf(mcAssetsDir),
            true
        )
        
        // add nova resource pack
        val novaJar = project.configurations.detachedConfiguration(
            project.dependencies.create("xyz.xenondevs.nova:nova:${novaVersion.get()}")
        ).files.first()
        renderer.loader.resourcePacks += ZipResourcePack(novaJar)
        
        // add addon resource pack
        renderer.loader.resourcePacks += AddonResourcePack(project, addonId.get())
        
        // render block models
        val wailaTexturesDir = File(resourcesDir.get(), "assets/textures/waila/").apply(File::mkdirs)
        val blockModelsDir = File(resourcesDir.get(), "assets/models/block/")
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