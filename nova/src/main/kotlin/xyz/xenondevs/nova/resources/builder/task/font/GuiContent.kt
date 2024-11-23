package xyz.xenondevs.nova.resources.builder.task.font

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.task.PackTask
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.util.data.readImageDimensions

internal class GuiTextureData(
    val font: Key,
    val codePoint: Int,
    val offset: Int
)

class GuiContent internal constructor(
    builder: ResourcePackBuilder
) : CustomFontContent(
    builder,
    "nova:gui_%s",
    true
) {
    
    @PackTask(runBefore = ["FontContent#write"])
    private fun write() {
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