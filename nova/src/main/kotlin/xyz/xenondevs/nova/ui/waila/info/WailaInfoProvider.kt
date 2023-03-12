package xyz.xenondevs.nova.ui.waila.info

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.material.BlockNovaMaterial

data class WailaLine(val text: Component, val alignment: Alignment) {
    
    enum class Alignment {
        LEFT,
        CENTERED,
        FIRST_LINE,
        PREVIOUS_LINE
    }
    
}

data class WailaInfo(var icon: NamespacedId, var lines: MutableList<WailaLine>)

sealed interface WailaInfoProvider<T> {
    fun getInfo(player: Player, block: T): WailaInfo
}

abstract class NovaWailaInfoProvider : WailaInfoProvider<NovaBlockState> {
    
    val addon: Addon?
    val materials: Set<BlockNovaMaterial>?
    
    constructor(addon: Addon) {
        this.addon = addon
        this.materials = null
    }
    
    constructor(materials: Set<BlockNovaMaterial>?) {
        this.addon = null
        this.materials = materials
    }
    
}

abstract class VanillaWailaInfoProvider(val materials: Set<Material>?) : WailaInfoProvider<Block>