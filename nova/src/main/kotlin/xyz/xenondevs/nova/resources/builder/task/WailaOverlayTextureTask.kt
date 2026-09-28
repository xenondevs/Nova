package xyz.xenondevs.nova.resources.builder.task

import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.font.Font
import xyz.xenondevs.nova.resources.builder.font.provider.bitmap.BitmapProvider
import xyz.xenondevs.nova.ui.waila.WailaManager
import java.awt.Color
import java.awt.RenderingHints
import java.awt.image.BufferedImage

internal object WailaBackgroundTextures {
    
    const val MIN_HEIGHT = 20
    const val MAX_HEIGHT = 128
    
    internal val FONT = ResourcePath(ResourceType.Font, "nova", "waila_frame")
    
    private const val START_CODE_POINT = 0xE000
    private const val PARTS_PER_HEIGHT = 3
    
    fun start(height: Int): FontChar = get(height, 0)
    fun part(height: Int): FontChar = get(height, 1)
    fun end(height: Int): FontChar = get(height, 2)
    
    private fun get(height: Int, part: Int): FontChar {
        require(height in MIN_HEIGHT..MAX_HEIGHT)
        require(part in 0..<PARTS_PER_HEIGHT)
        return FontChar(FONT, START_CODE_POINT + (height - MIN_HEIGHT) * PARTS_PER_HEIGHT + part)
    }
    
}

internal object WailaEnergyBarTextures {
    
    const val END_PIECE_WIDTH = 4
    const val MIDDLE_PIECE_WIDTH = 3
    const val MIDDLE_PIECE_COUNT = 4
    
    internal val FONT = ResourcePath(ResourceType.Font, "nova", "waila_energy_bar")
    
    val barPart = FontChar(FONT, 0xE000)
    val start = FontChar(FONT, 0xE001)
    private val middle = List(MIDDLE_PIECE_COUNT) { FontChar(FONT, 0xE002 + it) }
    val end = FontChar(FONT, 0xE006)
    
    fun middle(index: Int): FontChar {
        require(index >= 0)
        return middle[index % middle.size]
    }
    
}

private val WAILA_FRAME = ResourcePath(ResourceType.Texture, "nova", "font/waila/frame")
private val WAILA_ENERGY_BAR = ResourcePath(ResourceType.Texture, "nova", "font/waila/energy_bar")

private class WailaBackgroundNineSlice(
    private val source: BufferedImage
) {
    
    init {
        require(source.width >= 5 && source.height >= 5) { "WAILA background nine-slice texture must be at least 5x5 pixels" }
    }
    
    fun createStart(height: Int): BufferedImage = createGlyph(0, 2, 2, height)
    fun createPart(height: Int): BufferedImage = createGlyph(2, source.width - 2, 1, height)
    fun createEnd(height: Int): BufferedImage = createGlyph(source.width - 2, source.width, 2, height)
    
    private fun createGlyph(sourceX0: Int, sourceX1: Int, width: Int, height: Int): BufferedImage {
        val result = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val graphics = result.createGraphics()
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
        graphics.drawImage(source, 0, 0, width, 2, sourceX0, 0, sourceX1, 2, null)
        graphics.drawImage(source, 0, 2, width, height - 2, sourceX0, 2, sourceX1, source.height - 2, null)
        graphics.drawImage(source, 0, height - 2, width, height, sourceX0, source.height - 2, sourceX1, source.height, null)
        graphics.dispose()
        return result
    }
    
}

class WailaOverlayTextureTask(
    private val builder: ResourcePackBuilder
) : PackTask {
    
    override val stage = BuildStage.POST_WORLD
    override val runsBefore = setOf(MovedFontContent.Write::class, FontContent.Write::class)
    
    private val fontContent by builder.getBuildDataLazily<FontContent>()
    private val movedFontContent by builder.getBuildDataLazily<MovedFontContent>()
    
    override suspend fun run() {
        if (!WailaManager.ENABLED)
            return
        
        val textureContent = builder.getBuildData<TextureContent>()
        createFrameFont(textureContent.getImage(WAILA_FRAME))
        createEnergyBarFont(textureContent.getImage(WAILA_ENERGY_BAR))
    }
    
    private fun createFrameFont(source: BufferedImage) {
        val nineSlice = WailaBackgroundNineSlice(source)
        val font = Font(WailaBackgroundTextures.FONT)
        
        for (height in WailaBackgroundTextures.MIN_HEIGHT..WailaBackgroundTextures.MAX_HEIGHT) {
            addChar(font, WailaBackgroundTextures.start(height), "frame/$height/start", nineSlice.createStart(height), 0)
            addChar(font, WailaBackgroundTextures.part(height), "frame/$height/part", nineSlice.createPart(height), 0)
            addChar(font, WailaBackgroundTextures.end(height), "frame/$height/end", nineSlice.createEnd(height), 0)
        }
        
        fontContent += font
        movedFontContent.requestMovedFonts(font.id, 1..19)
    }
    
    private fun createEnergyBarFont(source: BufferedImage) {
        val endWidth = WailaEnergyBarTextures.END_PIECE_WIDTH
        val middleWidth = WailaEnergyBarTextures.MIDDLE_PIECE_WIDTH
        val expectedWidth = endWidth * 2 + middleWidth * WailaEnergyBarTextures.MIDDLE_PIECE_COUNT
        require(source.width == expectedWidth) { "WAILA energy bar texture must be ${expectedWidth}px wide" }
        require(source.height >= 3) { "WAILA energy bar texture must be at least 3px high" }
        
        val font = Font(WailaEnergyBarTextures.FONT)
        val barPart = BufferedImage(1, source.height - 2, BufferedImage.TYPE_INT_ARGB)
        repeat(barPart.height) { y -> barPart.setRGB(0, y, Color.WHITE.rgb) }
        
        addChar(font, WailaEnergyBarTextures.barPart, "energy_bar/bar_part", barPart, barPart.height - 1)
        addChar(font, WailaEnergyBarTextures.start, "energy_bar/start", source.getSubimage(0, 0, endWidth, source.height), source.height - 2)
        repeat(WailaEnergyBarTextures.MIDDLE_PIECE_COUNT) { index ->
            addChar(
                font,
                WailaEnergyBarTextures.middle(index),
                "energy_bar/middle_$index",
                source.getSubimage(endWidth + index * middleWidth, 0, middleWidth, source.height),
                source.height - 2
            )
        }
        addChar(
            font,
            WailaEnergyBarTextures.end,
            "energy_bar/end",
            source.getSubimage(source.width - endWidth, 0, endWidth, source.height),
            source.height - 2
        )
        
        fontContent += font
        movedFontContent.requestMovedFonts(font.id, 1..19)
    }
    
    private fun addChar(font: Font, char: FontChar, path: String, image: BufferedImage, ascent: Int) {
        val imagePath = ResourcePath(ResourceType.FontTexture, "nova", "waila_overlay/$path.png")
        font += BitmapProvider.single(imagePath, image, char.codePoint, image.height, ascent)
    }
    
}
