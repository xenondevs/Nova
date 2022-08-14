package xyz.xenondevs.nova.ui.waila.info

import net.md_5.bungee.api.chat.BaseComponent
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.material.BlockNovaMaterial
import xyz.xenondevs.nova.ui.overlay.character.DefaultFont
import xyz.xenondevs.nova.util.data.toPlainText

class WailaInfo {
    
    val icon: NamespacedId
    val text: List<Array<BaseComponent>>
    val widths: List<Int>
    
    constructor(icon: NamespacedId, text: List<Array<BaseComponent>>, width: List<Int>) {
        this.icon = icon
        this.text = text
        this.widths = width
    }
    
    constructor(icon: NamespacedId, text: List<Array<BaseComponent>>, player: Player) {
        this.icon = icon
        this.text = text
        this.widths = text.map { DefaultFont.getStringLength(it.toPlainText(player.locale)) }
    }
    
}

sealed interface WailaInfoProvider<T> {
    fun getInfo(player: Player, block: T): WailaInfo
}

abstract class NovaWailaInfoProvider : WailaInfoProvider<NovaBlockState> {
    
    val addon: Addon?
    val materials: List<BlockNovaMaterial>?
    
    constructor(addon: Addon) {
        this.addon = addon
        this.materials = null
    }
    
    constructor(materials: List<BlockNovaMaterial>?) {
        this.addon = null
        this.materials = materials
    }
    
}

abstract class VanillaWailaInfoProvider(val materials: List<Material>?) : WailaInfoProvider<Block>