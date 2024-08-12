@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.xenondevs.nova.resources.builder

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
    
    fun getInputStream(path: String): InputStream? =
        assetsDir.resolve(path).takeIf(Path::exists)?.inputStream()
    
}