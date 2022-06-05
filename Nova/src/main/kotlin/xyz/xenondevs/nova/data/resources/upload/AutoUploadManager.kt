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
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.data.fromJson
import java.io.File
import java.net.URL

private val LIST_COMMON_URL = URL("https://api.xenondevs.xyz/nova/rp/common/list")
private const val COMMON_DOWNLOAD_URL = "https://api.xenondevs.xyz/nova/rp/common/%s/download"

internal object AutoUploadManager : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = setOf(NovaConfig)
    
    private val SERVICES: List<UploadService> = listOf(Xenondevs, SelfHost, CustomMultiPart)
    
    private val commonPacks: Set<String> by lazy { GSON.fromJson<HashSet<String>>(LIST_COMMON_URL.readText()) ?: emptySet() }
    
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
        val config = DEFAULT_CONFIG.getConfigurationSection("resource_pack.auto_upload")!!
        enabled = config.getBoolean("enabled")
        useCommonPacks = config.getBoolean("use_common_packs")
        
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
        ForceResourcePack.getInstance().setResourcePack(
            url,
            ComponentBuilder("Nova Resource Pack").create(),
            true
        )
    }
    
}