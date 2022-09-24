package xyz.xenondevs.nova.data.world.legacy

import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.WorldLoadEvent
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.world.legacy.impl.v0_10.PreVarIntConverter
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.util.data.VersionRange
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.removeIf
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.logging.Level

private val WORLD_VERSION_KEY = NamespacedKey(NOVA, "regionVersion")

internal object LegacyFileConverter : Initializable(), Listener {
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer, VanillaTileEntityManager)
    
    private val converters = TreeMap<VersionRange, VersionConverter>()
    
    private val futures = HashMap<World, CompletableFuture<Unit>>()
    
    override fun init() {
        registerEvents()
        registerConverters()
        LOGGER.info("Running legacy conversions...")
        runConversions(Bukkit.getWorlds())
        LOGGER.info("Legacy conversions finished.")
    }
    
    private fun registerConverters() {
        register(Version("0.9")..Version("0.9.11"), PreVarIntConverter)
    }
    
    private fun register(versionRange: VersionRange, converter: VersionConverter) {
        converters[versionRange] = converter
    }
    
    private fun runConversions(worlds: List<World>) {
        val worldVersions = worlds.groupBy {
            Version(it.persistentDataContainer.get(WORLD_VERSION_KEY, PersistentDataType.STRING) ?: "0.9")
        }.toMutableMap().apply { removeIf { it.key.compareTo(NOVA.version, 2) == 0 } }
        
        worldVersions.forEach { (version, worlds) ->
            var minReached = false
            val toRun = ArrayList<VersionConverter>()
            converters.forEach { (versionRange, converter) ->
                if (!minReached)
                    minReached = version in versionRange
                if (minReached)
                    toRun.add(converter)
            }
            val size = toRun.size
            if (size == 0)
                return@forEach
            val regionFiles = prepareRegionFiles(worlds)
            toRun.forEachIndexed { i, converter ->
                regionFiles.forEach { (world, old, new) ->
                    try {
                        converter.getRegionFileConverter(world, old, new).convert()
                        old.delete()
                        if (i != size - 1)
                            new.renameTo(old)
                    } catch (e: Exception) {
                        LOGGER.log(Level.SEVERE, "Failed to convert world ${world.name}", e)
                    }
                }
                converter.handleRegionFilesConverted()
            }
        }
        
        worlds.forEach {
            futures.getOrPut(it) { CompletableFuture.completedFuture(Unit) }.complete(Unit)
            it.persistentDataContainer.set(WORLD_VERSION_KEY, PersistentDataType.STRING, NOVA.version.toString())
        }
    }
    
    private fun prepareRegionFiles(worlds: List<World>): List<Triple<World, File, File>> { // (world, old, new)
        val files = ArrayList<Triple<World, File, File>>()
        worlds.forEach { world ->
            val dir = File(world.worldFolder, "nova_region")
            if (!dir.exists() || !dir.isDirectory)
                return@forEach
            dir.listFiles()!!.asSequence().filter { it.isFile && it.name.endsWith(".nvr") }.forEach { file ->
                val legacyFile = File(file.parent, file.name.replaceAfterLast('.', "nvr-legacy"))
                if (!file.renameTo(legacyFile))
                    Files.move(file.toPath(), legacyFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                files += Triple(world, legacyFile, file)
            }
        }
        return files
    }
    
    @EventHandler
    private fun handleWorldLoad(event: WorldLoadEvent) {
        runConversions(listOf(event.world))
    }
    
    fun addConversionListener(world: World, run: () -> Unit) {
        futures.getOrPut(world) { CompletableFuture() }.thenRun(run)
    }
    
}