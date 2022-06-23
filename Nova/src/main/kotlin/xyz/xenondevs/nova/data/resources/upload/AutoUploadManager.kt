package xyz.xenondevs.nova.data.resources.upload

import de.studiocode.invui.resourcepack.ForceResourcePack
import net.md_5.bungee.api.chat.ComponentBuilder
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.upload.service.CustomMultiPart
import xyz.xenondevs.nova.data.resources.upload.service.SelfHost
import xyz.xenondevs.nova.data.resources.upload.service.Xenondevs
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.data.http.ConnectionUtils
import java.io.File
import java.util.logging.Level

internal object AutoUploadManager : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = setOf(NovaConfig)
    
    private val SERVICES: List<UploadService> = listOf(Xenondevs, SelfHost, CustomMultiPart)
    
    var enabled = false
        private set
    
    private var selectedService: UploadService? = null
    private var url: String? = PermanentStorage.retrieveOrNull("resourcePackURL")
        set(value) {
            field = value
            PermanentStorage.store("resourcePackURL", value)
        }
    
    override fun init() {
        enable()
        
        if (url != null) forceResourcePack()
    }
    
    private fun enable() {
        val packConfig = DEFAULT_CONFIG.getConfigurationSection("resource_pack")!!
        val config = packConfig.getConfigurationSection("auto_upload")!!
        enabled = config.getBoolean("enabled")
        
        if (packConfig.contains("url")) {
            val url = packConfig.getString("url")
            if (!url.isNullOrEmpty()) {
                if (enabled)
                    LOGGER.warning("The resource pack url is set in the config, but the auto upload is also enabled. Defaulting to the url in the config.")
                this.url = url
                forceResourcePack()
                return
            }
        }
        
        if (!enabled)
            return
        
        val serviceName = config.getString("service")
        if (serviceName != null) {
            val service = SERVICES.find { it.name.equals(serviceName, ignoreCase = true) }
            checkNotNull(service) { "Service $serviceName not found!" }
            service.loadConfig(config)
            
            selectedService = service
        } else {
            LOGGER.warning("No uploading service specified! Available: " + SERVICES.joinToString(transform = UploadService::name))
        }
    }
    
    override fun disable() {
        selectedService?.disable()
        selectedService = null
    }
    
    fun reload() {
        disable()
        enable()
    }
    
    suspend fun uploadPack(pack: File): String? {
        if (selectedService == SelfHost)
            SelfHost.startedLatch.await()
        
        require(pack.exists()) { pack.absolutePath + " not found!" }
        
        this.url = selectedService?.upload(pack)
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
                ComponentBuilder("Nova Resource Pack").create(),
                true
            )
        } catch (ex: Exception) {
            LOGGER.log(Level.SEVERE, "Failed to download resourcepack! Is the server down?", ex)
        }
    }
    
}