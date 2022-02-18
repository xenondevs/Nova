package xyz.xenondevs.nova.addon

data class AddonDescription(
    val id: String,
    val name: String,
    val version: String,
    val main: String,
    val assetPackURL: String,
    val authors: List<String>?,
    val depend: List<String>?,
    val softdepend: List<String>?,
    val spigotResourceId: Int
)