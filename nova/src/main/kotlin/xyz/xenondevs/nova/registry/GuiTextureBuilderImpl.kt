package xyz.xenondevs.nova.registry

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.resources.builder.task.GuiTextureData
import xyz.xenondevs.nova.resources.builder.task.GuiTextureTask
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture

internal class GuiTextureBuilderImpl(
    private val entry: RegistryEntry.Nova<GuiTexture>
) : GuiTextureBuilder, RegistryElementBuilder.Nova<GuiTexture> {
    
    private var hasInventoryLabel: Boolean = true
    private var configureLayout: GuiTextureLayoutBuilder.() -> Unit = {}
    
    private lateinit var data: Provider<GuiTextureData>
    
    override fun inventoryLabel(inventoryLabel: Boolean) {
        hasInventoryLabel = inventoryLabel
    }
    
    override fun texture(texture: GuiTextureLayoutBuilder.() -> Unit) {
        configureLayout = texture
    }
    
    override fun prepareBuild() {
        data = GuiTextureTask.request(entry) { rpb ->
            val builder = GuiTextureLayoutBuilder(entry.key.namespace(), rpb)
            builder.configureLayout()
            builder.build()
        }
    }
    
    override fun build() = GuiTexture(entry, data, hasInventoryLabel)
    
}