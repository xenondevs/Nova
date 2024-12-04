package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WoodType {
    
    @SerialName("oak")
    OAK,
    
    @SerialName("spruce")
    SPRUCE,
    
    @SerialName("birch")
    BIRCH,
    
    @SerialName("acacia")
    ACACIA,
    
    @SerialName("cherry")
    CHERRY,
    
    @SerialName("jungle")
    JUNGLE,
    
    @SerialName("dark_oak")
    DARK_OAK,
    
    @SerialName("pale_oak")
    PALE_OAK,
    
    @SerialName("mangrove")
    MANGROVE,
    
    @SerialName("bamboo")
    BAMBOO,
    
    @SerialName("crimson")
    CRIMSON,
    
    @SerialName("warped")
    WARPED
    
}