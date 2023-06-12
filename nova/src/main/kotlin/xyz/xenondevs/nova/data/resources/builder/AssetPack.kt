@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.xenondevs.nova.data.resources.builder

import net.minecraft.resources.ResourceLocation
import it.unimi.dsi.fastutil.ints.IntSet
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.RegisteredArmor
import xyz.xenondevs.nova.data.resources.builder.content.material.info.RegisteredMaterial
import xyz.xenondevs.nova.data.resources.builder.index.ArmorIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.index.GuisIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.index.MaterialsIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.index.MovedFontsIndexDeserializer
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.CopyActionResult
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.inputStream
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream
import kotlin.io.path.relativeTo

class AssetPack(val namespace: String, val assetsDir: Path) {
    
    val modelsDir: Path? = assetsDir.resolve("models/").takeIf(Path::exists)
    val texturesDir: Path? = assetsDir.resolve("textures/").takeIf(Path::exists)
    val fontsDir: Path? = assetsDir.resolve("fonts/").takeIf(Path::exists)
    val langDir: Path? = assetsDir.resolve("lang/").takeIf(Path::exists)
    val soundsDir: Path? = assetsDir.resolve("sounds/").takeIf(Path::exists)
    val soundsFile: Path? = assetsDir.resolve("sounds.json").takeIf(Path::exists)
    val wailaTexturesDir: Path? = assetsDir.resolve("textures/waila/").takeIf(Path::exists)
    val atlasesDir: Path? = assetsDir.resolve("atlases/").takeIf(Path::exists)
    
    internal val materialsIndex: List<RegisteredMaterial>? = assetsDir.resolve("materials.json")
        .takeIf(Path::exists)
        ?.let { MaterialsIndexDeserializer.deserialize(namespace, it.parseJson()) }
    
    internal val guisIndex: Map<ResourceLocation, ResourcePath>? = assetsDir.resolve("guis.json")
        .takeIf(Path::exists)
        ?.let { GuisIndexDeserializer.deserialize(namespace, it.parseJson()) }
    
    internal val armorIndex: List<RegisteredArmor>? = assetsDir.resolve("armor.json")
        .takeIf(Path::exists)
        ?.let { ArmorIndexDeserializer.deserialize(namespace, it.parseJson()) }
    
    internal val movedFontsIndex: Map<ResourcePath, IntSet>? = assetsDir.resolve("moved_fonts.json")
        .takeIf(Path::exists)
        ?.let { MovedFontsIndexDeserializer.deserialize(namespace, it.parseJson()) }
    
    fun getInputStream(path: String): InputStream? =
        assetsDir.resolve(path).takeIf(Path::exists)?.inputStream()
    
    internal fun extract(namespaceDir: Path, fileFilter: (String) -> Boolean) {
        fun extractDir(sourceDir: Path, dirName: String) {
            sourceDir.copyToRecursively(
                target = namespaceDir.resolve("$dirName/"),
                followLinks = false,
            ) { source, target ->
                if (source.isDirectory())
                    return@copyToRecursively CopyActionResult.CONTINUE
                
                val relPath = source.relativeTo(sourceDir).invariantSeparatorsPathString
                if (!fileFilter(relPath))
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
        
        texturesDir?.let { extractDir(it, "textures") }
        modelsDir?.let { extractDir(it, "models") }
        fontsDir?.let { extractDir(it, "font") }
        soundsDir?.let { extractDir(it, "sounds") }
        soundsFile?.copyTo(namespaceDir.resolve("sounds.json"))
    }
    
}