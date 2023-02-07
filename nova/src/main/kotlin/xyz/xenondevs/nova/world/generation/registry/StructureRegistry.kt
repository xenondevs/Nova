package xyz.xenondevs.nova.world.generation.registry

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.levelgen.structure.Structure
import net.minecraft.world.level.levelgen.structure.StructureSet
import net.minecraft.world.level.levelgen.structure.StructureType
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.util.NMSUtils

/**
 * TODO: fix configured structure deserialization
 */
object StructureRegistry : WorldGenRegistry(NMSUtils.REGISTRY_ACCESS) {
    
    override val neededRegistries get() = setOf(
        Registries.STRUCTURE, Registries.STRUCTURE_POOL_ELEMENT, Registries.STRUCTURE_PIECE,
        Registries.STRUCTURE_PLACEMENT, Registries.STRUCTURE_PROCESSOR, Registries.STRUCTURE_SET,
        Registries.STRUCTURE_TYPE
    )
    
    private val structures = Object2ObjectOpenHashMap<NamespacedId, Structure>()
    private val structurePoolElementTypes = Object2ObjectOpenHashMap<NamespacedId, StructurePoolElementType<*>>()
    private val structurePieceTypes = Object2ObjectOpenHashMap<NamespacedId, StructurePieceType>()
    private val structurePlacementTypes = Object2ObjectOpenHashMap<NamespacedId, StructurePlacementType<*>>()
    private val structureProcessorTypes = Object2ObjectOpenHashMap<NamespacedId, StructureProcessorType<*>>()
    private val structureSets = Object2ObjectOpenHashMap<NamespacedId, StructureSet>()
    private val structureTypes = Object2ObjectOpenHashMap<NamespacedId, StructureType<*>>()
    
    fun registerStructure(addon: Addon, name: String, structure: Structure) {
        val id = NamespacedId(addon, name)
        require(id !in structures) { "Duplicate structure $id" }
        structures[id] = structure
    }
    
    fun <P : StructurePoolElement> registerStructurePoolElementType(addon: Addon, name: String, structurePoolElementType: StructurePoolElementType<P>) {
        val id = NamespacedId(addon, name)
        require(id !in structurePoolElementTypes) { "Duplicate structure pool element $id" }
        structurePoolElementTypes[id] = structurePoolElementType
    }
    
    fun registerStructurePieceType(addon: Addon, name: String, structurePieceType: StructurePieceType) {
        val id = NamespacedId(addon, name)
        require(id !in structurePieceTypes) { "Duplicate structure piece $id" }
        structurePieceTypes[id] = structurePieceType
    }
    
    fun <SP : StructurePlacement> registerStructurePlacementType(addon: Addon, name: String, structurePlacementType: StructurePlacementType<SP>) {
        val id = NamespacedId(addon, name)
        require(id !in structurePlacementTypes) { "Duplicate structure placement type $id" }
        structurePlacementTypes[id] = structurePlacementType
    }
    
    fun <P : StructureProcessor> registerStructureProcessorType(addon: Addon, name: String, structureProcessorType: StructureProcessorType<P>) {
        val id = NamespacedId(addon, name)
        require(id !in structureProcessorTypes) { "Duplicate structure processor $id" }
        structureProcessorTypes[id] = structureProcessorType
    }
    
    fun registerStructureSet(addon: Addon, name: String, structureSet: StructureSet) {
        val id = NamespacedId(addon, name)
        require(id !in structureSets) { "Duplicate structure set $id" }
        structureSets[id] = structureSet
    }
    
    fun <S : Structure> registerStructureType(addon: Addon, name: String, structureType: StructureType<S>) {
        val id = NamespacedId(addon, name)
        require(id !in structureTypes) { "Duplicate structure type $id" }
        structureTypes[id] = structureType
    }
    
    override fun register() {
        loadFiles("structure", Structure.CODEC, structures)
        registerAll(Registries.STRUCTURE, structures)
        registerAll(Registries.STRUCTURE_POOL_ELEMENT, structurePoolElementTypes)
        registerAll(Registries.STRUCTURE_PIECE, structurePieceTypes)
        registerAll(Registries.STRUCTURE_PLACEMENT, structurePlacementTypes)
        registerAll(Registries.STRUCTURE_PROCESSOR, structureProcessorTypes)
        loadFiles("structure_set", StructureSet.CODEC, structureSets)
        registerAll(Registries.STRUCTURE_SET, structureSets)
        registerAll(Registries.STRUCTURE_TYPE, structureTypes)
    }
}