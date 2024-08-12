package xyz.xenondevs.nova.world.model

import org.bukkit.Location
import xyz.xenondevs.commons.collections.poll
import xyz.xenondevs.commons.collections.removeIf
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.nova.world.fakeentity.impl.FakeItemDisplay
import xyz.xenondevs.nova.world.fakeentity.metadata.impl.ItemDisplayMetadata

abstract class MultiModel {
    
    protected val models = HashMap<Model, FakeItemDisplay>()
    private var closed = false
    
    @Synchronized
    fun addAll(vararg models: Model) = addAll(models.asList())
    
    @Synchronized
    fun addAll(models: Iterable<Model>) {
        if (closed)
            return
        
        models.forEach(this::add)
    }
    
    @Synchronized
    fun add(model: Model) {
        val itemDisplay = model.createFakeItemDisplay()
        models.put(model, itemDisplay)?.remove()
    }
    
    @Synchronized
    fun clear() {
        models.values.forEach(FakeItemDisplay::remove)
        models.clear()
    }
    
    @Synchronized
    fun close() {
        clear()
        closed = true
    }
    
}

class MovableMultiModel : MultiModel() {
    
    val itemDisplays: Collection<FakeItemDisplay>
        get() = models.values
    
    @Synchronized
    fun use(run: (FakeItemDisplay) -> Unit) {
        models.values.forEach(run)
    }
    
    @Synchronized
    fun useMetadata(sendPacket: Boolean = true, run: (ItemDisplayMetadata) -> Unit) {
        models.values.forEach { it.updateEntityData(sendPacket, run) }
    }
    
    @Synchronized
    fun removeIf(predicate: (FakeItemDisplay) -> Boolean) {
        models.removeIf { (_, display) ->
            if (predicate(display)) {
                display.remove()
                true
            } else false
        }
    }
    
}

class FixedMultiModel : MultiModel() {
    
    @Synchronized
    fun replaceModels(newModels: Set<Model>) {
        val availableDisplays = HashMap<Location, HashSet<FakeItemDisplay>>()
        for ((model, itemDisplay) in models) {
            availableDisplays.getOrPut(model.location, ::HashSet) += itemDisplay
        }
        models.clear()
        
        // first, use existing display entities that are already at the correct position
        val remainingModels = newModels.toHashSet()
        remainingModels.removeIf { model ->
            val displaysAtPos = availableDisplays[model.location]
                ?.takeUnlessEmpty()
                ?: return@removeIf false
            
            val display = displaysAtPos.poll()!!
            display.updateEntityData(true) { model.applyMetadata(this) }
            models[model] = display
            
            return@removeIf true
        }
        
        // despawn all unused display entities
        for (displays in availableDisplays.values) {
            for (display in displays) {
                display.remove()
            }
        }
        
        // add the remaining models by spawning new display entities
        for (model in remainingModels) {
            add(model)
        }
    }
    
    @Synchronized
    fun removeIf(predicate: (Model, FakeItemDisplay) -> Boolean) {
        models.removeIf { (model, display) ->
            if (predicate(model, display)) {
                display.remove()
                true
            } else false
        }
    }
    
}