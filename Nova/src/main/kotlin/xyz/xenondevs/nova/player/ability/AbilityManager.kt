package xyz.xenondevs.nova.player.ability

import com.google.gson.JsonArray
import org.bukkit.Bukkit
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.data.serialization.persistentdata.JsonElementDataType
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.util.data.getAllStrings
import xyz.xenondevs.nova.util.runTaskTimer
import kotlin.collections.set

private val ABILITIES_KEY = NamespacedKey(NOVA, "abilities_0.9")

object AbilityManager : Initializable(), Listener {
    
    private val activeAbilities = HashMap<Player, HashMap<AbilityType<*>, Ability>>()
    
    override val inMainThread = true
    override val dependsOn = setOf(AddonsInitializer)
    
    override fun init() {
        LOGGER.info("Initializing AbilityManager")
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        Bukkit.getOnlinePlayers().forEach(AbilityManager::handlePlayerJoin)
        runTaskTimer(0, 1) { activeAbilities.values.flatMap(Map<*, Ability>::values).forEach(Ability::handleTick) }
        NOVA.disableHandlers.add { Bukkit.getOnlinePlayers().forEach(AbilityManager::handlePlayerQuit) }
    }
    
    fun giveAbility(player: Player, type: AbilityType<*>) {
        val abilityMap = activeAbilities.getOrPut(player) { HashMap() }
        check(type !in abilityMap) { "An ability of this type is already set" }
        
        val ability = type.createAbility(player)
        abilityMap[type] = ability
        
        saveActiveAbilities(player)
    }
    
    fun takeAbility(player: Player, type: AbilityType<*>) {
        val abilityMap = activeAbilities[player]
        check(abilityMap != null && type in abilityMap) { "No ability of this type is set" }
        
        val ability = abilityMap[type]
        ability?.handleRemove()
        
        if (abilityMap.size == 1)
            activeAbilities -= player
        else abilityMap -= type
        
        saveActiveAbilities(player)
    }
    
    private fun saveActiveAbilities(player: Player) {
        val abilityMap = activeAbilities[player]
        val dataContainer = player.persistentDataContainer
        
        if (abilityMap != null) {
            val abilitiesJson = JsonArray()
            abilityMap.keys.forEach { abilitiesJson.add(it.id) }
            dataContainer.set(ABILITIES_KEY, JsonElementDataType, abilitiesJson)
        } else {
            dataContainer.remove(ABILITIES_KEY)
        }
    }
    
    private fun handlePlayerJoin(player: Player) {
        val dataContainer = player.persistentDataContainer
        val array = dataContainer.get(ABILITIES_KEY, JsonElementDataType)?.asJsonArray
        
        array?.getAllStrings()
            ?.forEach {
                val abilityType = AbilityTypeRegistry.getAbilityType<AbilityType<*>>(it)
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