package xyz.xenondevs.nova.world.block

import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.resources.layout.block.BackingStateCategory
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers
import xyz.xenondevs.nova.patch.Patcher
import xyz.xenondevs.nova.util.bukkitBlockData
import xyz.xenondevs.nova.util.item.soundGroup
import xyz.xenondevs.nova.world.block.behavior.BlockSounds
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.block.behavior.LeavesBehavior
import xyz.xenondevs.nova.world.block.behavior.NoteBlockBehavior
import xyz.xenondevs.nova.world.block.behavior.UnknownBlockBehavior
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.block.state.model.AcaciaLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.AzaleaLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.BirchLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.CherryLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.DarkOakLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.FloweringAzaleaLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.JungleLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.LeavesBackingStateConfigType
import xyz.xenondevs.nova.world.block.state.model.MangroveLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.NoteBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.OakLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.SpruceLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.property.DefaultScopedBlockStateProperties

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    dependsOn = [Patcher::class]
)
internal object DefaultBlocks {
    
    val UNKNOWN = block("unknown") {
        behaviors(UnknownBlockBehavior)
        models {
            stateBacked(Int.MAX_VALUE, BackingStateCategory.NOTE_BLOCK, BackingStateCategory.MUSHROOM_BLOCK)
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
                breakParticles = null
            )
        )
        stateProperties(
            DefaultScopedBlockStateProperties.NOTE_BLOCK_INSTRUMENT,
            DefaultScopedBlockStateProperties.NOTE_BLOCK_NOTE,
            DefaultScopedBlockStateProperties.POWERED
        )
        models { modelLess { NoteBackingStateConfig.defaultStateConfig.vanillaBlockState.bukkitBlockData } }
    }
    
    val OAK_LEAVES = leaves(OakLeavesBackingStateConfig)
    val SPRUCE_LEAVES = leaves(SpruceLeavesBackingStateConfig)
    val BIRCH_LEAVES = leaves(BirchLeavesBackingStateConfig)
    val JUNGLE_LEAVES = leaves(JungleLeavesBackingStateConfig)
    val ACACIA_LEAVES = leaves(AcaciaLeavesBackingStateConfig)
    val DARK_OAK_LEAVES = leaves(DarkOakLeavesBackingStateConfig)
    val MANGROVE_LEAVES = leaves(MangroveLeavesBackingStateConfig)
    val CHERRY_LEAVES = leaves(CherryLeavesBackingStateConfig)
    val AZALEA_LEAVES = leaves(AzaleaLeavesBackingStateConfig)
    val FLOWERING_AZALEA_LEAVES = leaves(FloweringAzaleaLeavesBackingStateConfig)
    
    private fun leaves(cfg: LeavesBackingStateConfigType<*>): NovaBlock = block(cfg.fileName) {
        behaviors(
            LeavesBehavior,
            BlockSounds(SoundGroup.from(cfg.defaultStateConfig.vanillaBlockState.bukkitMaterial.soundGroup)),
            Breakable(
                hardness = 0.2,
                toolCategories = setOf(VanillaToolCategories.HOE, VanillaToolCategories.SHEARS),
                toolTier = VanillaToolTiers.WOOD,
                false,
                null
            )
        )
        stateProperties(
            DefaultScopedBlockStateProperties.LEAVES_DISTANCE,
            DefaultScopedBlockStateProperties.LEAVES_PERSISTENT,
            DefaultScopedBlockStateProperties.WATERLOGGED
        )
        models { modelLess { cfg.defaultStateConfig.vanillaBlockState.bukkitBlockData } }
    }
    
    private fun block(name: String, run: NovaBlockBuilder.() -> Unit): NovaBlock {
        val builder = NovaBlockBuilder(ResourceLocation.fromNamespaceAndPath("nova", name))
        builder.run()
        return builder.register()
    }
    
}