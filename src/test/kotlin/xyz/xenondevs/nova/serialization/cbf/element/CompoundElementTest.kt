package xyz.xenondevs.nova.serialization.cbf.element

import io.netty.buffer.Unpooled
import org.junit.jupiter.api.Test
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.primitive.*
import xyz.xenondevs.nova.util.data.toByteArray
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class CompoundElementTest {
    
    @Test
    fun write() {
        val compound = CompoundElement()
        compound.putElement("test", IntElement(123))
        compound.put("test", StringElement("test"))
        compound.put("test", DoubleElement(12.3))
        val buf = Unpooled.buffer()
        compound.write(buf)
        assertContentEquals(buf.toByteArray(), byteArrayOf(7, 0, 4, 116, 101, 115, 116, 64, 40, -103, -103, -103, -103, -103, -102, 0))
    }
    
    @Test
    fun putElement() {
        val compound = CompoundElement()
        val element = IntElement(123)
        compound.putElement("test", element)
        assertTrue("test" in compound)
        assertEquals(element, compound.getElement("test"))
    }
    
    @Test
    fun remove() {
        val compound = CompoundElement()
        compound.putElement("test", IntElement(123))
        compound.remove("test")
        assertTrue("test" !in compound)
    }
    
    @Test
    fun contains() {
        val compound = CompoundElement()
        compound.putElement("test", IntElement(123))
        assertTrue("test" in compound)
        assertTrue("test1" !in compound)
    }
    
    @Test
    fun getElement() {
        val compound = CompoundElement()
        val element = DoubleElement(12.3)
        compound.putElement("test", element)
        assertTrue("test" in compound)
        assertEquals(element, compound.getElement("test"))
    }
    
    @Test
    fun testToString() {
        val compound = CompoundElement()
        compound.putElement("test", IntElement(1))
        compound.putElement("test2", StringElement("test"))
        compound.putElement("test3", DoubleElement(5.5))
        compound.putElement("test4", IntArrayElement(intArrayOf(1, 2, 3, 4)))
        val nested = CompoundElement()
        nested.putElement("test", FloatElement(5f))
        nested.putElement("test2", ByteArrayElement(byteArrayOf(1, 2, 3)))
        compound.putElement("nestedtest", nested)
        
        assertEquals("""{
"test4": [1, 2, 3, 4]
"nestedtest": {
 "test2": [1, 2, 3]
 "test": 5.0
 }
"test2": test
"test3": 5.5
"test": 1
}""", compound.toString())
    }
    
    @Test
    fun isEmpty() {
        assertTrue(CompoundElement().isEmpty())
    }
    
    @Test
    fun isNotEmpty() {
        val compound = CompoundElement()
        compound.putElement("test", IntElement(1))
        assertTrue(compound.isNotEmpty())
    }
}