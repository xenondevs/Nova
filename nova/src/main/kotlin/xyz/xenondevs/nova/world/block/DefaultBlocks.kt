package xyz.xenondevs.nova.world.block

import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeavesBlock
import net.minecraft.world.level.block.TripWireBlock
import org.bukkit.inventory.ItemType
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistrar.block
import xyz.xenondevs.nova.registry.NovaRegistrar.blockTag
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.entries.ItemTypeEntries
import xyz.xenondevs.nova.registry.registryEntrySetOf
import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.util.bukkitBlockData
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
import xyz.xenondevs.nova.world.block.state.model.PaleOakLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.SpruceLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties
import xyz.xenondevs.nova.world.block.state.property.DefaultScopedBlockStateProperties
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    runBefore = [RegistryLoader::class]
)
internal object DefaultBlocks {
    
    val DELEGATES: Set<NovaBlock> by blockTag("delegates") {
        add(
            NOTE_BLOCK, TRIPWIRE,
            OAK_LEAVES, SPRUCE_LEAVES, BIRCH_LEAVES, JUNGLE_LEAVES, ACACIA_LEAVES,
            DARK_OAK_LEAVES, MANGROVE_LEAVES, CHERRY_LEAVES, AZALEA_LEAVES, FLOWERING_AZALEA_LEAVES,
            PALE_OAK_LEAVES
        )
    }
    
    val UNKNOWN = block("unknown") {
        behaviors(UnknownBlockBehavior)
        stateBacked(Int.MAX_VALUE, BackingStateCategory.NOTE_BLOCK, BackingStateCategory.MUSHROOM_BLOCK)
    }
    
    val NOTE_BLOCK = block("note_block") {
        behaviors(
            NoteBlockBehavior,
            BlockSounds(SoundGroup.WOOD),
            Breakable(
                hardness = 0.8,
                toolCategories = registryEntrySetOf(VanillaToolCategories.AXE),
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
        modelLess { NoteBackingStateConfig.defaultStateConfig.vanillaBlockState.bukkitBlockData }
    }
    
    val TRIPWIRE = block("tripwire") {
        behaviors(
            TripwireBehavior,
            BlockSounds(SoundGroup.STONE),
            Breakable(
                hardness = 0.0,
                requiresToolForDrops = false
            )
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
        modelLess {
            Blocks.TRIPWIRE.defaultBlockState()
                .setValue(TripWireBlock.NORTH, getPropertyValueOrThrow(DefaultBlockStateProperties.TRIPWIRE_NORTH))
                .setValue(TripWireBlock.EAST, getPropertyValueOrThrow(DefaultBlockStateProperties.TRIPWIRE_EAST))
                .setValue(TripWireBlock.SOUTH, getPropertyValueOrThrow(DefaultBlockStateProperties.TRIPWIRE_SOUTH))
                .setValue(TripWireBlock.WEST, getPropertyValueOrThrow(DefaultBlockStateProperties.TRIPWIRE_WEST))
                .setValue(TripWireBlock.ATTACHED, getPropertyValueOrThrow(DefaultBlockStateProperties.TRIPWIRE_ATTACHED))
                .bukkitBlockData
        }
    }
    
    val OAK_LEAVES = leaves(OakLeavesBackingStateConfig, ItemTypeEntries.OAK_LEAVES)
    val SPRUCE_LEAVES = leaves(SpruceLeavesBackingStateConfig, ItemTypeEntries.SPRUCE_LEAVES)
    val BIRCH_LEAVES = leaves(BirchLeavesBackingStateConfig, ItemTypeEntries.BIRCH_LEAVES)
    val JUNGLE_LEAVES = leaves(JungleLeavesBackingStateConfig, ItemTypeEntries.JUNGLE_LEAVES)
    val ACACIA_LEAVES = leaves(AcaciaLeavesBackingStateConfig, ItemTypeEntries.ACACIA_LEAVES)
    val DARK_OAK_LEAVES = leaves(DarkOakLeavesBackingStateConfig, ItemTypeEntries.DARK_OAK_LEAVES)
    val MANGROVE_LEAVES = leaves(MangroveLeavesBackingStateConfig, ItemTypeEntries.MANGROVE_LEAVES)
    val CHERRY_LEAVES = leaves(CherryLeavesBackingStateConfig, ItemTypeEntries.CHERRY_LEAVES)
    val AZALEA_LEAVES = leaves(AzaleaLeavesBackingStateConfig, ItemTypeEntries.AZALEA_LEAVES)
    val FLOWERING_AZALEA_LEAVES = leaves(FloweringAzaleaLeavesBackingStateConfig, ItemTypeEntries.FLOWERING_AZALEA_LEAVES)
    val PALE_OAK_LEAVES = leaves(PaleOakLeavesBackingStateConfig, ItemTypeEntries.PALE_OAK_LEAVES)
    
    private fun leaves(
        cfg: LeavesBackingStateConfigType<*>,
        itemType: RegistryEntry.Paper<ItemType>
    ): RegistryEntry.Nova<NovaBlock> = block(cfg.fileName) {
        behaviors(
            LeavesBehavior(itemType),
            BlockSounds(cfg.defaultStateConfig.blockType.map { SoundGroup.from(it.createBlockData().soundGroup) }),
            Breakable(
                hardness = 0.2,
                toolCategories = registryEntrySetOf(VanillaToolCategories.HOE, VanillaToolCategories.SHEARS),
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
        modelLess {
            cfg.defaultStateConfig.vanillaBlockState
                .setValue(LeavesBlock.WATERLOGGED, getPropertyValueOrThrow(DefaultBlockStateProperties.WATERLOGGED))
                .bukkitBlockData
        }
    }
    
}