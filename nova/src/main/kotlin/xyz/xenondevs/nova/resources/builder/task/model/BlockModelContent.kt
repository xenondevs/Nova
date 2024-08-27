package xyz.xenondevs.nova.resources.builder.task.model

import com.google.gson.JsonObject
import org.joml.Matrix4f
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.commons.gson.getObjectOrNull
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.commons.gson.writeToFile
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.resources.builder.task.PackTask
import xyz.xenondevs.nova.resources.builder.task.PackTaskHolder
import xyz.xenondevs.nova.resources.layout.block.BackingStateCategory
import xyz.xenondevs.nova.resources.layout.block.BlockModelLayout
import xyz.xenondevs.nova.resources.layout.block.BlockModelLayout.LayoutType
import xyz.xenondevs.nova.resources.layout.block.BlockModelSelectorScope
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.serialization.json.GSON
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelData
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.LinkedBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.WATERLOGGED
import java.nio.file.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists

/**
 * A [PackTaskHolder] that deals with generating and assigning custom block models to block states
 * (i.e. creating block state variant entries) or items.
 */
class BlockModelContent internal constructor(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val variantByConfig = HashMap<BackingStateConfig, BlockStateVariantData>()
    private val configsByVariant = HashMap<BlockStateVariantData, ArrayList<BackingStateConfig>>()
    
    private val blockStatePosition = HashMap<BackingStateConfigType<*>, Int>()
    
    private val modelContent by builder.getHolderLazily<ModelContent>()
    private val itemModelContent by builder.getHolderLazily<ItemModelContent>()
    
    /**
     * Reads all block state files that may be used by backing-state block models.
     */
    @PackTask(runAfter = ["ExtractTask#extractAll"])
    private fun readBlockStateFiles() {
        BackingStateCategory.entries.asSequence()
            .flatMap { it.backingStateConfigTypes }
            .forEach { type ->
                val file = getBlockStateFile(type)
                if (file.exists()) {
                    val blockStateJson = file.parseJson() as JsonObject
                    
                    if (blockStateJson.has("multipart"))
                        throw IllegalArgumentException("Block state file $file contains multipart block states, which are not supported")
                    
                    blockStateJson.getObjectOrNull("variants")?.entrySet()
                        ?.forEach { (variantStr, variantOpts) ->
                            val properties = variantStr.split(',')
                                .associate { val s = it.split('='); s[0] to s[1] }
                            
                            val config = type.of(properties)
                            val variant = GSON.fromJson<BlockStateVariantData>(variantOpts)!!
                            
                            variantByConfig[config] = variant
                            configsByVariant.getOrPut(variant, ::ArrayList) += config
                        }
                    
                }
            }
    }
    
    /**
     * Assigns models to all custom block states.
     */
    @PackTask(
        runAfter = [
            "BlockModelContent#readBlockStateFiles",
            "ModelContent#discoverAllModels",
            "ItemModelContent#loadOverrides"
        ],
        runBefore = ["BlockModelContent#writeBlockStateFiles"]
    )
    private fun assignBlockModels() {
        val lookup = HashMap<NovaBlockState, LinkedBlockModelProvider<*>>()
        
        NovaRegistries.BLOCK
            .sortedByDescending { it.requestedLayout.priority }
            .forEach { block ->
                val layout = block.requestedLayout
                for (blockState in block.blockStates) {
                    val scope = BlockModelSelectorScope(blockState, builder, modelContent)
                    when (layout.type) {
                        LayoutType.STATE_BACKED -> {
                            val modelBuilder = layout.modelSelector(scope)
                            val cfg = assignModelToVanillaBlockState(layout, modelBuilder, blockState[WATERLOGGED] == true)
                            if (cfg != null) {
                                lookup[blockState] = LinkedBlockModelProvider(BackingStateBlockModelProvider, cfg)
                            } else if (block is NovaTileEntityBlock) {
                                LOGGER.warning("No more block states for $blockState with layout, falling back to display entity.")
                                val data = DisplayEntityBlockModelData(assignModelToItem(modelBuilder), layout.stateSelector(scope))
                                lookup[blockState] = LinkedBlockModelProvider(DisplayEntityBlockModelProvider, data)
                            } else throw IllegalStateException("Ran out of backing states trying to assign a model to $blockState")
                        }
                        
                        LayoutType.ENTITY_BACKED -> {
                            if (block !is NovaTileEntityBlock)
                                throw IllegalArgumentException("$block cannot use entity-backed block models, as it is not a tile-entity")
                            
                            val modelBuilder = layout.modelSelector(BlockModelSelectorScope(blockState, builder, modelContent))
                            val data = DisplayEntityBlockModelData(assignModelToItem(modelBuilder), layout.stateSelector(scope))
                            lookup[blockState] = LinkedBlockModelProvider(DisplayEntityBlockModelProvider, data)
                        }
                        
                        LayoutType.MODEL_LESS -> {
                            lookup[blockState] = LinkedBlockModelProvider(ModelLessBlockModelProvider, layout.stateSelector(scope))
                        }
                    }
                }
            }
        
        ResourceLookups.BLOCK_MODEL = lookup
    }
    
    /**
     * Tries to assign the block model generated by [modelBuilder] to a vanilla block state using the given [layout],
     * potentially re-using existing block state variants.
     * Then returns the [BackingStateConfig] the model was assigned to, or null if it wasn't assigned to any.
     */
    private fun assignModelToVanillaBlockState(layout: BlockModelLayout, modelBuilder: ModelBuilder, waterlogged: Boolean): BackingStateConfig? {
        val (model, rotations) = modelBuilder.buildBlockStateVariant(modelContent)
        val variant = BlockStateVariantData(modelContent.getOrPutGenerated(model), rotations.x(), rotations.y())
        
        var cfg: BackingStateConfig? = null
        for (configType in layout.backingStateConfigTypes) {
            val match = configsByVariant[variant]?.firstOrNull { it.type == configType }
            if (match != null) {
                cfg = if (match.waterlogged != waterlogged) configType.of(match.id, waterlogged) else match
                break
            }
            
            val next = nextBackingStateConfig(configType, waterlogged)
            if (next != null) {
                cfg = next
                break
            }
        }
        
        if (cfg == null)
            return null
        
        modelContent.rememberUsage(variant.model)
        variantByConfig[cfg] = variant
        configsByVariant.getOrPut(variant, ::ArrayList) += cfg
        
        return cfg
    }
    
    /**
     * Gets the next available [BackingStateConfig] of the given [type] or null if no more are available.
     */
    private fun nextBackingStateConfig(type: BackingStateConfigType<*>, waterlogged: Boolean): BackingStateConfig? {
        var pos = blockStatePosition.getOrPut(type) { -1 } + 1
        
        val occupiedSet = builder.basePacks.occupiedSolidIds[type]
        val blockedSet = type.blockedIds
        
        while (pos in blockedSet || (occupiedSet != null && pos in occupiedSet))
            pos++
        
        blockStatePosition[type] = pos
        
        return if (pos < type.maxId) type.of(pos, waterlogged) else null
    }
    
    /**
     * Assigns the block models generated by [modelBuilder] to the default item via custom-model-data and returns
     * all models required to display the given [modelBuilder] using an item display entity.
     */
    private fun assignModelToItem(modelBuilder: ModelBuilder): List<DisplayEntityBlockModelData.Model> {
        return modelBuilder.buildDisplayEntity(modelContent).map { (model, transform) ->
            val modelId = modelContent.getOrPutGenerated(model)
            modelContent.rememberUsage(modelId)
            val (material, customModelData) = itemModelContent.getOrRegisterDefault(modelId)
            DisplayEntityBlockModelData.Model(material, customModelData, Matrix4f(transform))
        }
    }
    
    /**
     * Writes all block state files to the resource pack.
     */
    // TODO: support base pack merging
    @PackTask(runAfter = ["ModelContent#discoverAllModels"])
    private fun writeBlockStateFiles() {
        variantByConfig.entries
            .groupBy { (cfg, _) -> cfg.type }
            .forEach { (type, entries) ->
                val obj = JsonObject()
                val variantsObj = JsonObject().also { obj.add("variants", it) }
                for ((cfg, variantData) in entries) {
                    variantsObj.add(cfg.variantString, GSON.toJsonTree(variantData))
                }
                
                val blockStateFile = getBlockStateFile(type)
                blockStateFile.createParentDirectories()
                obj.writeToFile(blockStateFile)
                
            }
    }
    
    /**
     * Gets the block state file for the given [type].
     */
    private fun getBlockStateFile(type: BackingStateConfigType<*>): Path =
        ResourcePackBuilder.ASSETS_DIR.resolve("minecraft/blockstates/${type.fileName}.json")
    
}

internal data class BlockStateVariantData(
    val model: ResourcePath,
    val x: Int,
    val y: Int
)