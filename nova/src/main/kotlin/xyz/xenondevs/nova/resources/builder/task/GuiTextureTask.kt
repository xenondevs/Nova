package xyz.xenondevs.nova.resources.builder.task

import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.getOrThrow
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.layout.gui.GuiTextureLayout
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
    val offset: Int
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
        val guiTextures = HashMap<RegistryEntry.Nova<GuiTexture>, GuiTextureData>()
        val guiTexturesByFontChar = HashMap<FontChar, RegistryEntry.Nova<GuiTexture>>()
        
        for ((guiTexture, makeLayout) in requests) {
            val layout = makeLayout(builder)
            val texture = layout.texture.toType(ResourceType.FontTexture)
            val dim = builder.resolve(texture).readImageDimensions()
            val offset = layout.alignment.getOffset(dim.width, dim.height)
            
            val fontChar = addEntry(guiTexture.key.toString(), texture, dim.height, -offset.y())
            guiTextures[guiTexture] = GuiTextureData(fontChar.font, fontChar.codePoint, offset.x())
            guiTexturesByFontChar[fontChar] = guiTexture
        }
        
        ResourceLookups.guiTexture = guiTextures
        ResourceLookups.guiTextureByFontChar = guiTexturesByFontChar
    }
    
    internal companion object {
        
        private val requests = HashMap<RegistryEntry.Nova<GuiTexture>, (ResourcePackBuilder) -> GuiTextureLayout>()
        
        /**
         * Requests the generation of [entry] using [makeLayout].
         * Results will be written to [ResourceLookups.guiTextureLookup] and [ResourceLookups.guiTextureByFontChar]
         * and are also available in the returned provider.
         */
        fun request(
            entry: RegistryEntry.Nova<GuiTexture>,
            makeLayout: (ResourcePackBuilder) -> GuiTextureLayout
        ): Provider<GuiTextureData> {
            requests[entry] = makeLayout
            return ResourceLookups.guiTextureLookup.getOrThrow(entry)
        }
        
    }
    
}