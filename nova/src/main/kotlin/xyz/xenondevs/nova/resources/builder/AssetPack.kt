@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package xyz.xenondevs.nova.resources.builder

import java.nio.file.Path
import kotlin.io.path.exists

/**
 * Represents the `assets/` directory inside an addon of [namespace].
 * 
 * @param namespace The namespace of the addon.
 * @param assetsDir The `assets/` directory.
 */
class AssetPack(val namespace: String, val assetsDir: Path) {
    
    /**
     * The `models/` directory, or `null` if it doesn't exist.
     */
    val modelsDir: Path? = assetsDir.resolve("models/").takeIf(Path::exists)
    
    /**
     * The `textures/` directory, or `null` if it doesn't exist.
     */
    val texturesDir: Path? = assetsDir.resolve("textures/").takeIf(Path::exists)
    
    /**
     * The `fonts/` directory, or `null` if it doesn't exist.
     */
    val fontsDir: Path? = assetsDir.resolve("fonts/").takeIf(Path::exists)
    
    /**
     * The `lang/` directory, or `null` if it doesn't exist.
     */
    val langDir: Path? = assetsDir.resolve("lang/").takeIf(Path::exists)
    
    /**
     * The `sounds/` directory, or `null` if it doesn't exist.
     */
    val soundsDir: Path? = assetsDir.resolve("sounds/").takeIf(Path::exists)
    
    /**
     * The `sounds.json` file, or `null` if it doesn't exist.
     */
    val soundsFile: Path? = assetsDir.resolve("sounds.json").takeIf(Path::exists)
    
    /**
     * The `textures/waila/` textures directory, or `null` if it doesn't exist.
     */
    val wailaTexturesDir: Path? = assetsDir.resolve("textures/waila/").takeIf(Path::exists)
    
    /**
     * The `atlases/` directory, or `null` if it doesn't exist.
     */
    val atlasesDir: Path? = assetsDir.resolve("atlases/").takeIf(Path::exists)
    
}