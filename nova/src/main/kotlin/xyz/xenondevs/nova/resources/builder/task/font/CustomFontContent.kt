package xyz.xenondevs.nova.resources.builder.task.font

import net.kyori.adventure.text.Component
import xyz.xenondevs.nova.resources.CharSizes
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.Font
import xyz.xenondevs.nova.resources.builder.font.provider.bitmap.BitmapProvider
import xyz.xenondevs.nova.resources.builder.task.PackTaskHolder
import xyz.xenondevs.nova.util.component.adventure.font
import xyz.xenondevs.nova.util.data.readImageDimensions

private const val START_CODE_POINT: Int = 0xE000
private const val END_CODE_POINT: Int = 0xF8FF

abstract class CustomFontContent internal constructor(
    protected val builder: ResourcePackBuilder,
    private val fontNameTemplate: String,
    private val generateMovedVariants: Boolean
) : PackTaskHolder {
    
    private val fontContent by builder.getHolderLazily<FontContent>()
    private val movedFontContent by builder.getHolderLazily<MovedFontContent>()
    
    protected val fontCharLookup = HashMap<ResourcePath, FontChar>()
    private lateinit var currentFont: Font
    private var currentCodePoint = END_CODE_POINT
    private var currentFontNum = -1
    
    fun addEntry(charId: String, image: ResourcePath, height: Int?, ascent: Int): FontChar {
        return addEntry(ResourcePath.of(charId), image, height, ascent)
    }
    
    fun addEntry(charId: ResourcePath, image: ResourcePath, height: Int?, ascent: Int): FontChar {
        if (++currentCodePoint > 0xF8FF) {
            currentCodePoint = START_CODE_POINT
            val id = ResourcePath.of(fontNameTemplate.format(++currentFontNum))
            val font = Font(id)
            fontContent += font
            if (generateMovedVariants) movedFontContent.requestMovedFonts(id, 1..19)
            this.currentFont = font
        }
        
        currentFont += BitmapProvider.single(
            image,
            currentCodePoint,
            height ?: image.findInAssets("textures").readImageDimensions().height,
            ascent
        )
        
        val fontChar = FontChar(currentFont.id, currentCodePoint)
        fontCharLookup[charId] = fontChar
        return fontChar
    }
    
}

class FontChar internal constructor(val font: String, val codePoint: Int) {
    
    internal constructor(font: ResourcePath, codePoint: Int) : this(font.toString(), codePoint)
    
    val width by lazy { CharSizes.getCharWidth(font, codePoint) }
    val yRange by lazy { CharSizes.getCharYRange(font, codePoint) }
    
    val component: Component
        get() = Component.text()
            .content(Character.toString(codePoint))
            .font(font)
            .build()
    
}