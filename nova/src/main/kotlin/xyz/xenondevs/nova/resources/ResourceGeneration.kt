package xyz.xenondevs.nova.resources

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA_VERSION
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.addon.version
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.ui.overlay.guitexture.DefaultGuiTextures
import xyz.xenondevs.nova.util.data.update
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.migrator.BlockMigrator
import xyz.xenondevs.nova.world.item.DefaultBlockOverlays
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.DefaultItems
import java.security.MessageDigest
import java.util.*

/**
 * Handles resource pack generation on startup.
 * Decides whether the resource pack needs to be regenerated and splits the generation into two parts: PreWorld and PostWorld (async)
 */
internal object ResourceGeneration {
    
    private const val FORCE_REBUILD_FLAG = "NovaForceRegenerateResourcePack"
    const val RESOURCES_HASH = "resources_hash"
    
    private lateinit var resourcesHash: String
    private val activeBuilders = ArrayList<ResourcePackBuilder>()
    
    @InternalInit(
        stage = InternalInitStage.PRE_WORLD,
        dispatcher = Dispatcher.ASYNC,
        runAfter = [
            DefaultItems::class,
            DefaultGuiItems::class,
            DefaultBlocks::class,
            DefaultBlockOverlays::class,
            DefaultGuiTextures::class,
        ]
    )
    object PreWorld {
        
        @InitFun
        private suspend fun preWorld() {
            resourcesHash = calculateResourcesHash()
            if (System.getProperty(FORCE_REBUILD_FLAG) != null
                || PermanentStorage.retrieve<String>(RESOURCES_HASH) != resourcesHash
            ) {
                // Build resource pack
                LOGGER.info("Building resource pack(s)")
                coroutineScope {
                    for ((_, config) in ResourcePackBuilder.configurations) {
                        val builder = config.create()
                        activeBuilders += builder
                        launch { builder.buildPackPreWorld() }
                    }
                }
                LOGGER.info("Pre-world resource pack building done")
            }
        }
        
    }
    
    @InternalInit(
        stage = InternalInitStage.POST_WORLD,
        dispatcher = Dispatcher.ASYNC,
        runAfter = [HooksLoader::class]
    )
    object PostWorld {
        
        @InitFun
        private suspend fun postWorld() {
            if (activeBuilders.isNotEmpty()) {
                LOGGER.info("Continuing to build resource pack(s)")
                coroutineScope {
                    for (builder in activeBuilders) {
                        launch {
                            val bin = builder.buildPackPostWorld()
                            AutoUploadManager.uploadPack(builder.id, bin)
                            AutoCopier.copyToDestinations(builder.id, bin)
                            builder.postBuildHooks.forEach { it() }
                        }
                    }
                }
                
                activeBuilders.clear()
                PermanentStorage.store(RESOURCES_HASH, resourcesHash)
                BlockMigrator.updateMigrationId()
            } else {
                // load here at the latest to ensure initialization failure on broken lookups
                ResourceLookups.loadAll()
            }
        }
        
    }
    
    /**
     * Calculates a hash based on the version and name of Nova and all addons
     * and the resource_pack config section.
     */
    private fun calculateResourcesHash(): String {
        val digest = MessageDigest.getInstance("MD5")
        
        // Nova version
        digest.update(NOVA_VERSION.toString().toByteArray())
        
        // Addon versions
        for (addon in AddonBootstrapper.addons) {
            digest.update(addon.namespace().toByteArray())
            digest.update(addon.version.toByteArray())
        }
        
        // resource_pack config section
        digest.update(MAIN_CONFIG.get().jsonObject["resource_pack"].hashCode())
        
        return HexFormat.of().formatHex(digest.digest())
    }
    
}