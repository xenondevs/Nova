package xyz.xenondevs.nova.world.block.state.property

import net.kyori.adventure.key.Key
import org.bukkit.block.BlockFace
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import xyz.xenondevs.nova.world.block.state.property.impl.EnumProperty
import xyz.xenondevs.nova.world.block.state.property.impl.IntProperty
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

private class PropertiesTreeLeaf(
    val path: IntArray, 
    val scopedValues: Map<ScopedBlockStateProperty<*>, Any>
) {
    val values: Map<BlockStateProperty<*>, Any> = scopedValues.mapKeys { it.key.property }
}

class PropertiesTreeTest {
    
    private companion object {
        
        val FACING = EnumProperty<BlockFace>(Key.key("nova", "facing"))
        val SCOPED_FACING = FACING.scope(BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST)
        
        val NUMBER = IntProperty(Key.key("nova", "number"))
        val SCOPED_NUMBER = NUMBER.scope(0..15)
        
    }
    
    @Test
    fun testGenerateTree() {
        assertDoesNotThrow { PropertiesTree(listOf(SCOPED_FACING, SCOPED_NUMBER)) { path, map -> PropertiesTreeLeaf(path, map) } }
    }
    
    @Test
    fun testGenerateSinglePropertyTree() {
        assertDoesNotThrow { PropertiesTree(listOf(SCOPED_FACING)) { path, map -> PropertiesTreeLeaf(path, map) } }
    }
    
    @Test
    fun testGetFromPath() {
        val tree = PropertiesTree(listOf(SCOPED_FACING, SCOPED_NUMBER)) { path, map -> PropertiesTreeLeaf(path, map) }
        
        assertEquals(
            mapOf<BlockStateProperty<*>, Any>(FACING to BlockFace.NORTH, NUMBER to 0),
            tree.get(intArrayOf(0, 0)).values
        )
        
        assertEquals(
            mapOf<BlockStateProperty<*>, Any>(FACING to BlockFace.WEST, NUMBER to 15),
            tree.get(intArrayOf(3, 15)).values
        )
    }
    
    @Test
    fun testGetFromMap() {
        val tree = PropertiesTree(listOf(SCOPED_FACING, SCOPED_NUMBER)) { path, map -> PropertiesTreeLeaf(path, map) }
        
        val north0 = mapOf<BlockStateProperty<*>, Any>(FACING to BlockFace.NORTH, NUMBER to 0)
        val west15 = mapOf<BlockStateProperty<*>, Any>(FACING to BlockFace.WEST, NUMBER to 15)
        
        assertEquals(north0, tree.get(north0).values)
        assertContentEquals(intArrayOf(0, 0), tree.get(north0).path)
        assertEquals(west15, tree.get(west15).values)
        assertContentEquals(intArrayOf(3, 15), tree.get(west15).path)
    }
    
    @Test
    fun testGetFromBase() {
        val tree = PropertiesTree(listOf(SCOPED_FACING, SCOPED_NUMBER)) { path, map -> PropertiesTreeLeaf(path, map) }
        assertContentEquals(tree.get(intArrayOf(0, 1), 1, 0).path, intArrayOf(0, 0))
    }
    
    @Test
    fun testFind() {
        val tree = PropertiesTree(listOf(SCOPED_FACING, SCOPED_NUMBER)) { path, map -> PropertiesTreeLeaf(path, map) }
        
        assertEquals(IndexedValue(0, SCOPED_FACING), tree.find(FACING))
        assertEquals(IndexedValue(1, SCOPED_NUMBER), tree.find(NUMBER))
    }
    
}