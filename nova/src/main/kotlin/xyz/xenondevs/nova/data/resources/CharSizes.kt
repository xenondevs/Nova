@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.data.resources

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.md_5.bungee.api.chat.BaseComponent
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.util.data.ArrayKey
import xyz.xenondevs.nova.util.data.toPlainText
import xyz.xenondevs.nova.util.removeMinecraftFormatting
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream

object CharSizes {
    
    private val CHAR_SIZES_DIR = File(NOVA.dataFolder, ".data/char_sizes/")
    
    private val loadedTables = HashMap<String, CharSizeTable>()
    
    private val lengthCache: Cache<Pair<ArrayKey<BaseComponent>, String>, Int> = CacheBuilder.newBuilder()
        .maximumSize(5000L)
        .build()
    
    fun getCharWidth(font: String, char: Int): Int =
        getTable(font).getWidth(char)
    
    fun getCharWidth(font: String, char: Char): Int =
        getCharWidth(font, char.code)
    
    fun getCharHeight(font: String, char: Int): Int =
        getTable(font).getHeight(char)
    
    fun getCharAscent(font: String, char: Int): Int =
        getTable(font).getAscent(char)
    
    fun getCharHeight(font: String, char: Char): Int =
        getCharHeight(font, char.code)
    
    fun calculateComponentLength(components: Array<out BaseComponent>, locale: String): Int {
        return lengthCache.get(ArrayKey(components) to locale) {
            var length = 0
            for (component in components) {
                val text = component.toPlainText(locale).removeMinecraftFormatting()
                val font = component.font ?: "default"
                length += text.toCharArray().sumOf {
                    var width = getCharWidth(font, it)
                    if (width < 0) width += 1
                    if (component.isBold) width += 1
                    
                    return@sumOf width
                }
            }
            
            return@get length
        }
    }
    
    fun calculateStringLength(font: String, string: String): Int {
        return string.toCharArray().sumOf { getCharWidth(font, it) }
    }
    
    private fun loadTable(font: String): CharSizeTable? {
        val file = getFile(font)
        if (file.exists()) {
            val table = CharSizeTable.load(file)
            loadedTables[font] = table
            return table
        }
        
        return null
    }
    
    internal fun storeTable(font: String, table: CharSizeTable) {
        loadedTables[font] = table
        table.write(getFile(font))
    }
    
    internal fun getTable(font: String): CharSizeTable =
        getTableOrNull(font) ?: throw IllegalArgumentException("Unknown font: $font")
    
    internal fun getTableOrNull(font: String): CharSizeTable? {
        val namespacedFont = if (font.contains(':')) font else "minecraft:$font"
        return loadedTables[namespacedFont] ?: loadTable(namespacedFont)
    }
    
    internal fun deleteTables() {
        loadedTables.clear()
        CHAR_SIZES_DIR.deleteRecursively()
    }
    
    private fun getFile(font: String): File {
        val path = ResourcePath.of(font, "minecraft")
        val file = File(CHAR_SIZES_DIR, "${path.namespace}/${path.path}.bin")
        file.parentFile.mkdirs()
        return file
    }
    
}

internal class CharSizeTable(
    private val sizes: MutableMap<Int, IntArray> = Int2ObjectOpenHashMap(),
) {
    
    fun getWidth(char: Int): Int {
        return sizes[char]?.get(0) ?: 0
    }
    
    fun getHeight(char: Int): Int {
        return sizes[char]?.get(1) ?: 0
    }
    
    fun getAscent(char: Int): Int {
        return sizes[char]?.get(2) ?: 0
    }
    
    fun setWidth(char: Int, width: Int) {
        sizes.getOrPut(char) { IntArray(3) }[0] = width
    }
    
    fun setHeight(char: Int, height: Int) {
        sizes.getOrPut(char) { IntArray(3) }[1] = height
    }
    
    fun setAscent(char: Int, ascent: Int) {
        sizes.getOrPut(char) { IntArray(3) }[2] = ascent
    }
    
    fun setSizes(char: Int, sizes: IntArray) {
        this.sizes[char] = sizes
    }
    
    operator fun contains(char: Int): Boolean {
        return char in sizes
    }
    
    fun write(file: File) {
        FileOutputStream(file).use { out ->
            val dout = DataOutputStream(out)
            
            sizes.forEach { (char, charSizes) ->
                dout.writeInt(char)
                dout.writeInt(charSizes[0]) // width
                dout.writeInt(charSizes[1]) // height
                dout.writeInt(charSizes[2]) // ascent
            }
        }
    }
    
    companion object {
        
        fun load(file: File): CharSizeTable {
            file.inputStream().use {
                val din = DataInputStream(it)
                val sizes = Int2ObjectOpenHashMap<IntArray>()
                
                while (din.available() >= 16) {
                    // char code, width, height, ascent
                    sizes[din.readInt()] = intArrayOf(din.readInt(), din.readInt(), din.readInt())
                }
                
                return CharSizeTable(sizes)
            }
        }
        
    }
    
}