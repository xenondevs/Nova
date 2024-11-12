package xyz.xenondevs.nova.world.block

import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.TripWireBlock
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.layout.block.BackingStateCategory
import xyz.xenondevs.nova.world.block.behavior.BlockSounds
import xyz.xenondevs.nova.world.block.behavior.Breakable
import xyz.xenondevs.nova.world.block.behavior.LeavesBehavior
import xyz.xenondevs.nova.world.block.behavior.NoteBlockBehavior
import xyz.xenondevs.nova.world.block.behavior.TripwireBehavior
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
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import xyz.xenondevs.nova.world.block.state.property.DefaultScopedBlockStateProperties
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers

@InternalInit(stage = InternalInitStage.PRE_WORLD)
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
        models { modelLess { NoteBackingStateConfig.defaultStateConfig.vanillaBlockState } }
    }
    
    val TRIPWIRE = block("tripwire") {
        behaviors(
            TripwireBehavior,
            BlockSounds(SoundGroup.STONE),
            Breakable(hardness = 0.0)
        )
        stateProperties(
            DefaultScopedBlockStateProperties.TRIPWIRE_NORTH,
            DefaultScopedBlockStateProperties.TRIPWIRE_EAST,
            DefaultScopedBlockStateProperties.TRIPWIRE_SOUTH,
            DefaultScopedBlockStateProperties.TRIPWIRE_WEST,
            DefaultScopedBlockStateProperties.TRIPWIRE_ATTACHED,
            DefaultScopedBlockStateProperties.TRIPWIRE_DISARMED,
            DefaultScopedBlockStateProperties.POWERED
        )
        models {
            modelLess {
                Blocks.TRIPWIRE.defaultBlockState()
                    .setValue(TripWireBlock.NORTH, getPropertyValueOrThrow(DefaultBlockStateProperties.TRIPWIRE_NORTH))
                    .setValue(TripWireBlock.EAST, getPropertyValueOrThrow(DefaultBlockStateProperties.TRIPWIRE_EAST))
                    .setValue(TripWireBlock.SOUTH, getPropertyValueOrThrow(DefaultBlockStateProperties.TRIPWIRE_SOUTH))
                    .setValue(TripWireBlock.WEST, getPropertyValueOrThrow(DefaultBlockStateProperties.TRIPWIRE_WEST))
                    .setValue(TripWireBlock.ATTACHED, getPropertyValueOrThrow(DefaultBlockStateProperties.TRIPWIRE_ATTACHED))
            }
        }
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
            BlockSounds(SoundGroup.from(cfg.defaultStateConfig.vanillaBlockState.soundType)),
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
        models {
            modelLess {
                cfg.defaultStateConfig.vanillaBlockState
                    .setValue(LeavesBlock.WATERLOGGED, getPropertyValueOrThrow(DefaultBlockStateProperties.WATERLOGGED))
            }
        }
    }
    
    private fun block(name: String, run: NovaBlockBuilder.() -> Unit): NovaBlock {
        val builder = NovaBlockBuilder(ResourceLocation.fromNamespaceAndPath("nova", name))
        builder.run()
        return builder.register()
    }
    
}