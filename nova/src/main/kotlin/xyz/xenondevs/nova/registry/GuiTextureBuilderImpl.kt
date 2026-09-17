package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import org.joml.Vector2i
import org.joml.Vector2ic
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureLayoutBuilder
import xyz.xenondevs.nova.resources.builder.task.GuiTextureData
import xyz.xenondevs.nova.resources.builder.task.GuiTextureTask
import xyz.xenondevs.nova.resources.builder.task.MovedFontContent
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture.TitleLine
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture.TitlePosition
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture.TitlePosition.Alignment
import xyz.xenondevs.nova.util.component.adventure.getFontsRecursively

internal class GuiTextureBuilderImpl(
    override val entry: RegistryEntry.Nova<GuiTexture>
) : GuiTextureBuilder, RegistryElementBuilder.Nova<GuiTexture> {
    
    override val tags: Provider<Set<RegistryEntrySet.Nova.Tag<GuiTexture>>>
        field = mutableProvider(emptySet())
    
    private var hasInventoryLabel: Boolean = true
    private var configureLayout: GuiTextureLayoutBuilder.() -> Unit = {}
    private var titleLines: List<TitleLine> = [TitleLine.Dynamic(TitlePosition(), setOf(Key.key("default")))]
    
    private lateinit var data: Provider<GuiTextureData>
    
    override fun tags(vararg tags: RegistryEntrySet.Nova.Tag<GuiTexture>) {
        this.tags.set(this.tags.get() + tags)
    }
    
    override fun inventoryLabel(inventoryLabel: Boolean) {
        hasInventoryLabel = inventoryLabel
    }
    
    override fun title(title: GuiTextureTitleBuilder.() -> Unit) {
        val titleBuilder = GuiTextureTitleBuilderImpl()
        titleBuilder.title()
        titleLines = titleBuilder.lines.toList()
    }
    
    override fun texture(texture: GuiTextureLayoutBuilder.() -> Unit) {
        configureLayout = texture
    }
    
    override fun prepareBuild() {
        data = GuiTextureTask.request(entry) { rpb ->
            // request moved fonts for all used fonts and offsets
            val mfc = rpb.getBuildData<MovedFontContent>()
            for (line in titleLines) {
                val fonts = when (line) {
                    is TitleLine.Static -> line.text.getFontsRecursively()
                    is TitleLine.Dynamic -> line.fonts
                }
                for (font in fonts) {
                    mfc.requestMovedFonts(
                        ResourcePath.of(ResourceType.Font, font),
                        setOf(line.position.offset.y())
                    )
                }
            }
            
            // create the actual gui texture
            val builder = GuiTextureLayoutBuilder(entry.key.namespace(), entry.key.value(), rpb)
            builder.configureLayout()
            builder.build()
        }
    }
    
    override fun build() = GuiTexture(entry, data, titleLines, hasInventoryLabel)
    
}

internal class GuiTextureTitleBuilderImpl : GuiTextureTitleBuilder {
    
    val lines = mutableListOf<TitleLine>()
    
    override fun staticLine(
        text: Component,
        alignment: Alignment,
        offset: Vector2ic
    ) {
        lines += TitleLine.Static(TitlePosition(alignment, Vector2i(offset)), text)
    }
    
    override fun dynamicLine(
        alignment: Alignment,
        offset: Vector2ic,
        fonts: Set<Key>
    ) {
        lines += TitleLine.Dynamic(TitlePosition(alignment, Vector2i(offset)), fonts.toSet())
    }
    
}
