package xyz.xenondevs.nova.data.resources

import kotlinx.coroutines.runBlocking
import xyz.xenondevs.inventoryaccess.util.DataUtils
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.item.DefaultBlockOverlays
import xyz.xenondevs.nova.item.DefaultGuiItems
import xyz.xenondevs.nova.item.DefaultItems
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.migrator.BlockMigrator
import java.security.MessageDigest

private const val VERSION_HASH = "version_hash"

/**
 * Handles resource pack generation on startup.
 * Decides whether the resource pack needs to be regenerated and splits the generation into two parts: PreWorld and PostWorld (async)
 */
internal object ResourceGeneration {
    
    private lateinit var versionHash: String
    private var builder: ResourcePackBuilder? = null
    
    @InternalInit(
        stage = InternalInitStage.PRE_WORLD,
        dispatcher = Dispatcher.ASYNC,
        dependsOn = [
            AddonsInitializer::class,
            DefaultItems::class,
            DefaultGuiItems::class,
            DefaultBlocks::class,
            DefaultBlockOverlays::class,
        DefaultGuiTextures::class,
        ]
    )
    object PreWorld {
        
        @InitFun
        private fun init() {
            versionHash = calculateVersionHash()
            if (PermanentStorage.retrieveOrNull<String>(VERSION_HASH) != versionHash
                || !ResourceLookups.hasAllLookups()
                || !ResourceLookups.tryLoadAll()
                || !hasAllBlockModels()
            ) {
                // Build resource pack
                LOGGER.info("Building resource pack")
                builder = ResourcePackBuilder().also(ResourcePackBuilder::buildPackPreWorld)
                LOGGER.info("Pre-world resource pack building done")
            } else {
                ResourceLookups.loadAll()
            }
        }
        
    }
    
    @InternalInit(
        stage = InternalInitStage.POST_WORLD,
        dispatcher = Dispatcher.ASYNC,
        dependsOn = [HooksLoader::class]
    )
    object PostWorld {
        
        @InitFun
        private fun init() {
            val builder = builder
            if (builder != null) {
                LOGGER.info("Continuing to build resource pack")
                builder.buildPackPostWorld()
                AutoUploadManager.wasRegenerated = true
                PermanentStorage.store(VERSION_HASH, versionHash)
                BlockMigrator.updateMigrationId()
            }
        }
        
    }
    
    /**
     * Calculates a hash based on the version and name of Nova and all addons.
     */
    private fun calculateVersionHash(): String {
        val digest = MessageDigest.getInstance("MD5")
        
        // Nova version
        digest.update(NOVA.version.toString().toByteArray())
        
        // Addon versions
        AddonManager.loaders.forEach { (id, loader) ->
            // id and version
            digest.update(id.toByteArray())
            digest.update(loader.description.version.toByteArray())
        }
        
        return DataUtils.toHexadecimalString(digest.digest())
    }
    
    /**
     * Checks whether all block states have models.
     */
    private fun hasAllBlockModels(): Boolean =
        NovaRegistries.BLOCK.asSequence()
            .flatMap { it.blockStates }
            .all { it in ResourceLookups.BLOCK_MODEL }
    
    internal fun createResourcePack() {
        ResourcePackBuilder().buildPackCompletely()
        
        if (AutoUploadManager.enabled) {
            runBlocking {
                val url = AutoUploadManager.uploadPack(ResourcePackBuilder.RESOURCE_PACK_FILE)
                if (url == null)
                    LOGGER.warning("The resource pack was not uploaded. (Misconfigured auto uploader?)")
            }
        }
    }
    
}