package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeavesBlock
import xyz.xenondevs.nova.util.intValue

internal abstract class LeavesBackingStateConfig(block: Block, distance: Int, persistent: Boolean, override val waterlogged: Boolean) : BackingStateConfig() {
    
    override val id = (distance - 1) shl 1 or persistent.intValue
    override val variantString = "distance=$distance,persistent=$persistent,waterlogged=$waterlogged"
    override val vanillaBlockState = block.defaultBlockState()
        .setValue(LeavesBlock.DISTANCE, distance)
        .setValue(LeavesBlock.PERSISTENT, persistent)
        .setValue(LeavesBlock.WATERLOGGED, waterlogged)
    
}

internal abstract class LeavesBackingStateConfigType<T : LeavesBackingStateConfig>(
    private val ctor: (distance: Int, persistent: Boolean, waterlogged: Boolean) -> T,
    fileName: String,
) : DefaultingBackingStateConfigType<T>(13, fileName) {
    
    override val defaultStateConfig = ctor(7, true, false)
    override val blockedIds = hashSetOf(13)
    override val properties = hashSetOf("distance", "persistent", "waterlogged")
    override val isWaterloggable = true
    open val particleType = "tinted_leaves"
    
    override fun of(id: Int, waterlogged: Boolean): T {
        return ctor((id shr 1) + 1, (id and 1) == 1, waterlogged)
    }
    
    override fun of(properties: Map<String, String>): T {
        return ctor(
            properties["distance"]?.toInt() ?: 1,
            properties["persistent"]?.toBoolean() == true,
            properties["waterlogged"]?.toBoolean() == true
        )
    }
    
}

internal class OakLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.OAK_LEAVES, distance, persistent, waterlogged) {
    override val type = OakLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<OakLeavesBackingStateConfig>(::OakLeavesBackingStateConfig, "oak_leaves")
}

internal class SpruceLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.SPRUCE_LEAVES, distance, persistent, waterlogged) {
    override val type = SpruceLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<SpruceLeavesBackingStateConfig>(::SpruceLeavesBackingStateConfig, "spruce_leaves")
}

internal class BirchLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.BIRCH_LEAVES, distance, persistent, waterlogged) {
    override val type = BirchLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<BirchLeavesBackingStateConfig>(::BirchLeavesBackingStateConfig, "birch_leaves")
}

internal class JungleLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.JUNGLE_LEAVES, distance, persistent, waterlogged) {
    override val type = JungleLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<JungleLeavesBackingStateConfig>(::JungleLeavesBackingStateConfig, "jungle_leaves")
}

internal class AcaciaLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.ACACIA_LEAVES, distance, persistent, waterlogged) {
    override val type = AcaciaLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<AcaciaLeavesBackingStateConfig>(::AcaciaLeavesBackingStateConfig, "acacia_leaves")
}

internal class DarkOakLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.DARK_OAK_LEAVES, distance, persistent, waterlogged) {
    override val type = DarkOakLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<DarkOakLeavesBackingStateConfig>(::DarkOakLeavesBackingStateConfig, "dark_oak_leaves")
}

internal class MangroveLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.MANGROVE_LEAVES, distance, persistent, waterlogged) {
    override val type = MangroveLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<MangroveLeavesBackingStateConfig>(::MangroveLeavesBackingStateConfig, "mangrove_leaves")
}

internal class CherryLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.CHERRY_LEAVES, distance, persistent, waterlogged) {
    override val type = CherryLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<CherryLeavesBackingStateConfig>(::CherryLeavesBackingStateConfig, "cherry_leaves") {
        override val particleType = "cherry_leaves"
    }
}

internal class AzaleaLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.AZALEA_LEAVES, distance, persistent, waterlogged) {
    override val type = AzaleaLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<AzaleaLeavesBackingStateConfig>(::AzaleaLeavesBackingStateConfig, "azalea_leaves")
}

internal class FloweringAzaleaLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.FLOWERING_AZALEA_LEAVES, distance, persistent, waterlogged) {
    override val type = FloweringAzaleaLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<FloweringAzaleaLeavesBackingStateConfig>(::FloweringAzaleaLeavesBackingStateConfig, "flowering_azalea_leaves")
}

internal class PaleOakLeavesBackingStateConfig(
    distance: Int,
    persistent: Boolean,
    waterlogged: Boolean
) : LeavesBackingStateConfig(Blocks.CHERRY_LEAVES, distance, persistent, waterlogged) {
    override val type = PaleOakLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<PaleOakLeavesBackingStateConfig>(::PaleOakLeavesBackingStateConfig, "pale_oak_leaves") {
        override val particleType = "pale_oak_leaves"
    }
}