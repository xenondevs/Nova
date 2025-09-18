package xyz.xenondevs.nova.resources.builder.task

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.ui.menu.VanillaGuiTextureInjector
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.util.data.readImageDimensions
import java.awt.image.BufferedImage
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists

@Serializable
internal class GuiTextureData(
    @Serializable(with = KeySerializer::class)
    val font: Key,
    val codePoint: Int,
    val offset: Int,
    val hasInventoryLabel: Boolean
)

/**
 * Textures to overwrite with transparent images.
 * Omissions:
 * - brewing stand: gui texture renders over brew progress
 * - crafting table: gui texture renders over knowledge book button
 * - furnace: gui texture renders over fuel and smelt progress and knowledge book button
 * - smithing table: gui texture renders over placeholder elements and armor stand
 * - stonecutter: gui texture renders over recipe buttons and scroll bar
 * - merchant: too large for a single font char; centered title that is influenced by villager level, which is sent in a different packet
 * - other: not an InvUI window type
 */
private val REPLACED_TEXTURES = setOf(
    "anvil",
    "cartography_table",
    "crafter",
    "dispenser",
    "generic_54",
    "grindstone",
    "hopper"
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
    override val runsAfter = setOf(ExtractTask::class, LanguageContent.LoadAll::class)
    override val runsBefore = setOf(MovedFontContent.Write::class, FontContent.Write::class, LanguageContent.Write::class)
    
    override suspend fun run() {
        overwriteDefaultContainerTextures()
        overwriteInventoryTitle()
        loadCustomGuiTextures()
    }
    
    /**
     * Overwrites the default container textures with translucent images and
     * copies the original textures to the Nova namespace so that they can be used in custom gui textures.
     */
    private fun overwriteDefaultContainerTextures() {
        val img = BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB)
        for (texture in REPLACED_TEXTURES) {
            val path = ResourcePath(ResourceType.Texture, "minecraft", "gui/container/$texture")
            builder.writeImage(path, img)
            
            val src = builder.resolveVanilla(path)
            val target = builder.resolve(ResourcePath(ResourceType.Texture, "nova", "gui/vanilla/$texture"))
            if (!target.exists()) {
                target.createParentDirectories()
                src.copyTo(target)
            }
        }
    }
    
    /**
     * Overwrites the "container.inventory" translation to be empty and moves the actual translation
     * to the nova namespace, so that it can be enabled dynamically via the title.
     */
    private fun overwriteInventoryTitle() {
        val lc = builder.getBuildData<LanguageContent>()
        for ((lang, _) in lc.vanillaLangs) {
            val translation = lc.getTranslation(lang, "container.inventory")
            lc.setTranslation(lang, "container.inventory", "")
            lc.setTranslation(lang, "container.nova.inventory", translation)
        }
        
        // request moved fonts that are required for the new inventory label to be offset to its correct position
        val mfc = builder.getBuildData<MovedFontContent>()
        mfc.requestMovedFonts(ResourcePath(ResourceType.Font, "minecraft", "default"), VanillaGuiTextureInjector.requiredVerticalOffsets)
        mfc.requestMovedFonts(ResourcePath(ResourceType.Font, "minecraft", "uniform"), VanillaGuiTextureInjector.requiredVerticalOffsets)
    }
    
    private fun loadCustomGuiTextures() {
        val guiTextures = HashMap<GuiTexture, GuiTextureData>()
        val guiTexturesByFontChar = HashMap<FontChar, GuiTexture>()
        
        for (guiTexture in NovaRegistries.GUI_TEXTURE) {
            val layout = guiTexture.makeLayout(builder)
            val texture = layout.texture.toType(ResourceType.FontTexture)
            val dim = builder.resolve(texture).readImageDimensions()
            val offset = layout.alignment.getOffset(dim.width, dim.height)
            
            val fontChar = addEntry(guiTexture.id.toString(), texture, dim.height, -offset.y())
            guiTextures[guiTexture] = GuiTextureData(fontChar.font, fontChar.codePoint, offset.x(), layout.hasInventoryLabel)
            guiTexturesByFontChar[fontChar] = guiTexture
        }
        
        ResourceLookups.GUI_TEXTURE = guiTextures
        ResourceLookups.GUI_TEXTURE_BY_FONT_CHAR = guiTexturesByFontChar
    }
    
}