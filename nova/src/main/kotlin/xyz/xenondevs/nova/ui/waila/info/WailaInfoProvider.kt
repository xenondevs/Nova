package xyz.xenondevs.nova.ui.waila.info

import net.kyori.adventure.text.Component
import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState

data class WailaLine(val text: Component, val alignment: Alignment) {
    
    enum class Alignment {
        LEFT,
        CENTERED,
        FIRST_LINE,
        PREVIOUS_LINE
    }
    
}

data class WailaInfo(var icon: ResourceLocation, var lines: MutableList<WailaLine>)

sealed interface WailaInfoProvider<T> {
    fun getInfo(player: Player, pos: BlockPos, block: T): WailaInfo
}

abstract class NovaWailaInfoProvider : WailaInfoProvider<NovaBlockState> {
    
    val addon: Addon?
    val blocks: Set<NovaBlock>?
    
    constructor(addon: Addon) {
        this.addon = addon
        this.blocks = null
    }
    
    constructor(blocks: Set<NovaBlock>?) {
        this.addon = null
        this.blocks = blocks
    }
    
}

abstract class VanillaWailaInfoProvider(val materials: Set<Material>?) : WailaInfoProvider<Block>