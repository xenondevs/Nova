package xyz.xenondevs.nova.hooks.impl.oraxen

import io.th0rgal.oraxen.config.Settings
import io.th0rgal.oraxen.pack.upload.hosts.Polymath
import org.bukkit.configuration.ConfigurationSection
import xyz.xenondevs.nova.data.resources.upload.UploadService
import xyz.xenondevs.nova.hooks.Hook
import java.io.File

@Hook(plugins = ["Oraxen"])
internal object OraxenUploadService : UploadService {
    
    override val name = "oraxen"
    
    override suspend fun upload(file: File): String {
        val polymath = Polymath(Settings.POLYMATH_SERVER.toString())
        if(!polymath.uploadPack(file)) throw IllegalStateException("Failed to upload pack to polymath!")
        return polymath.minecraftPackURL
    }
    
    override fun loadConfig(cfg: ConfigurationSection) = Unit
    
    override fun disable() = Unit
    
}