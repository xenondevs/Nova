package xyz.xenondevs.nova.world.format

import com.google.common.collect.HashBiMap
import com.google.gson.JsonObject
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.serialization.json.serializer.NovaBlockStateSerialization
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.world.block.behavior.UnknownNovaBlockState
import xyz.xenondevs.nova.world.block.state.NovaBlockState

private const val ID_MAP_KEY = "block_state_id_map"

@InternalInit(
    stage = InternalInitStage.PRE_WORLD,
    dependsOn = [ResourceGeneration.PreWorld::class]
)
internal object BlockStateIdResolver : IdResolver<NovaBlockState> {
    
    private lateinit var map: HashBiMap<NovaBlockState, Int>
    override val size: Int
        get() = map.size
    
    /**
     * Generates the block state to id mappings while preserving previous mappings.
     */
    @InitFun
    private fun generate() {
        val map = HashBiMap.create<NovaBlockState, Int>()
        val serializedMap = JsonObject()
        
        var lastId = 0
        
        // register previous states with same id or replace with unknown state if not present
        val previousStatesObj = PermanentStorage.retrieveRaw(ID_MAP_KEY) as JsonObject?
        if (previousStatesObj != null) {
            for ((previousIdStr, previousStateObj) in previousStatesObj.entrySet()) {
                previousStateObj as JsonObject
                val previousId = previousIdStr.toInt()
                val previousState = NovaBlockStateSerialization.deserialize(previousStateObj)
                
                serializedMap.add(previousIdStr, previousStateObj)
                map[previousState ?: UnknownNovaBlockState(previousStateObj)] = previousId
                
                if (previousId > lastId)
                    lastId = previousId
            }
        }
        
        // register new states
        NovaRegistries.BLOCK.asSequence()
            .flatMap { it.blockStates }
            .filter { it !in map }
            .forEach {
                val id = ++lastId
                map[it] = id
                serializedMap.add(id.toString(), NovaBlockStateSerialization.serialize(it))
            }
        
        this.map = map
        PermanentStorage.storeRaw(ID_MAP_KEY, serializedMap)
    }
    
    override fun fromId(id: Int): NovaBlockState? {
        return map.inverse()[id]
    }
    
    override fun toId(value: NovaBlockState?): Int {
        return if (value != null) map[value]!! else 0
    }
    
}