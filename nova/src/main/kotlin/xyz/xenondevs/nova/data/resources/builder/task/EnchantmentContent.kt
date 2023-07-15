package xyz.xenondevs.nova.data.resources.builder.task

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.nova.data.resources.CharSizes
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.font.Font
import xyz.xenondevs.nova.data.resources.builder.font.provider.bitmap.BitmapProvider
import xyz.xenondevs.nova.data.resources.builder.task.font.FontContent
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.item.enchantment.NovaEnchantment
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.ui.overlay.character.MoveCharacters
import xyz.xenondevs.nova.util.NumberFormatUtils
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.math.pow

class EnchantmentData(
    val vanillaEnchantment: ResourceLocation,
    val enchantments: Map<ResourceLocation, IntArray> // <Enchantment ID, levels>
)

/**
 * The color used for the tooltip background overlay.
 */
private val TOOLTIP_BACKGROUND_COLOR = Color(20, 4, 18)

/**
 * The height of the characters in the tooltip background overlay.
 */
private const val TOOLTIP_OVERLAY_CHAR_HEIGHT = 8

/**
 * Defines how many tooltip overlay chars will be generated. The width of the largest char will be 2^(size - 1)
 */
private const val TOOLTIP_OVERLAY_CHAR_AMOUNT = 6

class EnchantmentContent(builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val fontContent by builder.getHolderLazily<FontContent>()
    private val languageContent by builder.getHolderLazily<LanguageContent>()
    
    private var backgroundCharOffset: Int = -1
    
    @PackTask(
        runAfter = ["FontContent#discoverAllFonts"],
        runBefore = ["FontContent#write", "CharSizeCalculator#calculateCharSizes"]
    )
    private fun createBackgroundChars() {
        val mergedFonts = fontContent.mergedFonts
        
        val codePoints = IntOpenHashSet()
        codePoints.addAll(mergedFonts[Font.DEFAULT]!!.getCodePoints(mergedFonts.values))
        codePoints.addAll(mergedFonts[Font.UNIFORM]!!.getCodePoints(mergedFonts.values))
        
        val defaultFont = fontContent.getOrCreate(Font.DEFAULT)
        val uniformFont = fontContent.getOrCreate(Font.UNIFORM)
        
        val offset = Font.findFirstUnoccupiedRange(codePoints, Font.PRIVATE_USE_AREA, TOOLTIP_OVERLAY_CHAR_AMOUNT).first
        backgroundCharOffset = offset
        for (i in 0..<TOOLTIP_OVERLAY_CHAR_AMOUNT) {
            val width = 2.0.pow(i).toInt()
            val img = BufferedImage(width, TOOLTIP_OVERLAY_CHAR_HEIGHT, BufferedImage.TYPE_INT_ARGB)
            
            val graphics = img.createGraphics()
            graphics.color = TOOLTIP_BACKGROUND_COLOR
            graphics.fillRect(0, 0, width, TOOLTIP_OVERLAY_CHAR_HEIGHT)
            graphics.dispose()
            
            val provider = BitmapProvider.single(
                ResourcePath("nova", "font/tooltip_background/$i.png"),
                img, offset + i, TOOLTIP_OVERLAY_CHAR_HEIGHT, 7
            )
            
            defaultFont.addFirst(provider)
            uniformFont.addFirst(provider)
        }
    }
    
    @PackTask(
        runAfter = ["LanguageContent#loadLangFiles", "EnchantmentContent#createBackgroundChars", "CharSizeCalculator#calculateCharSizes"],
        runBefore = ["LanguageContent#write"]
    )
    private fun write() {
        val lookup = HashMap<String, EnchantmentData>() // <Lang, EnchantmentData>
        
        for ((lang, vanillaLangMap) in languageContent.vanillaLangs) {
            // find vanilla enchantment with shortest name
            val (vanillaEnchKey, vanillaEnchTranslation) = vanillaLangMap.asSequence()
                .filter { it.key.startsWith("enchantment.minecraft") }
                .sortedBy { CharSizes.calculateStringWidth("default", it.value) }
                .first()
            val vanillaEnchId = ResourceLocation("minecraft", vanillaEnchKey.substringAfterLast('.'))
            
            // redefine translation for the vanilla enchantment for the unlikely case that a player uses a custom resource pack that changes it
            languageContent.setTranslation(lang, vanillaEnchKey, vanillaEnchTranslation)
            
            // calculate enchantment translation length and build the appropriate overlay box string
            val vanillaEnchLength = (CharSizes.calculateStringWidth("default", vanillaEnchTranslation) + CharSizes.getCharWidth("default", ' ')).toInt() // sub-gui-scale width is not expected here, as that is only possible through custom space characters
            val overlayBoxStr = createOverlayBox(vanillaEnchLength)
            
            // create data map for lookups
            val dataMap = HashMap<ResourceLocation, IntArray>()
            lookup[lang] = EnchantmentData(vanillaEnchId, dataMap)
            
            // register nova enchantment translations
            var vanillaLevel = 255
            NovaRegistries.ENCHANTMENT.asSequence()
                .filterIsInstance<NovaEnchantment>()
                .forEach { novaEnch ->
                    if (vanillaLevel + novaEnch.maxLevel > Short.MAX_VALUE)
                        throw IllegalStateException("Maximum number of enchantments-level combinations exceeded")
                    
                    val novaEnchTranslation = languageContent.getTranslation(lang, novaEnch.localizedName)
                    val vanillaLevels = IntArray(novaEnch.maxLevel)
                    for (novaLevel in 0..<novaEnch.maxLevel) {
                        vanillaLevels[novaLevel] = ++vanillaLevel
                        languageContent.setTranslation(lang, "enchantment.level.$vanillaLevel", createLevelOverrideTranslation(overlayBoxStr, novaEnchTranslation, novaLevel))
                    }
                    
                    dataMap[novaEnch.id] = vanillaLevels
                }
        }
        
        ResourceLookups.ENCHANTMENT_DATA_LOOKUP = lookup
    }
    
    /**
     * Creates the string used to hide the vanilla enchantment name.
     */
    private fun createOverlayBox(length: Int): String = buildString {
        val minusLength = MoveCharacters.getMovingString(-length)
        val minusOne = MoveCharacters.getMovingString(-1f)
        
        append(minusLength)
        for (bit in 0..<TOOLTIP_OVERLAY_CHAR_AMOUNT) {
            if (length and (1 shl bit) != 0) {
                appendCodePoint(backgroundCharOffset + bit)
                append(minusOne)
            }
        }
        append(minusLength)
    }
    
    /**
     * Creates the level translation that is used to replace the vanilla enchantment name with the custom translation and level.
     */
    private fun createLevelOverrideTranslation(overlayBox: String, enchTranslation: String, level: Int): String = buildString {
        append(overlayBox)
        append(enchTranslation)
        append(" ")
        append(NumberFormatUtils.getRomanNumeral(level + 1))
    }
    
}