package xyz.xenondevs.nova.addon.assets

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class AssetsPack(val directory: File) {
    
    val namespace = directory.name
    val assetsFile = File(directory, "assets.json")
    
    // Sub-folders
    val modelsDir = File(directory, "models").takeIf(File::exists)
    val texturesDir = File(directory, "textures").takeIf(File::exists)
    val fontsDir = File(directory, "fonts").takeIf(File::exists)
    val soundsDir = File(directory, "sounds").takeIf(File::exists)
    val langDir = File(directory, "lang").takeIf(File::exists)
    val guisDir = File(directory, "guis").takeIf(File::exists)
    
    /**
     * A map containing all nova materials as keys with their corresponding item and block modeldata
     */
    val assetsIndex: List<RegisteredMaterial>
    
    init {
        if (!assetsFile.exists())
            throw IllegalArgumentException("Asset Pack does not contain assets.json")
        
        assetsIndex = AssetsIndexDeserializer.deserializeAssetsIndex(namespace, JsonParser.parseReader(assetsFile.reader()) as JsonObject)
    }
    
}