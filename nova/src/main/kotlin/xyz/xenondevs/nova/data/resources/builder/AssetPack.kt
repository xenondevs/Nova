@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.xenondevs.nova.data.resources.builder

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.index.ArmorIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.index.GuisIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.index.MaterialsIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.task.armor.info.RegisteredArmor
import xyz.xenondevs.nova.data.resources.builder.task.material.info.RegisteredMaterial
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream

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
    
    fun getInputStream(path: String): InputStream? =
        assetsDir.resolve(path).takeIf(Path::exists)?.inputStream()
    
}