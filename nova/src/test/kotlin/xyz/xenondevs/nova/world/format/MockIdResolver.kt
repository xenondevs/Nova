package xyz.xenondevs.nova.world.format

import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap

class MockIdResolver : IdResolver<String> {
    
    val data: List<Pair<String, Int>> = listOf(
        "a" to 1,
        "b" to -1,
        "c" to Int.MAX_VALUE,
        "d" to Int.MIN_VALUE,
        "e" to 2,
        "f" to 3,
        "g" to 128,
        "h" to 256,
        "i" to 512,
        "j" to 1024,
        "k" to 2048,
        "l" to 4096,
        "m" to 8192,
        "n" to 11,
        "o" to 12,
        "p" to 13,
        "q" to 14,
        "r" to 15,
        "s" to 16,
        "t" to 17,
        "u" to 18,
        "v" to 19,
        "w" to 20,
        "x" to 21,
        "y" to 22,
        "z" to 23,
    )
    
    private val biMap: BiMap<String, Int> = HashBiMap.create(data.toMap())
    
    override val size: Int
        get() = biMap.size
    
    override fun fromId(id: Int): String? {
        return biMap.inverse()[id]
    }
    
    override fun toId(value: String?): Int {
        return biMap[value] ?: 0
    }
    
}