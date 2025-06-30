package xyz.xenondevs.nova.resources

import net.kyori.adventure.key.Key
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.ResourcePackManager.enablePack
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.serialization.persistentdata.get
import xyz.xenondevs.nova.serialization.persistentdata.set
import xyz.xenondevs.nova.util.registerEvents


private val PROMPT_MESSAGE by MAIN_CONFIG.entry<Component>("resource_pack", "prompt", "message")
private val PROMPT_FORCE by MAIN_CONFIG.entry<Boolean>("resource_pack", "prompt", "force")
private val ENABLE_PROMPT_FORCE_BYPASS_PERMISSION by MAIN_CONFIG.entry<Boolean>("resource_pack", "prompt", "enableForceBypassPermission")
private val ENABLE_PROMPT_BYPASS_PERMISSION by MAIN_CONFIG.entry<Boolean>("resource_pack", "prompt", "enablePromptBypassPermission")

private val PACK_APPLY_OVERRIDE_KEY = Key.key("nova", "resource_packs_override")
private const val PROMPT_BYPASS_PERMISSION = "nova.misc.resourcePack.bypass.prompt"
private const val FORCE_BYPASS_PERMISSION = "nova.misc.resourcePack.bypass.force"

/**
 * Controls which players receive which resource packs.
 */
@InternalInit(stage = InternalInitStage.POST_WORLD)
object ResourcePackManager : Listener {
    
    @InitFun
    private fun init() {
        registerEvents()
    }
    
    @EventHandler
    private fun handleJoin(event: PlayerJoinEvent) {
        val player = event.player
        val request = createPackRequest(player, getEnabledPacks(player))
            ?: return
        player.sendResourcePacks(request)
    }
    
    /**
     * Gets the ids of enabled resource packs for [player].
     * Includes the ids of all packs that are enabled by default or explicitly enabled via [enablePack].
     */
    fun getEnabledPacks(player: Player): Set<Key> {
        val overrides: Map<Key, Boolean> = player.persistentDataContainer[PACK_APPLY_OVERRIDE_KEY] ?: emptyMap()
        val enabled = ResourcePackBuilder.configurations
            .filter { (id, factory) -> overrides[id] ?: factory.isEnabledByDefault }
            .keys
        return enabled
    }
    
    /**
     * Explicitly enables the resource pack with [id] for [player], overwriting the default behavior.
     */
    fun enablePack(player: Player, id: Key) {
        val overrides: MutableMap<Key, Boolean> = player.persistentDataContainer[PACK_APPLY_OVERRIDE_KEY] ?: mutableMapOf()
        overrides[id] = true
        player.persistentDataContainer[PACK_APPLY_OVERRIDE_KEY] = overrides
        
        val request = createPackRequest(player, setOf(id))
            ?: return
        player.sendResourcePacks(request)
    }
    
    /**
     * Explicitly disables the resource pack with [id] for [player], overwriting the default behavior.
     */
    fun disablePack(player: Player, id: Key) {
        val overrides: MutableMap<Key, Boolean> = player.persistentDataContainer[PACK_APPLY_OVERRIDE_KEY] ?: mutableMapOf()
        overrides[id] = false
        player.persistentDataContainer[PACK_APPLY_OVERRIDE_KEY] = overrides
        
        player.removeResourcePack(AutoUploadManager.getPackUuid(id))
    }
    
    /**
     * Resets the resource pack with [id] for [player], removing it from the explicitly enabled and disabled packs.
     */
    fun resetPack(player: Player, id: Key) {
        val overrides = player.persistentDataContainer.get<MutableMap<Key, Boolean>>(PACK_APPLY_OVERRIDE_KEY)
        val override = overrides?.remove(id)
        player.persistentDataContainer[PACK_APPLY_OVERRIDE_KEY] = overrides
        
        val isEnabledByDefault = ResourcePackBuilder.configurations[id]?.isEnabledByDefault
        if (override != null && isEnabledByDefault != null && override != isEnabledByDefault) {
            if (isEnabledByDefault) {
                val request = createPackRequest(player, setOf(id))
                    ?: return
                player.sendResourcePacks(request)
            } else {
                player.removeResourcePack(AutoUploadManager.getPackUuid(id))
            }
        }
    }
    
    internal fun handlePackUpdated(id: Key) {
        for (player in Bukkit.getOnlinePlayers()) {
            if (id in getEnabledPacks(player)) {
                val request = createPackRequest(player, setOf(id))
                    ?: continue
                player.sendResourcePacks(request)
            }
        }
    }
    
    private fun createPackRequest(player: Player, packs: Set<Key>): ResourcePackRequest? {
        if (ENABLE_PROMPT_BYPASS_PERMISSION && player.hasPermission(PROMPT_BYPASS_PERMISSION))
            return null
        
        val force = PROMPT_FORCE && (!ENABLE_PROMPT_FORCE_BYPASS_PERMISSION || !player.hasPermission(FORCE_BYPASS_PERMISSION))
        
        val packInfos = packs.mapNotNull(AutoUploadManager::getPackInfo)
        if (packInfos.isEmpty())
            return null
        
        return ResourcePackRequest.resourcePackRequest()
            .replace(false)
            .required(force)
            .prompt(PROMPT_MESSAGE)
            .packs(packInfos)
            .build()
    }
    
}