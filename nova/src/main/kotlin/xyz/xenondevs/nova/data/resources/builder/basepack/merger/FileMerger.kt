package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import java.nio.file.Path

internal abstract class FileMerger(protected val basePacks: BasePacks) {
    
    abstract fun acceptsFile(relPath: Path): Boolean
    
    open fun merge(source: Path, destination: Path, baseDir: Path, relPath: Path) = merge(source, destination)
    open fun merge(source: Path, destination: Path) = Unit
    
    companion object {
        
        private val MERGER_TYPES = listOf(
            ::ModelFileMerger, ::BlockStateFileMerger, ::FontFileMerger, ::LangFileMerger, ::AtlasFileMerger,
            ::FancyPantsArmorFileMerger
        )
        
        fun createMergers(basePacks: BasePacks): List<FileMerger> = MERGER_TYPES.map { it(basePacks) }
        
    }
    
}

internal abstract class FileInDirectoryMerger(basePacks: BasePacks, val path: String) : FileMerger(basePacks) {
    
    override fun acceptsFile(relPath: Path): Boolean {
        return relPath.startsWith(path)
    }
    
}