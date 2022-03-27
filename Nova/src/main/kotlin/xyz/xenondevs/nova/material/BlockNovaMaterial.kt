package xyz.xenondevs.nova.material

import org.bukkit.Material
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.util.SoundEffect
import xyz.xenondevs.nova.util.item.ToolCategory
import xyz.xenondevs.nova.util.item.ToolLevel

open class BlockNovaMaterial internal constructor(
    id: String,
    localizedName: String,
    novaItem: NovaItem? = null,
    options: BlockOptions,
    val properties: List<BlockPropertyType<*>>
) : ItemNovaMaterial(id, localizedName, novaItem) {
    
    val block: ModelData by lazy { Resources.getModelData(id).second!! }
    
    val hardness = options.hardness
    val toolCategory = options.toolCategory
    val toolLevel = options.toolLevel
    val requiresToolForDrops = options.requiresToolForDrops
    val hitboxType = options.hitboxType
    val placeSound = options.placeSound
    val breakSound = options.breakSound
    val breakParticles = options.breakParticles
    val showBreakAnimation = options.showBreakAnimation
    
}

data class BlockOptions(
    val hardness: Double,
    val toolCategory: ToolCategory?,
    val toolLevel: ToolLevel?,
    val requiresToolForDrops: Boolean,
    val hitboxType: Material,
    val placeSound: SoundEffect? = null,
    val breakSound: SoundEffect? = null,
    val breakParticles: Material? = null,
    val showBreakAnimation: Boolean = true
)