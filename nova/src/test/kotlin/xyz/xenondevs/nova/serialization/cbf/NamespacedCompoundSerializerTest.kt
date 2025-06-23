package xyz.xenondevs.nova.serialization.cbf

import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.Cbf
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class NamespacedCompoundSerializerTest {
    
    companion object { 
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            Cbf.registerSerializer(NamespacedCompound.NamespacedCompoundBinarySerializer)
        }
        
    }
    
    @Test
    fun testReserialize() {
        val compound = NamespacedCompound()
        compound["nova", "int"] = 1
        compound["nova", "string"] = "abc"
        compound["my_addon", "string_list"] = listOf("a", "b", "c")
        
        val reserialized: NamespacedCompound = Cbf.read(Cbf.write(compound))!!
        
        assertEquals(1, reserialized["nova", "int"])
        assertEquals("abc", reserialized["nova", "string"])
        assertEquals(listOf("a", "b", "c"), reserialized["my_addon", "string_list"])
    }
    
    @Test
    fun testCopyNotSame() {
        val compound = NamespacedCompound()
        compound["namespace", "key"] = "value"
        
        val copy = Cbf.copy(compound)
        assertNotSame(compound, copy)
    }
    
    @Test
    fun testCopyIsDeep() {
        val compound = NamespacedCompound()
        compound["namespace", "key"] = listOf("value")
        
        val copy = Cbf.copy(compound)!!
        assertNotSame<List<String>?>(compound["namespace", "key"], copy["namespace", "key"])
    }
    
}