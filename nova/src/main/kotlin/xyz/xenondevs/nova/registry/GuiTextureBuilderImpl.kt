package xyz.xenondevs.nova.registry

import net.kyori.adventure.text.Component
import org.joml.Vector2i
import org.joml.Vector2ic
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.resources.builder.task.GuiTextureData
import xyz.xenondevs.nova.resources.builder.task.GuiTextureTask
import xyz.xenondevs.nova.resources.builder.task.MovedFontContent
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture.TitlePosition
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture.TitlePosition.Alignment
import xyz.xenondevs.nova.util.component.adventure.getFontsRecursively

internal class GuiTextureBuilderImpl(
    override val entry: RegistryEntry.Nova<GuiTexture>
) : GuiTextureBuilder, RegistryElementBuilder.Nova<GuiTexture> {
    
    private var hasInventoryLabel: Boolean = true
    private var configureLayout: GuiTextureLayoutBuilder.() -> Unit = {}
    private var titlePosition = TitlePosition()
    private val extraLines = mutableListOf<Pair<Component, TitlePosition>>()
    
    private lateinit var data: Provider<GuiTextureData>
    
    override fun inventoryLabel(inventoryLabel: Boolean) {
        hasInventoryLabel = inventoryLabel
    }
    
    override fun title(title: GuiTextureTitleBuilder.() -> Unit) {
        val titleBuilder = GuiTextureTitleBilderImpl()
        titleBuilder.title()
        titlePosition = titleBuilder.titlePosition
        extraLines += titleBuilder.extraLines
    }
    
    override fun texture(texture: GuiTextureLayoutBuilder.() -> Unit) {
        configureLayout = texture
    }
    
    override fun prepareBuild() {
        data = GuiTextureTask.request(entry) { rpb ->
            // request moved fonts for all used fonts and offsets
            val mfc = rpb.getBuildData<MovedFontContent>()
            mfc.requestMovedFonts(
                ResourcePath(ResourceType.Font, "minecraft", "default"),
                setOf(titlePosition.offset.y())
            )
            for ((text, position) in extraLines) {
                for (font in text.getFontsRecursively()) {
                    mfc.requestMovedFonts(
                        ResourcePath.of(ResourceType.Font, font),
                        setOf(position.offset.y())
                    )
                }
            }
            
            // create the actual gui texture
            val builder = GuiTextureLayoutBuilder(entry.key.namespace(), entry.key.value(), rpb)
            builder.configureLayout()
            builder.build()
        }
    }
    
    override fun build() = GuiTexture(entry, data, titlePosition, extraLines, hasInventoryLabel)
    
}

internal class GuiTextureTitleBilderImpl : GuiTextureTitleBuilder {
    
    var titlePosition = TitlePosition()
        private set
    val extraLines = mutableListOf<Pair<Component, TitlePosition>>()
    
    override fun alignment(
        alignment: Alignment,
        offset: Vector2ic
    ) {
        titlePosition = TitlePosition(alignment, Vector2i(offset))
    }
    
    override fun line(
        text: Component,
        alignment: Alignment,
        offset: Vector2ic
    ) {
        extraLines += text to TitlePosition(alignment, Vector2i(offset))
    }
    
}