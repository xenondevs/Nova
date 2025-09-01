package xyz.xenondevs.nova.resources.upload

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.resource.ResourcePackInfo
import org.spongepowered.configurate.ConfigurationNode
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.config.strongNode
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.upload.service.S3
import xyz.xenondevs.nova.resources.upload.service.SelfHost
import xyz.xenondevs.nova.util.data.HashUtils
import java.net.URI
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private const val PACK_URLS_KEY = "resource_pack_urls"

@Serializable
private class UploadedPack(
    val hashHex: String,
    val url: String
) {
    
    constructor(bin: ByteArray, url: String) : this(
        HexFormat.of().formatHex(HashUtils.getHash(bin, "SHA1")),
        url
    )
    
}

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    dispatcher = Dispatcher.ASYNC,
)
internal object AutoUploadManager {
    
    private val SERVICES = listOf(SelfHost, S3)//, CustomMultiPart)
    
    private var enabled = false
    private var selectedService: UploadService? = null
    
    private var uploadedPacks: MutableMap<Key, UploadedPack> =
        ConcurrentHashMap(PermanentStorage.retrieve<Map<Key, UploadedPack>>(PACK_URLS_KEY) ?: emptyMap())
    
    @InitFun
    private suspend fun init() {
        val cfg = MAIN_CONFIG.strongNode("resource_pack", "auto_upload")
        cfg.subscribe { runBlocking { disable(); enable(it) } }
        enable(cfg.get())
    }
    
    private suspend fun enable(cfg: ConfigurationNode) {
        enabled = cfg.node("enabled").boolean
        if (!enabled)
            return
        
        val serviceName = cfg.node("service").string?.lowercase()
        if (serviceName == null) {
            LOGGER.warn("No uploading service specified. Available: " + SERVICES.joinToString { it.names.first() })
            return
        }
        
        val service = SERVICES.firstOrNull { serviceName in it.names }
        if (service == null) {
            LOGGER.warn("Upload service with name '$serviceName' does not exist. Available: " + SERVICES.joinToString { it.names.first() })
            return
        }
        
        try {
            service.enable(cfg)
            this.selectedService = service
        } catch(e: IllegalArgumentException) {
            LOGGER.error("Failed to enable upload service $serviceName: ${e.message}")
        }
    }
    
    @DisableFun(dispatcher = Dispatcher.ASYNC)
    private suspend fun disable() {
        PermanentStorage.store(PACK_URLS_KEY, uploadedPacks)
        selectedService?.disable()
        selectedService = null
    }
    
    suspend fun uploadPack(id: Key, bin: ByteArray): String? {
        val url = selectedService?.upload(getPackUuid(id), bin)
        if (url != null) {
            uploadedPacks[id] = UploadedPack(bin, url)
            LOGGER.info("Resource pack $id available at $url")
        }
        return url
    }
    
    fun getPackUuid(id: Key): UUID =
        UUID.nameUUIDFromBytes(id.toString().encodeToByteArray())
    
    fun getPackInfo(id: Key): ResourcePackInfo? {
        val pack = uploadedPacks[id] ?: return null
        return ResourcePackInfo.resourcePackInfo(getPackUuid(id), URI(pack.url), pack.hashHex)
    }
    
}