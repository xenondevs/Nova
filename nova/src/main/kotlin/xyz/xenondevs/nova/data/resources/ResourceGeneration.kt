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
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.util.data.getResourceAsStream
import xyz.xenondevs.nova.util.data.update
import java.security.MessageDigest

private const val RESOURCES_HASH = "resourcesHash"
private val ASSET_INDEX_FILES = listOf("assets/materials.json", "assets/guis.json", "assets/armor.json")

/**
 * Handles resource pack generation on startup.
 * Decides whether the resource pack needs to be regenerated and splits the generation into two parts: PreWorld and PostWorld (async)
 */
internal object ResourceGeneration {
    
    private lateinit var resourcesHash: String
    private var builder: ResourcePackBuilder? = null
    
    @InternalInit(
        stage = InternalInitStage.PRE_WORLD,
        dependsOn = [AddonsInitializer::class]
    )
    object PreWorld {
        
        @InitFun
        private fun init() {
            resourcesHash = calculateResourcesHash()
            if (PermanentStorage.retrieveOrNull<String>(RESOURCES_HASH) == resourcesHash && ResourceLookups.hasAllLookups()) {
                // Load from PermanentStorage
                ResourceLookups.loadAll()
            } else {
                // Build resource pack
                LOGGER.info("Building resource pack")
                builder = ResourcePackBuilder().also(ResourcePackBuilder::buildPackPreWorld)
                LOGGER.info("Pre-world resource pack building done")
            }
        }
        
    }
    
    @InternalInit(
        stage = InternalInitStage.POST_WORLD_ASYNC,
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
                PermanentStorage.store(RESOURCES_HASH, resourcesHash)
            }
        }
        
    }
    
    private fun calculateResourcesHash(): String {
        val digest = MessageDigest.getInstance("MD5")
        
        // Nova version
        digest.update(NOVA.version.toString().toByteArray())
        // nova asset indices
        ASSET_INDEX_FILES.forEach { getResourceAsStream(it.replace("assets/", "assets/nova/"))?.let(digest::update) }
        
        // Addon id, version and asset indices
        AddonManager.loaders.forEach { (id, loader) ->
            // id and version
            digest.update(id.toByteArray())
            digest.update(loader.description.version.toByteArray())
            
            // asset indices
            ASSET_INDEX_FILES.forEach { getResourceAsStream(loader.file, it)?.let(digest::update) }
        }
        
        return DataUtils.toHexadecimalString(digest.digest())
    }
    
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