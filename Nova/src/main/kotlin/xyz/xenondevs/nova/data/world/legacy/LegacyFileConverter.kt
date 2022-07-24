package xyz.xenondevs.nova.data.world.legacy

import org.bukkit.Bukkit
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.data.VersionRange
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

internal object LegacyFileConverter : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = emptySet<Initializable>()
    
    private val lastVersion = Version(PermanentStorage.retrieve("last_version") { "0.1" })
    private val regionFileDirectories by lazy {
        Bukkit.getWorlds().mapNotNull { world ->
            File(world.worldFolder, "nova_region").takeIf { it.exists() && it.isDirectory }
        }
    }
    
    val converters = TreeMap<VersionRange, VersionConverter>()
    
    override fun init() {
        registerConverters()
//        runConversions()
    }
    
    private fun registerConverters() {
        //register(Version("0.9")..Version("0.9.9"), PreVarIntConverter)
    }
    
    private fun register(versionRange: VersionRange, converter: VersionConverter) {
        converters[versionRange] = converter
    }
    
    private fun runConversions() {
        var minReached = false
        val toRun = ArrayList<VersionConverter>()
        converters.forEach { (versionRange, converter) ->
            if (!minReached)
                minReached = lastVersion in versionRange
            if (minReached)
                toRun.add(converter)
        }
        val size = toRun.size
        if (size == 0)
            return
        LOGGER.info("Running legacy file conversions...")
        val regionFiles = prepareRegionFiles()
        toRun.forEachIndexed { i, converter ->
            regionFiles.forEach { (old, new) ->
                converter.getRegionFileConverter(old, new).convert()
                old.delete()
                if (i != size - 1)
                    new.renameTo(old)
            }
        }
        LOGGER.info("Converted $regionFiles region files.")
    }
    
    private fun prepareRegionFiles(): Map<File, File> { // old -> new
        val files = HashMap<File, File>()
        regionFileDirectories.forEach { dir ->
            dir.listFiles()!!.asSequence().filter { it.isFile && it.name.endsWith(".nvr") }.forEach { file ->
                val legacyFile = File(file.parent, file.name.replaceAfterLast('.', "nvr-legacy"))
                if (!file.renameTo(legacyFile))
                    Files.move(file.toPath(), legacyFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                files[legacyFile] = file
            }
        }
        return files
    }
    
}