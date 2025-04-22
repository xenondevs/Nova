package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.key.Key
import org.bukkit.Material
import org.bukkit.block.data.type.TestBlock
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.world.BlockPos

object TestBlockWailaInfoProvider : VanillaWailaInfoProvider<TestBlock>(setOf(Material.TEST_BLOCK)) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: TestBlock): WailaInfo {
        val info = DefaultVanillaWailaInfoProvider.getInfo(player, pos, blockState)
        info.icon = Key.key("test_block_${blockState.mode.name.lowercase()}")
        return info
    }
    
}