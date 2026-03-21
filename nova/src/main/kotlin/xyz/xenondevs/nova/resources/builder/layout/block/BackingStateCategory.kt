package xyz.xenondevs.nova.resources.builder.layout.block

import org.bukkit.block.BlockType
import org.bukkit.block.data.BlockData
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.entries.BlockTypeEntries
import xyz.xenondevs.nova.world.block.state.model.AcaciaLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.AzaleaLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType
import xyz.xenondevs.nova.world.block.state.model.BirchLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.BrownMushroomBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.CherryLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.DarkOakLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.FloweringAzaleaLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.JungleLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.MangroveLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.MushroomStemBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.NoteBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.OakLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.PaleOakLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.RedMushroomBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.SpruceLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.TripwireBackingStateConfigType

/**
 * Represent the different block types whose states can be used to display a custom block model.
 *
 * State-backed custom block models are generally more performant than entity-backed models, but have some limitations.
 * There is also only a certain amount of total block states available that can be used for custom block models.
 */
enum class BackingStateCategory(
    internal val fallbackCollider: Provider<BlockData>,
    internal vararg val backingStateConfigTypes: BackingStateConfigType<*>
) {
    
    /**
     * The block model uses note block states.
     *
     * - Limited to 2024 different models
     * - Models cannot be transparent
     * - Full block hitbox
     * - Full block collider
     * - Cannot be waterlogged
     * - Client-side arm swing animation on right-click
     */
    NOTE_BLOCK(
        fallbackCollider = BlockTypeEntries.BARRIER,
        NoteBackingStateConfig
    ),
    
    /**
     * The block model uses mushroom block states.
     *
     * - Limited to 189 different models
     * - Models cannot be transparent
     * - Full block hitbox
     * - Full block collider
     * - Cannot be waterlogged
     */
    MUSHROOM_BLOCK(
        fallbackCollider = BlockTypeEntries.BARRIER,
        RedMushroomBackingStateConfig, BrownMushroomBackingStateConfig, MushroomStemBackingStateConfig
    ),
    
    /**
     * The block model uses leave block states.
     *
     * - Limited to 153 different models
     * - Models can be transparent, translucent textures are disabled on "fancy" graphics setting
     * - Full block hitbox
     * - Full block collider
     * - Can be waterlogged
     * - Some shaders might animate blocks of this type to blow in the wind
     */
    LEAVES(
        fallbackCollider = BlockTypeEntries.BARRIER,
        OakLeavesBackingStateConfig, SpruceLeavesBackingStateConfig, BirchLeavesBackingStateConfig,
        JungleLeavesBackingStateConfig, AcaciaLeavesBackingStateConfig, DarkOakLeavesBackingStateConfig,
        MangroveLeavesBackingStateConfig, AzaleaLeavesBackingStateConfig, FloweringAzaleaLeavesBackingStateConfig,
        CherryLeavesBackingStateConfig, PaleOakLeavesBackingStateConfig
    ),
    
    /**
     * The block model uses tripwire block states.
     *
     * - Limited to 48 different models
     * - Models can be transparent
     * - Half-height block hitbox
     * - No collider
     * - Cannot be waterlogged
     */
    TRIPWIRE_UNATTACHED(
        fallbackCollider = BlockTypeEntries.STRUCTURE_VOID,
        TripwireBackingStateConfigType.Unattached
    ),
    
    /**
     * The block model uses tripwire block states.
     *
     * - Limited to 48 different models
     * - Models can be transparent
     * - Block hitbox: 1x0.09375x1, offset by +0.0625y
     * - No collider
     * - Cannot be waterlogged
     */
    TRIPWIRE_ATTACHED(
        fallbackCollider = BlockTypeEntries.STRUCTURE_VOID,
        TripwireBackingStateConfigType.Attached
    );
    
    constructor(fallbackCollider: RegistryEntry.Paper<BlockType>, vararg backingStateConfigTypes: BackingStateConfigType<*>) :
        this(fallbackCollider.map { it.createBlockData() }, *backingStateConfigTypes)
    
}