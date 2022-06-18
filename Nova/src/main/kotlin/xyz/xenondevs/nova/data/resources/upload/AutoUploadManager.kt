package xyz.xenondevs.nova.data.resources.upload

import de.studiocode.inventoryaccess.util.DataUtils
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
import xyz.xenondevs.nova.util.concurrent.wait
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.data.fromJson
import xyz.xenondevs.nova.util.data.http.ConnectionUtils
import java.io.File
import java.net.URL
import java.util.logging.Level

private val LIST_COMMON_URL = URL("https://api.xenondevs.xyz/nova/rp/common/list")
private const val COMMON_DOWNLOAD_URL = "https://api.xenondevs.xyz/nova/rp/common/%s/download"

internal object AutoUploadManager : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = setOf(NovaConfig)
    
    private val SERVICES: List<UploadService> = listOf(Xenondevs, SelfHost, CustomMultiPart)
    
    private val commonPacks: Set<String> by lazy {
        GSON.fromJson<HashSet<String>>(LIST_COMMON_URL.readText()) ?: emptySet()
    }
    
    var enabled = false
        private set
    private var useCommonPacks = false
    
    private var selectedService: UploadService? = null
    private var url: String? = PermanentStorage.retrieveOrNull("resourcePackURL")
        set(value) {
            field = value
            PermanentStorage.store("resourcePackURL", value)
        }
    
    override fun init() {
        val packConfig = DEFAULT_CONFIG.getConfigurationSection("resource_pack")!!
        val config = packConfig.getConfigurationSection("auto_upload")!!
        enabled = config.getBoolean("enabled")
        useCommonPacks = config.getBoolean("use_common_packs")
        
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
        
        if (url != null) forceResourcePack()
    }
    
    override fun disable() {
        selectedService?.disable()
        selectedService = null
    }
    
    fun reload() {
        disable()
        init()
    }
    
    suspend fun uploadPack(pack: File): String? {
        if (selectedService == SelfHost)
            SelfHost.startedSemaphore.wait()
        
        require(pack.exists()) { pack.absolutePath + " not found!" }
        
        var url: String? = null
        if (useCommonPacks) {
            val hash = DataUtils.toHexadecimalString(HashUtils.getFileHash(pack, "MD5"))
            if (hash in commonPacks)
                url = COMMON_DOWNLOAD_URL.format(hash)
        }
        
        if (url == null)
            url = selectedService?.upload(pack)
        
        this.url = url
        forceResourcePack()
        return url
    }
    
    private fun forceResourcePack() {
        if (selectedService == SelfHost)
            SelfHost.startedSemaphore.wait()
        
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
            LOGGER.severe("If this keeps happening delete the \"url\" field in plugins/Nova/storage.do-not-edit and reupload the pack.")
        }
    }
    
}