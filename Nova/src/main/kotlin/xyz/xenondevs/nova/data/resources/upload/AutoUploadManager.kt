package xyz.xenondevs.nova.data.resources.upload

import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.resources.upload.service.SelfHost
import xyz.xenondevs.nova.data.resources.upload.service.Xenondevs
import xyz.xenondevs.nova.initialize.Initializable
import java.io.File

internal object AutoUploadManager : Initializable() {
    
    private val SERVICES: List<UploadService> = listOf(Xenondevs, SelfHost)
    
    override val inMainThread = false
    override val dependsOn = setOf(NovaConfig)
    
    private var enabled = false
    private lateinit var selectedService: UploadService
    
    override fun init() {
        val config = DEFAULT_CONFIG.getConfigurationSection("resource_pack.auto_upload")!!
        this.enabled = config.getBoolean("enabled")
        if (!enabled) return
        
        val serviceName = config.getString("service")
        checkNotNull(serviceName) { "No service specified! Available: " + SERVICES.joinToString(transform = UploadService::name) }
        val service = SERVICES.find { it.name.equals(serviceName, ignoreCase = true) }
        checkNotNull(service) { "Service $serviceName not found!" }
        
        this.selectedService = service
        service.loadConfig(config)
    }
    
    override fun disable() {
        if (enabled) selectedService.disable()
    }
    
    fun reload() {
        disable()
        init()
    }
    
    suspend fun uploadPack(pack: File): String {
        require(pack.exists()) { pack.absolutePath + " not found!" }
        return selectedService.upload(pack)
    }
    
}