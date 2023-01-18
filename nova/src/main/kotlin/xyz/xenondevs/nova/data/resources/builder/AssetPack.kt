@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CanBeParameter")

package xyz.xenondevs.nova.data.resources.builder

import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.RegisteredArmor
import xyz.xenondevs.nova.data.resources.builder.content.material.info.RegisteredMaterial
import xyz.xenondevs.nova.data.resources.builder.index.ArmorIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.index.GUIsIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.index.MaterialsIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.index.MovedFontsIndexDeserializer
import xyz.xenondevs.nova.util.data.parseJson
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.CopyActionResult
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo

internal class AssetPack(val namespace: String, val assetsDir: Path) {
    
    val modelsDir: Path? = assetsDir.resolve("models/")
    val texturesDir: Path? = assetsDir.resolve("textures/")
    val fontsDir: Path? = assetsDir.resolve("fonts/")
    val langDir: Path? = assetsDir.resolve("lang/")
    val soundsDir: Path? = assetsDir.resolve("sounds/")
    val soundsFile: Path? = assetsDir.resolve("sounds.json")
    val wailaTexturesDir: Path? = assetsDir.resolve("textures/waila/")
    val atlasesDir: Path? = assetsDir.resolve("atlases/")
    
    val materialsIndex: List<RegisteredMaterial>? = assetsDir.resolve("materials.json")
        .takeIf(Path::exists)
        ?.let { MaterialsIndexDeserializer.deserialize(namespace, it.parseJson()) }
    
    val guisIndex: Map<String, ResourcePath>? = assetsDir.resolve("guis.json")
        .takeIf(Path::exists)
        ?.let { GUIsIndexDeserializer.deserialize(namespace, it.parseJson()) }
    
    val armorIndex: List<RegisteredArmor>? = assetsDir.resolve("armor.json")
        .takeIf(Path::exists)
        ?.let { ArmorIndexDeserializer.deserialize(namespace, it.parseJson()) }
    
    val movedFontsIndex: Map<ResourcePath, Set<Int>>? = assetsDir.resolve("moved_fonts.json")
        .takeIf(Path::exists)
        ?.let { MovedFontsIndexDeserializer.deserialize(namespace, it.parseJson()) }
    
    fun getInputStream(path: String): InputStream? =
        assetsDir.resolve(path).takeIf(Path::exists)?.inputStream()
    
    fun extract(destDir: Path, fileFilter: (String) -> Boolean) {
        val namespaceDir = destDir.resolve(namespace)
        
        fun extractDir(sourceDir: Path, dirName: String) {
            sourceDir.copyToRecursively(
                target = namespaceDir.resolve("$dirName/"),
                followLinks = false,
            ) { source, target ->
                val relPath = source.relativeTo(sourceDir).invariantSeparatorsPathString
                if (!fileFilter(relPath))
                    return@copyToRecursively CopyActionResult.SKIP_SUBTREE
                
                source.inputStream().use { ins ->
                    target.outputStream().use { out ->
                        if (source.extension.equals("png", true))
                            PNGMetadataRemover.remove(ins, out)
                        else ins.transferTo(out)
                    }
                }
                
                return@copyToRecursively CopyActionResult.CONTINUE
            }
        }
        
        texturesDir?.let { extractDir(it, "textures") }
        modelsDir?.let { extractDir(it, "models") }
        fontsDir?.let { extractDir(it, "font") }
        soundsDir?.let { extractDir(it, "sounds") }
        soundsFile?.copyTo(namespaceDir.resolve("sounds.json"))
    }
    
}