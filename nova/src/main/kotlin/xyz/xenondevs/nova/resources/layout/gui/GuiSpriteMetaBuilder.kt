package xyz.xenondevs.nova.resources.layout.gui

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.GuiSpriteMcMeta
import xyz.xenondevs.nova.resources.builder.data.SpriteScalingType

@RegistryElementBuilderDsl
class GuiSpriteMetaBuilder internal constructor(val resourcePackBuilder: ResourcePackBuilder) {
    
    private var type = SpriteScalingType.STRETCH
    private var width: Int? = null
    private var height: Int? = null
    private var stretchInner = false
    private var border: GuiSpriteMcMeta.Gui.Scaling.Border? = null
    
    fun type(type: SpriteScalingType) {
        this.type = type
    }
    
    fun dimensions(width: Int, height: Int) {
        this.width = width
        this.height = height
    }
    
    fun stretchInner(stretchInner: Boolean) {
        this.stretchInner = stretchInner
    }
    
    fun border(left: Int, right: Int, top: Int, bottom: Int) {
        border = GuiSpriteMcMeta.Gui.Scaling.Border(left, right, top, bottom)
    }
    
    fun border(size: Int) {
        border = GuiSpriteMcMeta.Gui.Scaling.Border(size)
    }
    
    internal fun build() = GuiSpriteMcMeta(
        GuiSpriteMcMeta.Gui(
            GuiSpriteMcMeta.Gui.Scaling(
                type,
                width,
                height,
                stretchInner,
                border
            )
        )
    )
    
}