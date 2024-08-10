package xyz.xenondevs.nova.world.block.tileentity.network.task

import jdk.jfr.Category
import jdk.jfr.Event
import jdk.jfr.Label
import jdk.jfr.Name
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.format.NetworkState

internal class CustomWriteTask(
    state: NetworkState,
    override val chunkPos: ChunkPos,
    private val write: suspend (NetworkState) -> Unit
) : NetworkTask(state) {
    
    //<editor-fold desc="jfr event", defaultstate="collapsed">
    @Name("xyz.xenondevs.CustomWriteTask")
    @Label("Custom Write")
    @Category("Nova", "TileEntity Network")
    private inner class CustomWriteTaskEvent : Event()
    override val event: Event = CustomWriteTaskEvent()
    //</editor-fold>
    
    override suspend fun run(): Boolean {
        write(state)
        return true
    }

}

internal class CustomReadTask(
    state: NetworkState,
    override val chunkPos: ChunkPos,
    private val read: suspend (NetworkState) -> Unit
) : NetworkTask(state) {
    
    //<editor-fold desc="jfr event", defaultstate="collapsed">
    @Name("xyz.xenondevs.CustomRead")
    @Label("Custom Read")
    @Category("Nova", "TileEntity Network")
    private inner class CustomReadTaskEvent : Event()
    override val event: Event = CustomReadTaskEvent()
    //</editor-fold>
    
    override suspend fun run(): Boolean {
        read(state)
        return false
    }

}

internal class CustomUncertainTask(
    state: NetworkState,
    override val chunkPos: ChunkPos,
    private val task: suspend (NetworkState) -> Boolean
) : NetworkTask(state) {
    
    //<editor-fold desc="jfr event", defaultstate="collapsed">
    @Name("xyz.xenondevs.CustomUncertain")
    @Label("Custom Read or Write")
    @Category("Nova", "TileEntity Network")
    private inner class CustomUncertainTaskEvent : Event()
    override val event: Event = CustomUncertainTaskEvent()
    //</editor-fold>
    
    override suspend fun run(): Boolean {
        return task(state)
    }
    
}