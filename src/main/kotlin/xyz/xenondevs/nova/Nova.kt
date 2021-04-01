package xyz.xenondevs.nova

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.ui.TestUI
import xyz.xenondevs.nova.ui.setGlobalIngredients

lateinit var NOVA: Nova

class Nova : JavaPlugin() {
    
    val disableHandlers = ArrayList<() -> Unit>()
    
    override fun onEnable() {
        NOVA = this
        
        TileEntityManager // init TileEntityManager
        setGlobalIngredients()
        
        getCommand("test")!!.setExecutor(this)
        getCommand("getNovaMaterial")!!.setExecutor(this)
    }
    
    override fun onDisable() {
        disableHandlers.forEach { it() }
    }
    
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        sender as Player
        if (label == "test") {
            TestUI(sender, sender.uniqueId)
        } else if (label == "getnovamaterial") {
            val materialName = args[0]
            val material = NovaMaterial.valueOf(materialName)
            sender.inventory.addItem(material.createItemStack())
        }
        
        return false
    }
    
}