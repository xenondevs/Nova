package xyz.xenondevs.nova.data.resources.upload

import de.studiocode.invui.resourcepack.ForceResourcePack
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.chat.TextComponent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.config.configReloadable
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.upload.service.CustomMultiPart
import xyz.xenondevs.nova.data.resources.upload.service.S3
import xyz.xenondevs.nova.data.resources.upload.service.SelfHost
import xyz.xenondevs.nova.data.resources.upload.service.Xenondevs
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.util.data.hash
import xyz.xenondevs.nova.util.data.http.ConnectionUtils
import java.io.File
import java.util.logging.Level

internal object AutoUploadManager : Initializable() {
    
    override val initializationStage = InitializationStage.POST_WORLD_ASYNC
    override val dependsOn = setOf(NovaConfig, ResourceGeneration.PostWorld)
    
    private val SERVICES: List<UploadService> = listOf(Xenondevs, SelfHost, CustomMultiPart, S3)
    
    private val config by configReloadable { DEFAULT_CONFIG.getConfigurationSection("resource_pack.auto_upload")!! }
    
    var enabled = false
        private set
    var wasRegenerated = false
    private var explicitUrl = false
    private var selectedService: UploadService? = null
    
    private var url: String? = PermanentStorage.retrieveOrNull("resourcePackURL")
        set(value) {
            field = value
            PermanentStorage.store("resourcePackURL", value)
        }
    private var lastConfig: Int? = PermanentStorage.retrieveOrNull("lastUploadConfig")
        set(value) {
            field = value
            PermanentStorage.store("lastUploadConfig", value)
        }
    
    override fun init() {
        enable(fromReload = false)
        
        if (url != null)
            forceResourcePack()
        
        if (selectedService == SelfHost)
            SelfHost.startedLatch.await()
    }
    
    private fun enable(fromReload: Boolean) {
        val packConfig = DEFAULT_CONFIG.getConfigurationSection("resource_pack")!!
        enabled = config.getBoolean("enabled")
        
        if (packConfig.contains("url")) {
            val url = packConfig.getString("url")
            if (!url.isNullOrEmpty()) {
                if (enabled)
                    LOGGER.warning("The resource pack url is set in the config, but the auto upload is also enabled. Defaulting to the url in the config.")
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
        
        val serviceName = config.getString("service")
        if (serviceName != null) {
            val service = SERVICES.find { it.name.equals(serviceName, ignoreCase = true) }
            checkNotNull(service) { "Service $serviceName not found!" }
            service.loadConfig(config)
            
            selectedService = service
        } else {
            LOGGER.warning("No uploading service specified! Available: " + SERVICES.joinToString(transform = UploadService::name))
            return
        }
        
        val configHash = config.hash()
        if (wasRegenerated || lastConfig != configHash) {
            wasRegenerated = false
            runBlocking {
                val url = uploadPack(ResourcePackBuilder.RESOURCE_PACK_FILE)
                if (url == null)
                    LOGGER.warning("The resource pack was not uploaded. (Misconfigured auto uploader?)")
            }
            lastConfig = configHash
            
            if (fromReload)
                forceResourcePack()
        }
    }
    
    override fun disable() {
        selectedService?.disable()
        selectedService = null
    }
    
    fun reload() {
        disable()
        enable(fromReload = true)
    }
    
    suspend fun uploadPack(pack: File): String? {
        try {
            if (selectedService == SelfHost)
                SelfHost.startedLatch.await()
            
            require(pack.exists()) { pack.absolutePath + " not found!" }
            
            this.url = selectedService?.upload(pack)
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to upload the resource pack!", e)
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
                LOGGER.log(Level.SEVERE, "Invalid resource pack URL: $url. Please check your CustomMultiPart config!")
                if (CustomMultiPart.urlRegex != null)
                    LOGGER.log(Level.SEVERE, "Your urlRegex might be wrong: ${CustomMultiPart.urlRegex}")
            } else if (enabled) {
                LOGGER.log(Level.SEVERE, "Server responded with an invalid pack URL: $url")
            } else {
                LOGGER.log(Level.SEVERE, "Invalid resource pack URL: $url")
            }
            this.url = null
            return
        }
        try {
            ForceResourcePack.getInstance().setResourcePack(
                url,
                TextComponent.fromLegacyText(DEFAULT_CONFIG.getString("resource_pack.message")),
                true
            )
        } catch (e: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to download the resource pack! Is the server down?", e)
        }
    }
    
}