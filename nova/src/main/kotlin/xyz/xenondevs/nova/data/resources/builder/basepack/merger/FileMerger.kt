package xyz.xenondevs.nova.data.resources.builder.basepack.merger

import xyz.xenondevs.nova.data.resources.builder.basepack.BasePacks
import java.nio.file.Path

internal abstract class FileMerger(protected val basePacks: BasePacks, val path: String) {
    
    abstract fun merge(source: Path, destination: Path)
    
    companion object {
        
        private val MERGER_TYPES = listOf(::ModelFileMerger, ::BlockStateFileMerger, ::FontFileMerger, ::LangFileMerger)
        
        fun createMergers(basePacks: BasePacks): List<FileMerger> = MERGER_TYPES.map { it(basePacks) }
        
    }
    
}