package xyz.xenondevs.nova

import com.google.gson.JsonObject
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.advancement.AdvancementManager
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.recipe.NovaRecipes
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.getTileEntityData
import xyz.xenondevs.nova.ui.setGlobalIngredients
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.blockLocation

lateinit var NOVA: Nova

class Nova : JavaPlugin() {
    
    val disableHandlers = ArrayList<() -> Unit>()
    
    override fun onEnable() {
        NOVA = this
        setGlobalIngredients()
        
        TileEntityManager.init()
        NetworkManager.init()
        NovaRecipes.loadRecipes()
        AdvancementManager.loadAdvancements()
        
        getCommand("test")!!.setExecutor(this)
        getCommand("getNovaMaterial")!!.setExecutor(this)
        getCommand("stressTest")!!.setExecutor(this)
    }
    
    override fun onDisable() {
        disableHandlers.forEach { it() }
    }
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        sender as Player
        when {
            label.equals("test", true) -> {
                val armorStands = sender
                    .location
                    .chunk
                    .entities
                    .filterIsInstance<ArmorStand>()
                
                sender.sendMessage("Amount of ArmorStands in Chunk: ${armorStands.count()}")
                
                val nearest = armorStands.minByOrNull { it.location.distance(sender.location) }!!
                
                println(GSON.toJson(nearest.getTileEntityData()))
            }
            
            label.equals("getnovamaterial", true) -> {
                val materialName = args[0]
                val material = NovaMaterial.valueOf(materialName)
                sender.inventory.addItem(material.createItemStack())
            }
            
            label.equals("stresstest", true) -> {
                for (x in 0..7) {
                    for (y in 0..7) {
                        for (z in 0..7) {
                            val location = sender.location.clone().add(x.toDouble(), y.toDouble(), z.toDouble()).blockLocation
                            TileEntityManager.placeTileEntity(location, 0f, NovaMaterial.CABLE, JsonObject())
                        }
                    }
                }
            }
        }
        
        return false
    }
    
}