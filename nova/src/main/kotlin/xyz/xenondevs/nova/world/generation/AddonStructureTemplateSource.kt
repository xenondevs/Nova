package xyz.xenondevs.nova.world.generation

import com.mojang.datafixers.DataFixer
import net.minecraft.core.HolderGetter
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.TemplateSource
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.resources.ResourcePath
import java.nio.file.Files
import java.util.*
import java.util.stream.Stream
import kotlin.io.path.extension
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isRegularFile
import kotlin.io.path.relativeTo

/**
 * Loads addon templates from `worldgen/structure_template/<path>.nbt`.
 * Template ids use the addon's namespace and the relative path without the extension.
 */
internal class AddonStructureTemplateSource(
    fixerUpper: DataFixer,
    blockLookup: HolderGetter<Block>
) : TemplateSource(fixerUpper, blockLookup) {
    
    override fun load(id: Identifier): Optional<StructureTemplate> {
        val addon = AddonBootstrapper.addons.firstOrNull { it.namespace() == id.namespace }
            ?: return Optional.empty()
        val root = addon.dataFolder.resolve("worldgen/structure_template").normalize()
        val file = root.resolve("${id.path}.nbt").normalize()
        if (!file.startsWith(root) || !file.isRegularFile())
            return Optional.empty()
        
        return load({ Files.newInputStream(file) }, false) {
            LOGGER.error("Couldn't load addon structure template {}", id, it)
        }
    }
    
    override fun list(): Stream<Identifier> {
        return AddonBootstrapper.addons.stream().flatMap { addon ->
            val root = addon.dataFolder.resolve("worldgen/structure_template")
            if (!Files.isDirectory(root))
                return@flatMap Stream.empty()
            
            Files.walk(root).use { files ->
                files.filter { it.isRegularFile() && it.extension == "nbt" }
                    .map { it.relativeTo(root).invariantSeparatorsPathString.removeSuffix(".nbt") }
                    .filter { ResourcePath.isValidPath(it) }
                    .map { Identifier.fromNamespaceAndPath(addon.namespace(), it) }
                    .toList()
                    .stream()
            }
        }
    }
    
}
