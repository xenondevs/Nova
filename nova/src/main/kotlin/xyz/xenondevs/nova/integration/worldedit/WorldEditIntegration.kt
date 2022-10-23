package xyz.xenondevs.nova.integration.worldedit

import com.sk89q.worldedit.WorldEdit
import org.bukkit.Bukkit
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.integration.worldedit.WorldEditIntegration.WorldEditType.*
import xyz.xenondevs.nova.integration.worldedit.fawe.FAWEListener
import xyz.xenondevs.nova.integration.worldedit.normal.WEListener
import xyz.xenondevs.nova.world.block.BlockManager

internal object WorldEditIntegration : Initializable() {
    
    override val initializationStage = InitializationStage.POST_WORLD
    override val dependsOn = setOf(BlockManager)
    
    private val WORLD_EDIT_TYPE = WorldEditType.getInstalledType()
    
    override fun init() {
        if (WORLD_EDIT_TYPE != NONE) {
            val worldEdit = WorldEdit.getInstance()
            worldEdit.blockFactory.register(NovaBlockInputParser(worldEdit))
            
            when (WORLD_EDIT_TYPE) {
                WORLD_EDIT -> worldEdit.eventBus.register(WEListener())
                FAST_ASYNC_WORLD_EDIT -> worldEdit.eventBus.register(FAWEListener())
                else -> Unit
            }
        }
    }
    
    private enum class WorldEditType {
    
        NONE,
        WORLD_EDIT,
        FAST_ASYNC_WORLD_EDIT;
        
        companion object {
            
            fun getInstalledType(): WorldEditType {
                val pm = Bukkit.getPluginManager()
                return when {
                    pm.getPlugin("FastAsyncWorldEdit") != null -> FAST_ASYNC_WORLD_EDIT
                    pm.getPlugin("WorldEdit") != null -> WORLD_EDIT
                    else -> NONE
                }
            }
            
        }
        
    }
    
}