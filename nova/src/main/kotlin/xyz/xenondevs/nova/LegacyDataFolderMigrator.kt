package xyz.xenondevs.nova

import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.id
import xyz.xenondevs.nova.config.PermanentStorage
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.mapKeys
import kotlin.io.path.CopyActionResult
import kotlin.io.path.absolutePathString
import kotlin.io.path.copyTo
import kotlin.io.path.copyToRecursively
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteExisting
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.fileVisitor
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.relativeTo
import kotlin.io.path.visitFileTree
import kotlin.io.path.walk

// TODO: Remove in 0.19
internal object LegacyDataFolderMigrator {
    
    private val PLUGINS_DIR = DATA_FOLDER.parent
    
    fun migrate() {
        // addons need to be moved manually because plugins are already loaded at this point
        val addonsDir = DATA_FOLDER.resolve("addons")
        if (addonsDir.exists() && addonsDir.listDirectoryEntries().isNotEmpty()) {
            throw Exception("plugins/Nova/addons exists, but addons are plugins now. Please move all addons to plugins/")
        }
        
        migrateConfigs()
        migratePrefixedDataFiles("recipes")
        migratePrefixedDataFiles("loot")
        migrateWorldgen()
        
        PermanentStorage.remove("storedConfigs")
        PermanentStorage.remove("updatableFileHashes")
    }
    
    private fun determineDataFolder(namespace: String): Path? {
        for (addon in AddonBootstrapper.addons) {
            if (addon.id == namespace)
                return addon.dataFolder
        }
        
        return null
    }
    
    private fun determineDataFolder(prefixedFile: Path): Path? {
        for (addon in AddonBootstrapper.addons) {
            if (prefixedFile.name.startsWith("${addon.id}_"))
                return addon.dataFolder
        }
        
        return null
    }
    
    private fun migrateConfigs() {
        val configs = DATA_FOLDER.resolve("configs")
        if (configs.exists()) {
            // move plugins/Nova/configs/nova/ contents to plugins/Nova/configs/
            val configsNova = configs.resolve("nova")
            configsNova.walk()
                .filter { it.isRegularFile() }
                .forEach {
                    it.copyTo(it.parent.parent.resolve(it.name), true)
                    it.deleteRecursively()
                }
            configsNova.deleteRecursively()
            
            // move plugins/Nova/configs/<addon_id>/ contents to plugins/<addon_name>/configs/
            configs.listDirectoryEntries()
                .filter { it.isDirectory() && it.name != "nova" }
                .forEach { legacyConfigDir ->
                    val dataFolder = determineDataFolder(legacyConfigDir.name)
                    if (dataFolder != null) {
                        val newConfigDir = dataFolder.resolve("configs/")
                        newConfigDir.createDirectories()
                        legacyConfigDir.copyToRecursively(newConfigDir, followLinks = false, overwrite = true)
                        legacyConfigDir.deleteRecursively()
                    } else {
                        LOGGER.warn("Could not resolve addon ${legacyConfigDir.name} for config relocation")
                    }
                }
        }
        
        val storedConfigs: Map<String, String>? = PermanentStorage.retrieveOrNull("storedConfigs")
        if (storedConfigs != null) {
            // configs/<addon_id>/<config_name>.yml -> <addon_id>:<config_name>
            val newStoredConfigs = storedConfigs.mapKeys { (key, _) ->
                // special case: main config is now under the nova namespace
                if (key == "configs/config.yml")
                    return@mapKeys "nova:config"
                
                key.substringAfter('/').replaceFirst('/', ':').removeSuffix(".yml")
            }
            PermanentStorage.store("stored_configs", newStoredConfigs)
            PermanentStorage.remove("storedConfigs")
        }
    }
    
    private fun migratePrefixedDataFiles(dir: String) {
        val legacyDir = DATA_FOLDER.resolve(dir)
        if (legacyDir.exists()) {
            val oldUpdatableFiles: Map<String, String> = PermanentStorage.retrieve("updatableFileHashes", ::HashMap)
            val newUpdatableFiles: MutableMap<String, String> = PermanentStorage.retrieve("updatable_file_hashes", ::HashMap)
            legacyDir.walk()
                .filter { it.isRegularFile() }
                // file was extracted from addon, which means it starts with <addon_id>_
                .filter { legacyFile -> legacyFile.absolutePathString() in oldUpdatableFiles }
                .forEach { legacyFile ->
                    val dataFolder = determineDataFolder(legacyFile)
                    if (dataFolder != null) {
                        val relDirPath = legacyFile.relativeTo(legacyDir).parent?.invariantSeparatorsPathString ?: ""
                        val newFile = dataFolder.resolve(dir).resolve(relDirPath).resolve(legacyFile.name.substringAfter('_'))
                        newFile.parent.createDirectories()
                        legacyFile.copyTo(newFile, true)
                        legacyFile.deleteExisting()
                        
                        newUpdatableFiles[newFile.relativeTo(PLUGINS_DIR).invariantSeparatorsPathString] = oldUpdatableFiles[legacyFile.absolutePathString()]!!
                    } else {
                        LOGGER.warn("Could not resolve addon for prefixed data file relocation of $legacyFile")
                    }
                }
            
            PermanentStorage.store("updatable_file_hashes", newUpdatableFiles)
            
            deleteEmptyDirs(legacyDir)
        }
    }
    
    private fun migrateWorldgen() {
        val data = DATA_FOLDER.resolve("data")
        if (data.exists()) {
            val oldUpdatableFiles: Map<String, String> = PermanentStorage.retrieve("updatableFileHashes", ::HashMap)
            val newUpdatableFiles: MutableMap<String, String> = PermanentStorage.retrieve("updatable_file_hashes", ::HashMap)
            
            data.listDirectoryEntries().forEach { namespaceDir ->
                val addonId = namespaceDir.name
                val dataFolder = determineDataFolder(addonId)
                if (dataFolder != null) {
                    val oldWorldgen = namespaceDir.resolve("worldgen")
                    if (oldWorldgen.exists()) {
                        val newWorldgen = dataFolder.resolve("worldgen")
                        newWorldgen.parent.createDirectories()
                        oldWorldgen.copyToRecursively(newWorldgen, followLinks = false) { source, target ->
                            if (source.isDirectory())
                                return@copyToRecursively CopyActionResult.CONTINUE
                            
                            target.parent.createDirectories()
                            source.copyTo(target, true)
                            
                            val hash = oldUpdatableFiles[source.absolutePathString()]
                            if (hash != null) {
                                newUpdatableFiles[target.relativeTo(PLUGINS_DIR).invariantSeparatorsPathString] = hash
                            }
                            
                            CopyActionResult.CONTINUE
                        }
                        oldWorldgen.deleteRecursively()
                    }
                } else {
                    LOGGER.warn("Could not resolve addon $addonId for worldgen relocation")
                }
            }
            
            PermanentStorage.store("updatable_file_hashes", newUpdatableFiles)
            
            deleteEmptyDirs(data)
        }
    }
    
    private fun deleteEmptyDirs(dir: Path) {
        dir.visitFileTree(fileVisitor {
            onPostVisitDirectory { dir, _ ->
                if (dir.listDirectoryEntries().isEmpty())
                    dir.deleteExisting()
                
                FileVisitResult.CONTINUE
            }
        })
    }
    
}