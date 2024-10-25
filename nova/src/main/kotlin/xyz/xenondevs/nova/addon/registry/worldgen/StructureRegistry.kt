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
import xyz.xenondevs.nova.addon.registry.AddonHolder
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistries
import xyz.xenondevs.nova.util.ResourceLocation
import xyz.xenondevs.nova.util.set
import xyz.xenondevs.nova.world.generation.ExperimentalWorldGen

interface StructureRegistry : AddonHolder {
    
    @ExperimentalWorldGen
    fun registerStructure(name: String, structure: Structure): Structure {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.STRUCTURE[id] = structure
        return structure
    }
    
    @ExperimentalWorldGen
    fun <P : StructurePoolElement> registerStructurePoolElementType(name: String, structurePoolElementType: StructurePoolElementType<P>): StructurePoolElementType<P> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.STRUCTURE_POOL_ELEMENT[id] = structurePoolElementType
        return structurePoolElementType
    }
    
    @ExperimentalWorldGen
    fun registerStructurePieceType(name: String, structurePieceType: StructurePieceType): StructurePieceType {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.STRUCTURE_PIECE[id] = structurePieceType
        return structurePieceType
    }
    
    @ExperimentalWorldGen
    fun <SP : StructurePlacement> registerStructurePlacementType(name: String, structurePlacementType: StructurePlacementType<SP>): StructurePlacementType<SP> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.STRUCTURE_PLACEMENT[id] = structurePlacementType
        return structurePlacementType
    }
    
    @ExperimentalWorldGen
    fun <P : StructureProcessor> registerStructureProcessorType(name: String, structureProcessorType: StructureProcessorType<P>): StructureProcessorType<P> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.STRUCTURE_PROCESSOR[id] = structureProcessorType
        return structureProcessorType
    }
    
    @ExperimentalWorldGen
    fun registerStructureSet(name: String, structureSet: StructureSet): StructureSet {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.STRUCTURE_SET[id] = structureSet
        return structureSet
    }
    
    @ExperimentalWorldGen
    fun <S : Structure> registerStructureType(name: String, structureType: StructureType<S>): StructureType<S> {
        val id = ResourceLocation(addon, name)
        VanillaRegistries.STRUCTURE_TYPE[id] = structureType
        return structureType
    }
    
}