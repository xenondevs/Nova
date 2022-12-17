@file:Suppress("MemberVisibilityCanBePrivate", "unused", "CanBeParameter")

package xyz.xenondevs.nova.data.resources.builder

import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.FileHeader
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.content.armor.info.RegisteredArmor
import xyz.xenondevs.nova.data.resources.builder.content.material.info.RegisteredMaterial
import xyz.xenondevs.nova.data.resources.builder.index.ArmorIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.index.GUIsIndexDeserializer
import xyz.xenondevs.nova.data.resources.builder.index.MaterialsIndexDeserializer
import xyz.xenondevs.nova.util.data.get
import xyz.xenondevs.nova.util.data.parseJson
import java.io.InputStream

internal class AssetPack(val namespace: String, val zip: ZipFile, val assetsPath: String) {
    
    val modelsDir: FileHeader? = zip[assetsPath, "models/"]
    val texturesDir: FileHeader? = zip[assetsPath, "textures/"]
    val fontsDir: FileHeader? = zip[assetsPath, "fonts/"]
    val langDir: FileHeader? = zip[assetsPath, "lang/"]
    val soundsDir: FileHeader? = zip[assetsPath, "sounds/"]
    val soundsFile: FileHeader? = zip[assetsPath, "sounds.json"]
    val wailaTexturesDir: FileHeader? = zip[assetsPath, "textures/waila/"]
    val atlasesDir: FileHeader? = zip[assetsPath, "atlases/"]
    
    val materialsIndex: List<RegisteredMaterial>? = zip[assetsPath, "materials.json"]
        ?.let { MaterialsIndexDeserializer.deserialize(namespace, zip.getInputStream(it).parseJson()) }
    
    val guisIndex: Map<String, ResourcePath>? = zip[assetsPath, "guis.json"]
        ?.let { GUIsIndexDeserializer.deserialize(namespace, zip.getInputStream(it).parseJson()) }
    
    val armorIndex: List<RegisteredArmor>? = zip[assetsPath, "armor.json"]
        ?.let { ArmorIndexDeserializer.deserialize(namespace, zip.getInputStream(it).parseJson()) }
    
    fun getFileHeader(path: String): FileHeader? =
        zip[assetsPath, path]
    
    fun getInputStream(path: String): InputStream? =
        getFileHeader(path)?.let(zip::getInputStream)
    
}