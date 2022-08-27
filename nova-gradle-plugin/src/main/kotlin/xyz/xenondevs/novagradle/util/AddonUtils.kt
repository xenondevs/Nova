package xyz.xenondevs.novagradle.util

import org.gradle.api.Project
import java.io.File

private val ADDON_ID_REGEX = Regex("""id:\s*([a-z\d_]*)""")

internal object AddonUtils {
    
    fun getAddonId(project: Project, resourcesDir: String): String {
        val addonCfgFile = project.file("$resourcesDir/addon.yml").takeIf(File::exists)
            ?: throw IllegalStateException("addon.yml is not present")
        return ADDON_ID_REGEX.find(addonCfgFile.readText())?.groupValues?.get(1)
            ?: throw IllegalStateException("No id in addon.yml")
    }
    
}
