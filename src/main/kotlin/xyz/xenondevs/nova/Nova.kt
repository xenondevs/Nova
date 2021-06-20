package xyz.xenondevs.nova

import de.studiocode.invui.resourcepack.ForceResourcePack
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.ability.AbilityManager
import xyz.xenondevs.nova.advancement.AdvancementManager
import xyz.xenondevs.nova.attachment.AttachmentManager
import xyz.xenondevs.nova.command.CommandManager
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.equipment.ArmorEquipListener
import xyz.xenondevs.nova.item.ItemManager
import xyz.xenondevs.nova.network.NetworkManager
import xyz.xenondevs.nova.recipe.RecipeManager
import xyz.xenondevs.nova.region.VisualRegion
import xyz.xenondevs.nova.tileentity.ChunkLoadManager
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.ui.setGlobalIngredients

lateinit var NOVA: Nova

class Nova : JavaPlugin() {
    
    val disableHandlers = ArrayList<() -> Unit>()
    val pluginFile
        get() = file
    
    override fun onEnable() {
        NOVA = this
        setGlobalIngredients()
        
        NovaConfig.init()
        AdvancementManager.loadAdvancements()
        RecipeManager.registerRecipes()
        ChunkLoadManager.init()
        VanillaTileEntityManager.init()
        TileEntityManager.init()
        NetworkManager.init()
        ItemManager.init()
        AttachmentManager.init()
        CommandManager.init()
        ArmorEquipListener.init()
        AbilityManager.init()
        VisualRegion.init()
        
        forceResourcePack()
    }
    
    override fun onDisable() {
        disableHandlers.forEach { it() }
    }
    
    private fun forceResourcePack() {
        if (NovaConfig.getBoolean("resource_pack.enabled")) 
            ForceResourcePack.getInstance().resourcePackUrl = NovaConfig.getString("resource_pack.url")
    }
    
}