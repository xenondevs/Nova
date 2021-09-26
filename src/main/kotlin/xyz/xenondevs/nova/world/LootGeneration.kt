package xyz.xenondevs.nova.world

import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.world.LootGenerateEvent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import kotlin.random.Random

object LootGeneration : Listener {
    
    private val lootFrequency = HashMap<String, Pair<Int, Int>>()
    private val possibleLoot = ArrayList<NovaMaterial>()
    
    fun init() {
        addPossibleLoot()
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    private fun addPossibleLoot() {
        // min and max loot amounts have to be added to the config
        // at loot_frequency.<material_name>.min and loot_frequency.<material_name>.max
        possibleLoot.add(NovaMaterialRegistry.STAR_SHARDS)
    }
    
    @EventHandler
    fun handleLootGenerationEvent(event: LootGenerateEvent) {
        if (event.lootTable.key.toString() == "minecraft:chests/jungle_temple_dispenser") return
        
        for (material in possibleLoot) {
            val name = material.typeName.lowercase()
            val (min, max) = lootFrequency.getOrPut(name) {
                val min = DEFAULT_CONFIG.getInt("loot_frequency.$name.min")!!
                val max = DEFAULT_CONFIG.getInt("loot_frequency.$name.max")!!
                min to max
            }
            val amount = Random.nextInt(min, max)
            event.loot.add(material.createItemStack(amount))
        }
    }
    
}