package xyz.xenondevs.nova.resources

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent
import net.kyori.adventure.key.Key
import net.kyori.adventure.resource.ResourcePackCallback
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.resource.ResourcePackStatus
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.permission.PermissionManager
import xyz.xenondevs.nova.resources.ResourcePackManager.enablePack
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.util.registerEvents
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap


private val PROMPT_MESSAGE by MAIN_CONFIG.entry<Component>("resource_pack", "prompt", "message")
private val PROMPT_FORCE by MAIN_CONFIG.entry<Boolean>("resource_pack", "prompt", "force")
private val ENABLE_PROMPT_FORCE_BYPASS_PERMISSION by MAIN_CONFIG.entry<Boolean>("resource_pack", "prompt", "enableForceBypassPermission")
private val ENABLE_PROMPT_BYPASS_PERMISSION by MAIN_CONFIG.entry<Boolean>("resource_pack", "prompt", "enablePromptBypassPermission")

private const val PACK_APPLY_OVERRIDES_KEY = "resource_packs_override"
private const val PROMPT_BYPASS_PERMISSION = "nova.misc.resourcePack.bypass.prompt"
private const val FORCE_BYPASS_PERMISSION = "nova.misc.resourcePack.bypass.force"

/**
 * Controls which players receive which resource packs.
 */
@InternalInit(stage = InternalInitStage.POST_WORLD)
object ResourcePackManager : Listener {
    
    private val packApplyOverrides: MutableMap<UUID, MutableMap<Key, Boolean>> =
        (PermanentStorage.retrieve<Map<UUID, Map<Key, Boolean>>>(PACK_APPLY_OVERRIDES_KEY) ?: emptyMap())
            .mapValuesTo(ConcurrentHashMap()) { (_, map) -> ConcurrentHashMap(map) }
    
    private val packStatusFutures = ConcurrentHashMap<UUID, CompletableFuture<Boolean>>()
    
    @InitFun
    private fun init() {
        registerEvents()
    }
    
    @DisableFun
    private fun saveOverrides() {
        PermanentStorage.store(PACK_APPLY_OVERRIDES_KEY, packApplyOverrides)
    }
    
    @EventHandler
    private fun handleConfigure(event: AsyncPlayerConnectionConfigureEvent) {
        val uuid = event.connection.profile.id
            ?: return
        
        val packStatusFuture = CompletableFuture<Boolean>()
        packStatusFutures[uuid] = packStatusFuture
        
        val player = Bukkit.getOfflinePlayer(uuid)
        val request = createPackRequest(player, getEnabledPacks(player)) { _, status, _ ->
            when (status) {
                ResourcePackStatus.SUCCESSFULLY_LOADED -> packStatusFuture.complete(true)
                
                ResourcePackStatus.DECLINED, 
                ResourcePackStatus.INVALID_URL,
                ResourcePackStatus.FAILED_DOWNLOAD,
                ResourcePackStatus.FAILED_RELOAD, 
                ResourcePackStatus.DISCARDED -> packStatusFuture.complete(false)
                
                ResourcePackStatus.ACCEPTED, ResourcePackStatus.DOWNLOADED -> Unit
            }
        } ?: return
        
        event.connection.audience.sendResourcePacks(request)
        
        packStatusFuture.join()
        packStatusFutures.remove(uuid, packStatusFuture)
    }
    
    @EventHandler
    private fun handleConnectionClose(event: PlayerConnectionCloseEvent) {
        packStatusFutures.remove(event.playerUniqueId)?.complete(false)
    }
    
    /**
     * Gets the ids of enabled resource packs for [player].
     * Includes the ids of all packs that are enabled by default or explicitly enabled via [enablePack].
     */
    fun getEnabledPacks(player: OfflinePlayer): Set<Key> {
        val overrides: Map<Key, Boolean> = packApplyOverrides[player.uniqueId] ?: emptyMap()
        val enabled = ResourcePackBuilder.configurations
            .filter { (id, factory) -> overrides[id] ?: factory.isEnabledByDefault }
            .keys
        return enabled
    }
    
    /**
     * Explicitly enables the resource pack with [id] for [player], overwriting the default behavior.
     */
    fun enablePack(player: OfflinePlayer, id: Key) {
        val previous = packApplyOverrides.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }.put(id, true)
        
        if (previous == true)
            return
        val onlinePlayer = player.player
            ?: return
        val request = createPackRequest(onlinePlayer, setOf(id))
            ?: return
        onlinePlayer.sendResourcePacks(request)
    }
    
    /**
     * Explicitly disables the resource pack with [id] for [player], overwriting the default behavior.
     */
    fun disablePack(player: OfflinePlayer, id: Key) {
        val previous = packApplyOverrides.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }.put(id, false)
        if (previous == false)
            return
        player.player?.removeResourcePack(AutoUploadManager.getPackUuid(id))
    }
    
    /**
     * Resets the resource pack with [id] for [player], removing it from the explicitly enabled and disabled packs.
     */
    fun resetPack(player: OfflinePlayer, id: Key) {
        val previous = packApplyOverrides.computeIfAbsent(player.uniqueId) { ConcurrentHashMap() }.remove(id)
        
        val onlinePlayer = player.player
            ?: return
        val isEnabledByDefault = ResourcePackBuilder.configurations[id]?.isEnabledByDefault
        if (previous != null && isEnabledByDefault != null && previous != isEnabledByDefault) {
            if (isEnabledByDefault) {
                val request = createPackRequest(onlinePlayer, setOf(id))
                    ?: return
                onlinePlayer.sendResourcePacks(request)
            } else {
                onlinePlayer.removeResourcePack(AutoUploadManager.getPackUuid(id))
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
    
    private fun createPackRequest(player: OfflinePlayer, packs: Set<Key>, callback: ResourcePackCallback? = null): ResourcePackRequest? {
        val w = Bukkit.getWorlds().first()
        if (ENABLE_PROMPT_BYPASS_PERMISSION && PermissionManager.hasPermission(w, player, PROMPT_BYPASS_PERMISSION).join())
            return null
        
        val force = PROMPT_FORCE && (!ENABLE_PROMPT_FORCE_BYPASS_PERMISSION || !PermissionManager.hasPermission(w, player, FORCE_BYPASS_PERMISSION).join())
        
        val packInfos = packs.mapNotNull(AutoUploadManager::getPackInfo)
        if (packInfos.isEmpty())
            return null
        
        val builder = ResourcePackRequest.resourcePackRequest()
            .replace(false)
            .required(force)
            .prompt(PROMPT_MESSAGE)
            .packs(packInfos)
        if (callback != null)
            builder.callback(callback)
        return builder.build()
    }
    
}