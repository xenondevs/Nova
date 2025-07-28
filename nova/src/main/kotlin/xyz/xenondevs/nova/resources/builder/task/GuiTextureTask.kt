package xyz.xenondevs.nova.resources.builder.task

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.util.data.readImageDimensions

@Serializable
internal class GuiTextureData(
    @Serializable(with = KeySerializer::class)
    val font: Key,
    val codePoint: Int,
    val offset: Int
)

/**
 * Generates gui texture assets.
 */
class GuiTextureTask(
    builder: ResourcePackBuilder
) : CustomFontContent(
    builder,
    "nova:gui_%s",
    true
), PackTask {
    
    override val stage = BuildStage.PRE_WORLD
    override val runBefore = setOf(FontContent.Write::class)
    
    override suspend fun run() {
        val guiTextures = HashMap<GuiTexture, GuiTextureData>()
        
        for (guiTexture in NovaRegistries.GUI_TEXTURE) {
            val layout = guiTexture.makeLayout(builder)
            val texture = layout.texture.toType(ResourceType.FontTexture)
            val dim = builder.resolve(texture).readImageDimensions()
            val offset = layout.alignment.getOffset(dim.width, dim.height)
            
            val fontChar = addEntry(guiTexture.id.toString(), texture, dim.height, -offset.y())
            guiTextures[guiTexture] = GuiTextureData(fontChar.font, fontChar.codePoint, offset.x())
        }
        
        ResourceLookups.GUI_TEXTURE = guiTextures
    }
    
}