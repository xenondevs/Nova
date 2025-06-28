@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.addon.registry.worldgen

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
import xyz.xenondevs.nova.addon.REGISTRIES_DEPRECATION
import xyz.xenondevs.nova.addon.registry.AddonGetter
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

@Deprecated(REGISTRIES_DEPRECATION)
interface StructureRegistry : AddonGetter {
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun registerStructure(name: String, structure: Structure): Structure =
        addon.registerStructure(name, structure)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun <P : StructurePoolElement> registerStructurePoolElementType(name: String, structurePoolElementType: StructurePoolElementType<P>): StructurePoolElementType<P> =
        addon.registerStructurePoolElementType(name, structurePoolElementType)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun registerStructurePieceType(name: String, structurePieceType: StructurePieceType): StructurePieceType =
        addon.registerStructurePieceType(name, structurePieceType)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun <SP : StructurePlacement> registerStructurePlacementType(name: String, structurePlacementType: StructurePlacementType<SP>): StructurePlacementType<SP> =
        addon.registerStructurePlacementType(name, structurePlacementType)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun <P : StructureProcessor> registerStructureProcessorType(name: String, structureProcessorType: StructureProcessorType<P>): StructureProcessorType<P> =
        addon.registerStructureProcessorType(name, structureProcessorType)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun registerStructureSet(name: String, structureSet: StructureSet): StructureSet =
        addon.registerStructureSet(name, structureSet)
    
    @Deprecated(REGISTRIES_DEPRECATION)
    @ExperimentalWorldGen
    fun <S : Structure> registerStructureType(name: String, structureType: StructureType<S>): StructureType<S> =
        addon.registerStructureType(name, structureType)
    
}