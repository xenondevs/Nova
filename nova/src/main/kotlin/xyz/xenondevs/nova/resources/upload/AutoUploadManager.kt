package xyz.xenondevs.nova.resources.upload

import kotlinx.coroutines.runBlocking
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
import xyz.xenondevs.nova.integration.HooksLoader
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.upload.service.CustomMultiPart
import xyz.xenondevs.nova.resources.upload.service.S3
import xyz.xenondevs.nova.resources.upload.service.SelfHost
import xyz.xenondevs.nova.util.data.http.ConnectionUtils
import java.nio.file.Path
import kotlin.io.path.exists

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC,
    dependsOn = [HooksLoader::class, ResourceGeneration.PostWorld::class]
)
internal object AutoUploadManager {
    
    internal val services = arrayListOf(SelfHost, CustomMultiPart, S3)
    
    var enabled = false
        private set
    var wasRegenerated = false
    private var explicitUrl = false
    private var selectedService: UploadService? = null
    
    private var url: String? = PermanentStorage.retrieve("resourcePackURL")
        set(value) {
            field = value
            PermanentStorage.store("resourcePackURL", value)
        }
    private var lastConfig: Int? = PermanentStorage.retrieve("lastUploadConfig")
        set(value) {
            field = value
            PermanentStorage.store("lastUploadConfig", value)
        }
    
    @InitFun
    private fun init() {
        val cfg = MAIN_CONFIG.strongNode("resource_pack")
        cfg.subscribe { disable(); enable(it, fromReload = true) }
        enable(cfg.get(), fromReload = false)
        
        if (url != null)
            forceResourcePack()
        
        if (selectedService == SelfHost)
            SelfHost.startedLatch.await()
    }
    
    private fun enable(cfg: ConfigurationNode, fromReload: Boolean) {
        val autoUploadCfg = cfg.node("auto_upload")
        enabled = autoUploadCfg.node("enabled").boolean
        
        if (cfg.hasChild("url")) {
            val url = cfg.node("url").string
            if (!url.isNullOrEmpty()) {
                if (enabled)
                    LOGGER.warn("The resource pack url is set in the config, but the auto upload is also enabled. Defaulting to the url in the config.")
                explicitUrl = true
                if (this.url != url) {
                    this.url = url
                    if (fromReload)
                        forceResourcePack()
                }
                return
            }
        }
        
        if (!enabled) {
            this.url = null
            return
        }
        
        val serviceName = autoUploadCfg.node("service").string?.lowercase()
        if (serviceName != null) {
            val service = services.find { serviceName in it.names }
            checkNotNull(service) { "Service $serviceName not found!" }
            service.loadConfig(autoUploadCfg)
            
            selectedService = service
        } else {
            LOGGER.warn("No uploading service specified! Available: " + services.joinToString { it.names[0] })
            return
        }
        
        val configHash = autoUploadCfg.hashCode()
        if (wasRegenerated || lastConfig != configHash) {
            wasRegenerated = false
            runBlocking {
                val url = uploadPack(ResourcePackBuilder.RESOURCE_PACK_FILE)
                if (url == null)
                    LOGGER.warn("The resource pack was not uploaded. (Misconfigured auto uploader?)")
            }
            lastConfig = configHash
            
            if (fromReload)
                forceResourcePack()
        }
    }
    
    @DisableFun(dispatcher = Dispatcher.ASYNC)
    private fun disable() {
        selectedService?.disable()
        selectedService = null
    }
    
    suspend fun uploadPack(pack: Path): String? {
        try {
            if (selectedService == SelfHost)
                SelfHost.startedLatch.await()
            
            require(pack.exists()) { pack + " not found!" }
            
            this.url = selectedService?.upload(pack)
        } catch (e: Exception) {
            LOGGER.error("Failed to upload the resource pack!", e)
        }
        
        forceResourcePack()
        return url
    }
    
    private fun forceResourcePack() {
        if (selectedService == SelfHost)
            SelfHost.startedLatch.await()
        
        val url = url
        if (url != null && !ConnectionUtils.isURL(url)) {
            if (selectedService == CustomMultiPart) {
                LOGGER.error("Invalid resource pack URL: $url. Please check your CustomMultiPart config!")
                if (CustomMultiPart.urlRegex != null)
                    LOGGER.error("Your urlRegex might be wrong: ${CustomMultiPart.urlRegex}")
            } else if (enabled) {
                LOGGER.error("Server responded with an invalid pack URL: $url")
            } else {
                LOGGER.error("Invalid resource pack URL: $url")
            }
            this.url = null
            return
        }
        
        ForceResourcePack.setResourcePack(url)
    }
    
}