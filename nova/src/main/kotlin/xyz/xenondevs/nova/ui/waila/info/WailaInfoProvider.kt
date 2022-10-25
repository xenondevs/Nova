package xyz.xenondevs.nova.ui.waila.info

import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.integration.customitems.CustomItemService
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.util.data.MovingComponentBuilder

class WailaLine(val components: Array<out BaseComponent>, val alignment: Alignment) {
    
    constructor(builder: ComponentBuilder, alignment: Alignment) : this(
        builder.create(),
        alignment
    )
    
    constructor(builder: MovingComponentBuilder, alignment: Alignment) : this(
        builder.create(),
        alignment
    )
    
    operator fun component1() = components
    operator fun component2() = alignment
    
    enum class Alignment {
        LEFT,
        CENTERED,
        FIRST_LINE,
        PREVIOUS_LINE
    }
    
}

class WailaInfo(var icon: NamespacedId, var lines: MutableList<WailaLine>)

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

internal abstract class CustomItemServiceWailaInfoProvider(val services: Set<CustomItemService>?) : WailaInfoProvider<Block>