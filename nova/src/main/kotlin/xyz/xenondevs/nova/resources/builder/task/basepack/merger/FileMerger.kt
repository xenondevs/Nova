package xyz.xenondevs.nova.resources.builder.task.basepack.merger

import xyz.xenondevs.nova.resources.builder.task.basepack.BasePacks
import java.nio.file.Path

internal abstract class FileMerger(protected val basePacks: BasePacks) {
    
    abstract fun acceptsFile(relPath: String): Boolean
    
    open fun merge(source: Path, destination: Path) = Unit
    
    companion object {
        
        private val MERGER_TYPES = listOf(
            ::ModelFileMerger, ::BlockStateFileMerger, ::FontFileMerger, ::LangFileMerger, ::AtlasFileMerger
        )
        
        fun createMergers(basePacks: BasePacks): List<FileMerger> = MERGER_TYPES.map { it(basePacks) }
        
    }
    
}

internal abstract class FileInDirectoryMerger(basePacks: BasePacks, val path: String) : FileMerger(basePacks) {
    
    override fun acceptsFile(relPath: String): Boolean {
        return relPath.startsWith(path)
    }
    
}