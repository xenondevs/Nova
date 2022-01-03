package xyz.xenondevs.nova.tileentity

import xyz.xenondevs.particle.task.SuppliedTask
import xyz.xenondevs.particle.task.TaskManager

class TileEntityParticleTask(tileEntity: TileEntity, particles: List<Any>, tickDelay: Int) {
    
    private val task = SuppliedTask(particles, tickDelay) { tileEntity.getViewers() }
    private var taskId = -1
    
    fun start() {
        if (taskId == -1)
            taskId = TaskManager.getTaskManager().startTask(task)
    }
    
    fun stop() {
        TaskManager.getTaskManager().stopTask(taskId)
        taskId = -1
    }
    
    fun isRunning() = taskId != -1
    
}