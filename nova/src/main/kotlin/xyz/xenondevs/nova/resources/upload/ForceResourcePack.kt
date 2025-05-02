package xyz.xenondevs.nova.resources.upload

import net.kyori.adventure.text.Component
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.data.HashUtils
import xyz.xenondevs.nova.util.registerEvents
import java.util.*

private val PROMPT_MESSAGE by MAIN_CONFIG.entry<Component>("resource_pack", "prompt", "message")
private val PROMPT_FORCE by MAIN_CONFIG.entry<Boolean>("resource_pack", "prompt", "force")
private val ENABLE_PROMPT_FORCE_BYPASS_PERMISSION by MAIN_CONFIG.entry<Boolean>("resource_pack", "prompt", "enableForceBypassPermission")
private val ENABLE_PROMPT_BYPASS_PERMISSION by MAIN_CONFIG.entry<Boolean>("resource_pack", "prompt", "enablePromptBypassPermission")

private const val PROMPT_BYPASS_PERMISSION = "nova.misc.resourcePack.bypass.prompt"
private const val FORCE_BYPASS_PERMISSION = "nova.misc.resourcePack.bypass.force"

@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object ForceResourcePack : Listener {
    
    private val packId: UUID by PermanentStorage.storedValue("force_resource_pack_uuid") { UUID.randomUUID() }
    
    private var hash: ByteArray? = null
    private var url: String? = null
    
    @InitFun
    private fun init() {
        registerEvents()
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        // TODO: replace with net.kyori.adventure.resource api once available
        
        val url = url
        val hash = hash
        if (url == null || hash == null)
            return
        
        val player = event.player
        if (ENABLE_PROMPT_BYPASS_PERMISSION && player.hasPermission(PROMPT_BYPASS_PERMISSION))
            return
        
        val force = PROMPT_FORCE && (!ENABLE_PROMPT_FORCE_BYPASS_PERMISSION || !player.hasPermission(FORCE_BYPASS_PERMISSION))
        event.player.setResourcePack(packId, url, hash, PROMPT_MESSAGE, force)
    }
    
    fun setResourcePack(url: String?) {
        if (url != null) {
            this.url = url
            this.hash = HashUtils.getFileHash(ResourcePackBuilder.RESOURCE_PACK_FILE, "SHA1")
        } else {
            this.url = null
            this.hash = null
        }
    }
    
}