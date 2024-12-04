package xyz.xenondevs.nova.world.block.state.property

import net.kyori.adventure.key.Key
import org.bukkit.Axis
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import xyz.xenondevs.nova.world.block.state.property.impl.EnumProperty
import kotlin.test.assertEquals

class EnumPropertyTest {
    
    @Test
    fun testValueToId() {
        val property1 = EnumProperty<Axis>(Key.key("")).scope(Axis.X, Axis.Y, Axis.Z)
        
        assertEquals(0, property1.valueToId(Axis.X))
        assertEquals(1, property1.valueToId(Axis.Y))
        assertEquals(2, property1.valueToId(Axis.Z))
        
        val property2 = EnumProperty<Axis>(Key.key("")).scope(Axis.Z, Axis.X)
        
        assertEquals(0, property2.valueToId(Axis.X))
        assertEquals(1, property2.valueToId(Axis.Z))
        assertThrows<IllegalArgumentException> { property2.valueToId(Axis.Y)  }
    }
    
    @Test
    fun testValueFromId() {
        val property1 = EnumProperty<Axis>(Key.key("")).scope(Axis.X, Axis.Y, Axis.Z)
        
        assertEquals(Axis.X, property1.idToValue(0))
        assertEquals(Axis.Y, property1.idToValue(1))
        assertEquals(Axis.Z, property1.idToValue(2))
        
        val property2 = EnumProperty<Axis>(Key.key("")).scope(Axis.Z, Axis.X)
        
        assertEquals(Axis.X, property2.idToValue(0))
        assertEquals(Axis.Z, property2.idToValue(1))
        assertThrows<IllegalArgumentException> { property2.idToValue(2) }
    }
    
    @Test
    fun testValueToString() {
        val property1 = EnumProperty<Axis>(Key.key("")).scope(Axis.X, Axis.Y, Axis.Z)
        
        assertEquals("x", property1.valueToString(Axis.X))
        assertEquals("y", property1.valueToString(Axis.Y))
        assertEquals("z", property1.valueToString(Axis.Z))
        
        val property2 = EnumProperty<Axis>(Key.key("")).scope(Axis.Z, Axis.X)
        
        assertEquals("x", property2.valueToString(Axis.X))
        assertThrows<IllegalArgumentException> { property2.valueToString(Axis.Y) }
        assertEquals("z", property2.valueToString(Axis.Z))
    }
    
    @Test
    fun testStringToValue() {
        val property1 = EnumProperty<Axis>(Key.key("")).scope(Axis.X, Axis.Y, Axis.Z)
        
        assertEquals(Axis.X, property1.stringToValue("x"))
        assertEquals(Axis.Y, property1.stringToValue("y"))
        assertEquals(Axis.Z, property1.stringToValue("z"))
        
        val property2 = EnumProperty<Axis>(Key.key("")).scope(Axis.Z, Axis.X)
        
        assertEquals(Axis.X, property2.stringToValue("x"))
        assertThrows<IllegalArgumentException> { property2.stringToValue("y") }
        assertEquals(Axis.Z, property2.stringToValue("z"))
    }
    
}