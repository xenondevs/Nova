package xyz.xenondevs.nova.world.block

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import xyz.xenondevs.nova.data.resources.layout.block.BackingStateCategory
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.item.tool.VanillaToolTiers
import xyz.xenondevs.nova.world.block.behavior.BlockSounds
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.block.behavior.NoteBlockBehavior
import xyz.xenondevs.nova.world.block.behavior.UnknownBlockBehavior
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.block.state.property.DefaultScopedBlockStateProperties

@InternalInit(stage = InternalInitStage.PRE_WORLD)
internal object DefaultBlocks {
    
    val UNKNOWN = block("unknown") {
        behaviors(UnknownBlockBehavior)
        models {
            stateBacked(Int.MAX_VALUE, BackingStateCategory.NOTE_BLOCK, BackingStateCategory.MUSHROOM_BLOCK)
            selectModel {
                createCubeModel("unknown")
            }
        }
    }
    
    val NOTE_BLOCK = block("note_block") {
        behaviors(
            NoteBlockBehavior,
            BlockSounds(SoundGroup.WOOD),
            Breakable(
                hardness = 0.8,
                toolCategory = VanillaToolCategories.AXE,
                toolTier = VanillaToolTiers.WOOD,
                requiresToolForDrops = false,
                breakParticles = Material.NOTE_BLOCK
            )
        )
        stateProperties(
            DefaultScopedBlockStateProperties.INSTRUMENT,
            DefaultScopedBlockStateProperties.NOTE,
            DefaultScopedBlockStateProperties.POWERED
        )
        models { modelLess { Material.NOTE_BLOCK.createBlockData() } }
    }
    
    private fun block(name: String, run: NovaBlockBuilder.() -> Unit): NovaBlock {
        val builder = NovaBlockBuilder(ResourceLocation("nova", name))
        builder.run()
        return builder.register()
    }
    
}