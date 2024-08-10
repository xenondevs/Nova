package xyz.xenondevs.nova.world.player.ability

import net.minecraft.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.commons.collections.removeIf
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.serialization.persistentdata.get
import xyz.xenondevs.nova.serialization.persistentdata.set
import xyz.xenondevs.nova.initialize.DisableFun
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries.ABILITY_TYPE
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTaskTimer
import kotlin.collections.set

private val ABILITIES_KEY = NamespacedKey(NOVA, "abilities1")

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [AddonsInitializer::class]
)
object AbilityManager : Listener {
    
    internal val activeAbilities = HashMap<Player, HashMap<AbilityType<*>, Ability>>()
    
    @InitFun
    private fun init() {
        registerEvents()
        Bukkit.getOnlinePlayers().forEach(AbilityManager::handlePlayerJoin)
        runTaskTimer(0, 1) { activeAbilities.values.flatMap(Map<*, Ability>::values).forEach(Ability::handleTick) }
    }
    
    @DisableFun
    private fun disable() {
        Bukkit.getOnlinePlayers().forEach(AbilityManager::handlePlayerQuit)
    }
    
    fun giveAbility(player: Player, type: AbilityType<*>) {
        val abilityMap = activeAbilities.getOrPut(player) { HashMap() }
        if (type in abilityMap)
            return
        
        val ability = type.createAbility(player)
        abilityMap[type] = ability
        
        saveActiveAbilities(player)
    }
    
    fun hasAbility(player: Player, type: AbilityType<*>): Boolean =
        activeAbilities[player]?.contains(type) ?: false
    
    fun takeAbility(player: Player, type: AbilityType<*>) {
        val abilityMap = activeAbilities[player]
        if (abilityMap == null || type !in abilityMap)
            return
        
        val ability = abilityMap[type]
        ability?.handleRemove()
        
        if (abilityMap.size == 1)
            activeAbilities -= player
        else abilityMap -= type
        
        saveActiveAbilities(player)
    }
    
    fun takeAbility(player: Player, ability: Ability) {
        val abilityMap = activeAbilities[player] ?: return
        
        abilityMap.removeIf { it.value == ability }
        ability.handleRemove()
        
        if (abilityMap.isEmpty())
            activeAbilities -= player
        
        saveActiveAbilities(player)
    }
    
    private fun saveActiveAbilities(player: Player) {
        val abilities = activeAbilities[player]?.map { it.key.id }
        val dataContainer = player.persistentDataContainer
        
        if (abilities != null)
            dataContainer.set(ABILITIES_KEY, abilities)
        else dataContainer.remove(ABILITIES_KEY)
    }
    
    private fun handlePlayerJoin(player: Player) {
        val dataContainer = player.persistentDataContainer
        val ids = dataContainer.get<List<ResourceLocation>>(ABILITIES_KEY)
        
        ids?.forEach {
            val abilityType = ABILITY_TYPE[it]
            if (abilityType != null)
                giveAbility(player, abilityType)
        }
    }
    
    private fun handlePlayerQuit(player: Player) {
        activeAbilities.remove(player)?.values?.forEach(Ability::handleRemove)
    }
    
    @EventHandler
    private fun handlePlayerQuit(event: PlayerQuitEvent) =
        handlePlayerQuit(event.player)
    
    @EventHandler
    private fun handlePlayerJoin(event: PlayerJoinEvent) =
        handlePlayerJoin(event.player)
    
}