package xyz.xenondevs.nova.world.generation.inject.biome

import com.google.common.collect.ImmutableList
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.levelgen.placement.PlacedFeature
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.util.NMSUtils
import xyz.xenondevs.nova.util.data.decodeJsonFile
import xyz.xenondevs.nova.util.data.getFirstOrThrow
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.BIOME_GENERATION_SETTINGS_FEATURES_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import xyz.xenondevs.nova.world.generation.WorldGenManager
import java.io.File

object BiomeInjector {
    
    private val INJECTIONS_DIR = File(WorldGenManager.WORLD_GEN_DIR, "inject/biome")
    private val BIOME_REGISTRY = NMSUtils.getRegistry(Registry.BIOME_REGISTRY)
    
    private val toInject = Object2ObjectOpenHashMap<ResourceLocation, MutableList<MutableSet<Holder<PlacedFeature>>>>()
    private val patchedBiomes = IntOpenHashSet()
    
    fun parseInjections() {
        INJECTIONS_DIR.walkTopDown().filter(File::isFile).forEach { file ->
            // decode the biome injection file
            val biomeInjection = BiomeInjection.CODEC.decodeJsonFile(file).getFirstOrThrow()
            // get a list of all biomes in the specified tag
            val biomes = BIOME_REGISTRY.getTag(biomeInjection.biomesTag).get().map { BIOME_REGISTRY.getKey(it.value()) }
            // loop through all biomes and add new injections to the toInject map
            biomes.forEach { biome ->
                // retrieve or create the list of injections for this biome
                val featureList = toInject.getOrPut(biome, ::ObjectArrayList)
                // loop through each decoration category and add the features to the set
                biomeInjection.features.forEachIndexed { i, features ->
                    val featureSet = featureList.getOrNull(i) ?: ObjectOpenHashSet<Holder<PlacedFeature>>().also(featureList::add)
                    featureSet += features
                }
            }
        }
    }
    
    @Suppress("UNCHECKED_CAST", "unused")
    @JvmStatic
    fun injectFeatures(list: List<Any>) {
        // check if the specified list is a list of biomes
        if (list.isEmpty() || list[0] !is Holder<*> || (list[0] as Holder<Any>).value() !is Biome) return
        (list as List<Holder<Biome>>).asSequence().map { it.value() }.forEach { biome ->
            // check if this biome object has already been patched
            val hash = System.identityHashCode(biome)
            if (hash in patchedBiomes) return@forEach
            val settings = biome.generationSettings
            val biomeFeatures = settings.features()
            val key = BIOME_REGISTRY.getKey(biome)
            
            // check if this biome is even registered
            if (key == null) {
                LOGGER.warning("Failed to inject features into biome $settings")
                return@forEach
            }
            
            val injections = toInject[key] ?: return@forEach
            val preInjectSize = biomeFeatures.size
            
            // check if we need to add more HolderSets to the biome
            if (injections.size <= preInjectSize) {
                // loop through all HolderSets and replaced the backing list & set with our own
                for (i in injections.indices) {
                    val features = biomeFeatures[i]
                    val newFeatures = ArrayList(features.unwrap().right().get()).apply { addAll(injections[i]) }
                    ReflectionUtils.setFinalField(ReflectionRegistry.HOLDER_SET_DIRECT_CONTENTS_FIELD, features, newFeatures)
                    ReflectionUtils.setFinalField(ReflectionRegistry.HOLDER_SET_DIRECT_CONTENTS_SET_FIELD, features, newFeatures.toSet())
                }
            } else {
                // We have to create a new list
                val newList = ObjectArrayList<HolderSet<PlacedFeature>>()
                for (i in injections.indices) {
                    if (i < preInjectSize) {
                        newList.add(HolderSet.direct(biomeFeatures[i].unwrap().right().get().toMutableList().apply { addAll(injections[i]) }))
                    } else {
                        newList.add(HolderSet.direct(injections[i].toMutableList()))
                    }
                }
                ReflectionUtils.setFinalField(BIOME_GENERATION_SETTINGS_FEATURES_FIELD, settings, ImmutableList.copyOf(newList))
            }
            
            patchedBiomes += hash
            
            println("Patched new biome: $biome")
        }
    }
    
}