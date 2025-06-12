package xyz.xenondevs.nova.serialization.cbf

import com.google.common.collect.HashBasedTable
import com.google.common.collect.Table
import com.google.common.collect.TreeBasedTable
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.commons.guava.set
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame

class TableSerializerTest {
    
    companion object {
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            Cbf.registerSerializerFactory(TableBinarySerializer)
        }
        
    }
    
    @Test
    fun testHashBasedTable() {
        val table = HashBasedTable.create<Int, Int, Int>()
        table[0, 0] = 0
        table[1, 0] = 1
        table[1, 1] = 11
        
        val reserialized: HashBasedTable<Int, Int, Int> = Cbf.read(Cbf.write(table))!!
        
        assertEquals(0, reserialized[0, 0])
        assertEquals(1, reserialized[1, 0])
        assertEquals(11, reserialized[1, 1])
    }
    
    @Test
    fun testTreeBasedTable() {
        val table = TreeBasedTable.create<Int, Int, Int>()
        table[0, 0] = 0
        table[1, 0] = 1
        table[1, 1] = 11
        
        val reserialized: TreeBasedTable<Int, Int, Int> = Cbf.read(Cbf.write(table))!!
        assertEquals(0, reserialized[0, 0])
        assertEquals(1, reserialized[1, 0])
        assertEquals(11, reserialized[1, 1])
    }
    
    @Test
    fun testDefaultIsHashBasedTable() {
        val table = HashBasedTable.create<Int, Int, Int>()
        val reserialized: Table<Int, Int, Int> = Cbf.read(Cbf.write(table))!!
        
        assertIs<HashBasedTable<Int, Int, Int>>(reserialized)
    }
    
    @Test
    fun testEmptyTable() {
        val table = HashBasedTable.create<Int, Int, Int>()
        val reserialized: HashBasedTable<Int, Int, Int> = Cbf.read(Cbf.write(table))!!
        assertEquals(0, reserialized.size())
    }
    
    @Test
    fun testCopyEquals() {
        val table = HashBasedTable.create<Int, Int, Int>()
        table[0, 0] = 0
        
        val reserialized: HashBasedTable<Int, Int, Int> = Cbf.read(Cbf.write(table))!!
        assertEquals(table, reserialized)
    }
    
    @Test
    fun testCopyNotSame() {
        val table = HashBasedTable.create<Int, Int, Int>()
        table[0, 0] = 0
        
        val reserialized: HashBasedTable<Int, Int, Int> = Cbf.read(Cbf.write(table))!!
        assertNotSame(table, reserialized)
    }
    
    @Test
    fun testCopyIsDeep() {
        val table = HashBasedTable.create<Int, Int, List<String>>()
        table[0, 0] = listOf("Hello", "World")
        
        val reserialized: HashBasedTable<Int, Int, List<String>> = Cbf.read(Cbf.write(table))!!
        assertEquals(table[0, 0], reserialized[0, 0])
        assertNotSame(table[0, 0], reserialized[0, 0])
    }
    
}