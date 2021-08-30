package xyz.xenondevs.nova.tileentity.network.item

enum class ItemConnectionType(val insert: Boolean, val extract: Boolean) {
    
    NONE(false, false),
    INSERT(true, false),
    EXTRACT(false, true),
    BUFFER(true, true);
    
    companion object {
        
        val ALL_TYPES = listOf(NONE, INSERT, EXTRACT, BUFFER)
        val INSERT_TYPES = listOf(NONE, INSERT)
        val EXTRACT_TYPES = listOf(NONE, EXTRACT)
        
        fun of(insert: Boolean, extract: Boolean) = values().first { it.insert == insert && it.extract == extract }
        
    }
    
}