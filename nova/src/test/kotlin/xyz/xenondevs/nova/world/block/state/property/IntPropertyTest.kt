package xyz.xenondevs.nova.world.block.state.property

import net.minecraft.resources.ResourceLocation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.xenondevs.nova.world.block.state.property.impl.IntProperty
import kotlin.test.assertEquals

class IntPropertyTest {
    
    @Test
    fun testValueToId() {
        val property = IntProperty(ResourceLocation.withDefaultNamespace("")).scope(10, 20, 30)
        
        assertEquals(0, property.valueToId(10))
        assertEquals(1, property.valueToId(20))
        assertEquals(2, property.valueToId(30))
        assertThrows<IllegalArgumentException> { property.valueToId(0) }
    }
    
    @Test
    fun testValueFromId() {
        val property = IntProperty(ResourceLocation.withDefaultNamespace("")).scope(10, 20, 30)
        
        assertEquals(10, property.idToValue(0))
        assertEquals(20, property.idToValue(1))
        assertEquals(30, property.idToValue(2))
        assertThrows<IllegalArgumentException> { property.idToValue(3) }
    }
    
    @Test
    fun testValueToString() {
        val property = IntProperty(ResourceLocation.withDefaultNamespace("")).scope(10, 20, 30)
        
        assertEquals("10", property.valueToString(10))
        assertEquals("20", property.valueToString(20))
        assertEquals("30", property.valueToString(30))
        assertThrows<IllegalArgumentException> { property.valueToString(0) }
    }
    
    @Test
    fun testStringToValue() {
        val property = IntProperty(ResourceLocation.withDefaultNamespace("")).scope(10, 20, 30)
        
        assertEquals(10, property.stringToValue("10"))
        assertEquals(20, property.stringToValue("20"))
        assertEquals(30, property.stringToValue("30"))
        assertThrows<IllegalArgumentException> { property.stringToValue("0") }
    }
    
}