package xyz.xenondevs.nova.world.block.state.model

import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.LeavesBlock
import xyz.xenondevs.nova.util.intValue

internal abstract class LeavesBackingStateConfig(block: Block, distance: Int, persistent: Boolean) : BackingStateConfig() {
    
    override val id = (distance - 1) shl 1 or persistent.intValue
    override val variantString = "distance=$distance,persistent=$persistent"
    override val vanillaBlockState = block.defaultBlockState()
        .setValue(LeavesBlock.DISTANCE, distance)
        .setValue(LeavesBlock.PERSISTENT, persistent)
    
}

internal abstract class LeavesBackingStateConfigType<T : LeavesBackingStateConfig>(
    private val ctor: (distance: Int, persistent: Boolean) -> T,
    fileName: String,
) : DefaultingBackingStateConfigType<T>(13, fileName) {
    
    override val defaultStateConfig = ctor(7, true)
    override val blockedIds = hashSetOf(13)
    
    override fun of(id: Int): T {
        return ctor((id shr 1) + 1, (id and 1) == 1)
    }
    
    override fun of(properties: Map<String, String>): T {
        return ctor(
            properties["distance"]?.toInt() ?: 0,
            properties["persistent"]?.toBoolean() ?: false
        )
    }
    
}

internal class OakLeavesBackingStateConfig(distance: Int, persistent: Boolean) : LeavesBackingStateConfig(Blocks.OAK_LEAVES, distance, persistent) {
    override val type = OakLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<OakLeavesBackingStateConfig>(::OakLeavesBackingStateConfig, "oak_leaves")
}

internal class SpruceLeavesBackingStateConfig(distance: Int, persistent: Boolean) : LeavesBackingStateConfig(Blocks.SPRUCE_LEAVES, distance, persistent) {
    override val type = SpruceLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<SpruceLeavesBackingStateConfig>(::SpruceLeavesBackingStateConfig, "spruce_leaves")
}

internal class BirchLeavesBackingStateConfig(distance: Int, persistent: Boolean) : LeavesBackingStateConfig(Blocks.BIRCH_LEAVES, distance, persistent) {
    override val type = BirchLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<BirchLeavesBackingStateConfig>(::BirchLeavesBackingStateConfig, "birch_leaves")
}

internal class JungleLeavesBackingStateConfig(distance: Int, persistent: Boolean) : LeavesBackingStateConfig(Blocks.JUNGLE_LEAVES, distance, persistent) {
    override val type = JungleLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<JungleLeavesBackingStateConfig>(::JungleLeavesBackingStateConfig, "jungle_leaves")
}

internal class AcaciaLeavesBackingStateConfig(distance: Int, persistent: Boolean) : LeavesBackingStateConfig(Blocks.ACACIA_LEAVES, distance, persistent) {
    override val type = AcaciaLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<AcaciaLeavesBackingStateConfig>(::AcaciaLeavesBackingStateConfig, "acacia_leaves")
}

internal class DarkOakLeavesBackingStateConfig(distance: Int, persistent: Boolean) : LeavesBackingStateConfig(Blocks.DARK_OAK_LEAVES, distance, persistent) {
    override val type = DarkOakLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<DarkOakLeavesBackingStateConfig>(::DarkOakLeavesBackingStateConfig, "dark_oak_leaves")
}

internal class MangroveLeavesBackingStateConfig(distance: Int, persistent: Boolean) : LeavesBackingStateConfig(Blocks.MANGROVE_LEAVES, distance, persistent) {
    override val type = MangroveLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<MangroveLeavesBackingStateConfig>(::MangroveLeavesBackingStateConfig, "mangrove_leaves")
}

internal class CherryLeavesBackingStateConfig(distance: Int, persistent: Boolean) : LeavesBackingStateConfig(Blocks.CHERRY_LEAVES, distance, persistent) {
    override val type = CherryLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<CherryLeavesBackingStateConfig>(::CherryLeavesBackingStateConfig, "cherry_leaves")
}

internal class AzaleaLeavesBackingStateConfig(distance: Int, persistent: Boolean) : LeavesBackingStateConfig(Blocks.AZALEA_LEAVES, distance, persistent) {
    override val type = AzaleaLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<AzaleaLeavesBackingStateConfig>(::AzaleaLeavesBackingStateConfig, "azalea_leaves")
}

internal class FloweringAzaleaLeavesBackingStateConfig(distance: Int, persistent: Boolean) : LeavesBackingStateConfig(Blocks.FLOWERING_AZALEA_LEAVES, distance, persistent) {
    override val type = FloweringAzaleaLeavesBackingStateConfig
    
    companion object : LeavesBackingStateConfigType<FloweringAzaleaLeavesBackingStateConfig>(::FloweringAzaleaLeavesBackingStateConfig, "flowering_azalea_leaves")
}