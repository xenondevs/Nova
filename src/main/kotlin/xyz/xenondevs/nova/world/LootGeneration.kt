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
    private val addedLoot = ArrayList<NovaMaterial>()
    
    fun init() {
        addLoot()
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    private fun addLoot() {
        // min and max loot ammounts have to be added to the config
        // at loot_frequency.<material_name>.min and loot_frequency.<material_name>.max
        addedLoot.add(NovaMaterialRegistry.STAR_SHARDS)
    }
    
    @EventHandler
    fun handleLootGenerationEvent(event: LootGenerateEvent) {
        for (material in addedLoot) {
            val name = material.typeName.lowercase()
            if (name !in lootFrequency) {
                val min = DEFAULT_CONFIG.getInt("loot_frequency.$name.min")!!
                val max = DEFAULT_CONFIG.getInt("loot_frequency.$name.max")!!
                lootFrequency[name] = min to max
            }
            val (min, max) = lootFrequency[name]!!
            val ammount = Random.nextInt(min, max)
            event.loot.add(material.createItemStack(ammount))
        }
    }
    
}