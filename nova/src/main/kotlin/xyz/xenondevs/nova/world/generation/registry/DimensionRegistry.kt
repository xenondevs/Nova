package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.dimension.DimensionType
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.NMSUtils

/**
 * TODO: inject into WorldPresets
 */
object DimensionRegistry : WorldGenRegistry(NMSUtils.REGISTRY_ACCESS) {
    
    override val neededRegistries get() = setOf(Registries.DIMENSION_TYPE)
    
    private val dimensionTypes = Object2ObjectOpenHashMap<NamespacedId, DimensionType>()
    
    fun registerDimensionType(addon: Addon, name: String, dimensionType: DimensionType) {
        val id = NamespacedId(addon, name)
        require(id !in dimensionTypes) { "Duplicate dimension type $id" }
        dimensionTypes[id] = dimensionType
    }
    
    override fun register() {
        loadFiles("dimension_type", DimensionType.CODEC, dimensionTypes)
        registerAll(Registries.DIMENSION_TYPE, dimensionTypes)
    }
}