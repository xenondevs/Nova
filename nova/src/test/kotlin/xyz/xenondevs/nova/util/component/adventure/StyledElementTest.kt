package xyz.xenondevs.nova.util.component.adventure

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.`object`.ObjectContents
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class StyledElementTest {
    
    @Test
    fun `empty elements`() {
        val component = Component.empty()
        assertEquals(emptyList(), component.elements().toList())
    }
    
    @Test
    fun `simple styled text`() {
        val style = Style.style(NamedTextColor.AQUA)
        val component = Component.text("ABC", style)
        assertContentEquals(
            listOf(
                StyledElement.CodePoint(style, 'A'.code),
                StyledElement.CodePoint(style, 'B'.code),
                StyledElement.CodePoint(style, 'C'.code)
            ),
            component.elements({ null }).toList()
        )
    }
    
    @Test
    fun `nested styled text`() {
        val outerStyle = Style.style(NamedTextColor.AQUA)
        val innerStyle = Style.style(TextDecoration.BOLD)
        val component = Component.text("A", outerStyle)
            .append(Component.text("BC", innerStyle))
            .append(Component.text("D"))
        
        assertContentEquals(
            listOf(
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'A'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true), 'B'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true), 'C'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'D'.code)
            ),
            component.elements({ null }).toList()
        )
    }
    
    @Test
    fun `nested styled text with objects`() {
        val sprite = ObjectContents.sprite(Key.key("minecraft", "sprite"))
        val head = ObjectContents.playerHead(UUID.randomUUID())
        
        val outerStyle = Style.style(NamedTextColor.AQUA)
        val innerStyle = Style.style(TextDecoration.BOLD)
        val component = Component.text("A", outerStyle)
            .append(Component.text("B", innerStyle))
            .append(Component.`object`(sprite))
            .append(Component.`object`(head))
        
        assertContentEquals(
            listOf(
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'A'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true), 'B'.code),
                StyledElement.Object(Style.style(NamedTextColor.AQUA), sprite),
                StyledElement.Object(Style.style(NamedTextColor.AQUA), head)
            ),
            component.elements({ null }).toList()
        )
    }
    
    @Test
    fun `styled translatable`() {
        val translations = mapOf(
            "test.key" to "A%sC"
        )
        
        val outerStyle = Style.style(NamedTextColor.AQUA)
        val innerStyle = Style.style(TextDecoration.BOLD)
        val component = Component.translatable("test.key", outerStyle, Component.text("B", innerStyle))
        assertContentEquals(
            listOf(
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'A'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true), 'B'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'C'.code)
            ),
            component.elements(translations::get).toList()
        )
    }
    
    @Test
    fun `styled translatable with escaped percent`() {
        val translations = mapOf(
            "test.key" to "A%%B"
        )
        
        val outerStyle = Style.style(NamedTextColor.AQUA)
        val component = Component.translatable("test.key", outerStyle)
        assertContentEquals(
            listOf(
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'A'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), '%'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'B'.code),
            ),
            component.elements(translations::get).toList()
        )
    }
    
    @Test
    fun `styled translatable with arg at beginning`() {
        val translations = mapOf(
            "test.key" to "%sBC"
        )
        
        val outerStyle = Style.style(NamedTextColor.AQUA)
        val innerStyle = Style.style(TextDecoration.BOLD)
        val component = Component.translatable("test.key", outerStyle, Component.text("A", innerStyle))
        assertContentEquals(
            listOf(
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true), 'A'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'B'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'C'.code)
            ),
            component.elements(translations::get).toList()
        )
    }
    
    @Test
    fun `styled translatable with arg at end`() {
        val translations = mapOf(
            "test.key" to "AB%s"
        )
        
        val outerStyle = Style.style(NamedTextColor.AQUA)
        val innerStyle = Style.style(TextDecoration.BOLD)
        val component = Component.translatable("test.key", outerStyle, Component.text("C", innerStyle))
        assertContentEquals(
            listOf(
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'A'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'B'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true), 'C'.code)
            ),
            component.elements(translations::get).toList()
        )
    }
    
    @Test
    fun `styled translatable with indexed and non-indexed args`() {
        val translations = mapOf(
            "test.key" to $$"%2$sA%sB"
        )
        
        val outerStyle = Style.style(NamedTextColor.AQUA)
        val boldStyle = Style.style(TextDecoration.BOLD)
        val italicStyle = Style.style(TextDecoration.ITALIC)
        val component = Component.translatable(
            "test.key",
            outerStyle,
            Component.text("C", boldStyle),
            Component.text("X", italicStyle)
        )
        assertContentEquals(
            listOf(
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, true), 'X'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'A'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true), 'C'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'B'.code)
            ),
            component.elements(translations::get).toList()
        )
    }
    
    @Test
    fun `styled translatable with missing translation but fallback`() {
        val outerStyle = Style.style(NamedTextColor.AQUA)
        val innerStyle = Style.style(TextDecoration.BOLD)
        
        val component = Component.translatable(
            "missing",
            outerStyle,
            Component.text("B", innerStyle)
        ).fallback("A%sC")
        
        assertContentEquals(
            listOf(
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'A'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true), 'B'.code),
                StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'C'.code)
            ),
            component.elements({ null }).toList()
        )
    }
    
    @Test
    fun `styled translatable with missing translation`() {
        val outerStyle = Style.style(NamedTextColor.AQUA)
        val innerStyle = Style.style(TextDecoration.BOLD)
        
        val component = Component.translatable(
            "A",
            outerStyle,
            Component.text("B", innerStyle)
        )
        
        assertContentEquals(
            listOf(StyledElement.CodePoint(Style.style(NamedTextColor.AQUA), 'A'.code)),
            component.elements({ null }).toList()
        )
    }
    
    @Test
    fun `translatable with less args than placeholders`() {
        val translations = mapOf(
            "test.key" to "A%sB%sC"
        )
        
        val component = Component.translatable("test.key", Component.text("X"))
        component.elements(translations::get)
        assertEquals("A%sB%sC", component.elementsAsString(translations::get))
    }
    
    @Test
    fun `translatable with trailing percent sign`() {
        val translations = mapOf(
            "test.key" to "A%"
        )
        
        val component = Component.translatable("test.key", Component.text("B"))
        assertEquals("A%", component.elementsAsString(translations::get))
    }
    
    @Test
    fun `translatable with non-string format specifier`() {
        val translations = mapOf(
            "test.key" to "A%s%dD"
        )
        
        val component = Component.translatable("test.key", Component.text("B"), Component.text("1"))
        assertEquals("A%s%dD", component.elementsAsString(translations::get))
    }
    
    @Test
    fun `translatable with out-of-bounds explicit index`() {
        val translations = mapOf(
            "test.key" to $$"A%3$sC"
        )
        
        val component = Component.translatable("test.key", Component.text("B"))
        assertEquals($$"A%3$sC", component.elementsAsString(translations::get))
    }
    
    @Test
    fun `test translatable with complex format string`() {
        val translations = mapOf(
            "test.key" to $$"%%A%2$s%%B%2$s%s%%"
        )
        
        val component = Component.translatable("test.key", Component.text("1"), Component.text("2"))
        assertEquals("%A2%B21%", component.elementsAsString(translations::get))
    }
    
    private fun Component.elementsAsString(translate: (String) -> String?): String =
        elements(translate).filterIsInstance<StyledElement.CodePoint>().joinToString("") { Character.toString(it.codePoint) }
    
}