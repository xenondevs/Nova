package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import java.io.File
import java.nio.file.Path

internal abstract class FileMerger(protected val basePacks: BasePacks, val path: Path) {
    
    constructor(basePacks: BasePacks, path: String) : this(basePacks, Path.of(path))
    
    abstract fun merge(source: File, destination: File)
    
    companion object {
        
        private val MERGER_TYPES = listOf(::ModelFileMerger, ::BlockStateFileMerger, ::FontFileMerger, ::LangFileMerger, ::AtlasFileMerger)
        
        fun createMergers(basePacks: BasePacks): List<FileMerger> = MERGER_TYPES.map { it(basePacks) }
        
    }
    
}