package xyz.xenondevs.nova.world.block.state.property

import net.kyori.adventure.key.Key
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.xenondevs.nova.world.block.state.property.impl.BooleanProperty
import kotlin.test.assertEquals

class BooleanPropertyTest {
    
    @Test
    fun testValueToId() {
        val property = BooleanProperty(Key.key("")).scope()
        
        assertEquals(0, property.valueToId(false))
        assertEquals(1, property.valueToId(true))
    }
    
    @Test
    fun testValueFromId() {
        val property = BooleanProperty(Key.key("")).scope()
        
        assertEquals(false, property.idToValue(0))
        assertEquals(true, property.idToValue(1))
        assertThrows<IllegalArgumentException> { property.idToValue(2) }
    }
    
    @Test
    fun testValueToString() {
        val property = BooleanProperty(Key.key("")).scope()
        
        assertEquals("false", property.valueToString(false))
        assertEquals("true", property.valueToString(true))
    }
    
    @Test
    fun testStringToValue() {
        val property = BooleanProperty(Key.key("")).scope()
        
        assertEquals(false, property.stringToValue("false"))
        assertEquals(true, property.stringToValue("true"))
        assertThrows<IllegalArgumentException> { property.stringToValue("0") }
    }
    
}