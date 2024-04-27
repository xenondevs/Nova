package xyz.xenondevs.nova.tileentity.network.task

import xyz.xenondevs.nova.world.format.NetworkState

internal class CustomWriteTask(
    state: NetworkState,
    private val write: suspend (NetworkState) -> Unit
) : NetworkTask(state) {
    
    override suspend fun run(): Boolean {
        write(state)
        return true
    }

}

internal class CustomReadTask(
    state: NetworkState,
    private val read: suspend (NetworkState) -> Unit
) : NetworkTask(state) {
    
    override suspend fun run(): Boolean {
        read(state)
        return false
    }

}

internal class CustomUncertainTask(
    state: NetworkState,
    private val task: suspend (NetworkState) -> Boolean
) : NetworkTask(state) {
    
    override suspend fun run(): Boolean {
        return task(state)
    }
    
}