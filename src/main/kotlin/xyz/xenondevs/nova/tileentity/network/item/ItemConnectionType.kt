package xyz.xenondevs.nova.tileentity.network.item

enum class ItemConnectionType(val insert: Boolean, val extract: Boolean, included: ArrayList<ItemConnectionType>) {
    
    NONE(false, false, arrayListOf()),
    INSERT(true, false, arrayListOf(NONE)),
    EXTRACT(false, true, arrayListOf(NONE)),
    BUFFER(true, true, arrayListOf(NONE, INSERT, EXTRACT));
    
    val included: List<ItemConnectionType> = included.also { it.add(this) }
    
    companion object {
        
        fun of(insert: Boolean, extract: Boolean) = values().first { it.insert == insert && it.extract == extract }
        
    }
    
}