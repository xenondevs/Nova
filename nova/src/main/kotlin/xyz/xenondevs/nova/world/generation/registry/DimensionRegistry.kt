package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.Registry
import net.minecraft.core.RegistryAccess
import net.minecraft.world.level.dimension.DimensionType
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId

/**
 * TODO: inject into WorldPresets
 */
object DimensionRegistry : WorldGenRegistry(){
    
    override val neededRegistries = setOf(Registry.DIMENSION_TYPE_REGISTRY)
    
    private val dimensionTypes = Object2ObjectOpenHashMap<NamespacedId, DimensionType>()
    
    fun registerDimensionType(addon: Addon, name: String, dimensionType: DimensionType) {
        val id = NamespacedId(addon, name)
        require(id !in dimensionTypes) { "Duplicate dimension type $id" }
        dimensionTypes[id] = dimensionType
    }
    
    override fun register(registryAccess: RegistryAccess) {
        loadFiles("dimension_type", DimensionType.CODEC, dimensionTypes)
        registerAll(registryAccess, Registry.DIMENSION_TYPE_REGISTRY, dimensionTypes)
    }
}