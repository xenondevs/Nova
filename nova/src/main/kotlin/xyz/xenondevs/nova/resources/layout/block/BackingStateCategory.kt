package xyz.xenondevs.nova.resources.layout.block

import org.bukkit.Material
import org.bukkit.block.data.BlockData
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
import xyz.xenondevs.nova.world.block.state.model.RedMushroomBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.SpruceLeavesBackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.TripwireBackingStateConfigType

/**
 * Represent the different block types whose states can be used to display a custom block model.
 *
 * State-backed custom block models are generally more performant than entity-backed models, but have some limitations.
 * There is also only a certain amount of total block states available that can be used for custom block models.
 *
 * @param fallbackHitbox The hitbox block type if display entities are used instead.
 */
enum class BackingStateCategory(
    internal val fallbackHitbox: BlockData,
    internal vararg val backingStateConfigTypes: BackingStateConfigType<*>
) {
    
    /**
     * The block model uses note block states.
     *
     * - Limited to 1149 different models
     * - Models cannot be transparent
     * - Full block hitbox
     * - Full block collider
     * - Cannot be waterlogged
     * - Client-side arm swing animation on right-click
     */
    NOTE_BLOCK(
        fallbackHitbox = Material.BARRIER,
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
        fallbackHitbox = Material.BARRIER,
        RedMushroomBackingStateConfig, BrownMushroomBackingStateConfig, MushroomStemBackingStateConfig
    ),
    
    /**
     * The block model uses leave block states.
     *
     * - Limited to 130 different models
     * - Models can be transparent, translucent textures are disabled on "fancy" graphics setting
     * - Full block hitbox
     * - Full block collider
     * - Can be waterlogged
     * - Some shaders might animate blocks of this type to blow in the wind
     */
    LEAVES(
        fallbackHitbox = Material.BARRIER,
        OakLeavesBackingStateConfig, SpruceLeavesBackingStateConfig, BirchLeavesBackingStateConfig,
        JungleLeavesBackingStateConfig, AcaciaLeavesBackingStateConfig, DarkOakLeavesBackingStateConfig,
        MangroveLeavesBackingStateConfig, CherryLeavesBackingStateConfig, AzaleaLeavesBackingStateConfig,
        FloweringAzaleaLeavesBackingStateConfig
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
        fallbackHitbox = Material.STRUCTURE_VOID,
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
        fallbackHitbox = Material.STRUCTURE_VOID,
        TripwireBackingStateConfigType.Attached
    );
    
    constructor(fallbackHitbox: Material, vararg backingStateConfigTypes: BackingStateConfigType<*>) :
        this(fallbackHitbox.createBlockData(), *backingStateConfigTypes)
    
}