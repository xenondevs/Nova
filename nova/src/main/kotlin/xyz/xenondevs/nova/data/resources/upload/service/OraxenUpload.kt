package xyz.xenondevs.nova.data.resources.upload.service

import io.th0rgal.oraxen.pack.upload.hosts.Polymath
import org.bukkit.configuration.ConfigurationSection
import xyz.xenondevs.nova.data.resources.upload.UploadService
import xyz.xenondevs.nova.integration.customitems.plugin.Oraxen
import java.io.File
import io.th0rgal.oraxen.config.Settings as OraxenSettings

internal object OraxenUpload : UploadService {
    
    override val name = "oraxen"
    
    override suspend fun upload(file: File): String {
        if (!Oraxen.isInstalled) throw IllegalStateException("Oraxen is not installed!")
        val polymath = Polymath(OraxenSettings.POLYMATH_SERVER.toString())
        if(!polymath.uploadPack(file)) throw IllegalStateException("Failed to upload pack to polymath!")
        return polymath.minecraftPackURL
    }
    
    override fun loadConfig(cfg: ConfigurationSection) = Unit
    
    override fun disable() = Unit
}