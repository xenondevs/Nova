@file:OptIn(InternalResourcePackDTO::class)

package xyz.xenondevs.nova.resources.builder.task

import org.bukkit.block.BlockType
import org.joml.Matrix4f
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.nova.registry.ProtoBlockState
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.BlockStateDefinition
import xyz.xenondevs.nova.resources.builder.data.InternalResourcePackDTO
import xyz.xenondevs.nova.resources.builder.data.ItemModel
import xyz.xenondevs.nova.resources.builder.data.ItemModelDefinition
import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.resources.builder.layout.block.BlockSelectorScope
import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.nmsBlock
import xyz.xenondevs.nova.util.toPropertyStringMap
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelData
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider

/**
 * Hides selected vanilla block state models and displays them using entities.
 */
class VanillaBlockModelOverrideTask(private val builder: ResourcePackBuilder) : PackTask {
    
    override val stage = BuildStage.POST_WORLD
    override val runsAfter = setOf(BlockModelTask::class)
    override val runsBefore = setOf(BlockStateContent.Write::class, ModelContent.Write::class, ItemModelContent.Write::class)
    
    private val blockStateContent by builder.getBuildDataLazily<BlockStateContent>()
    private val modelContent by builder.getBuildDataLazily<ModelContent>()
    private val itemModelContent by builder.getBuildDataLazily<ItemModelContent>()
    
    override suspend fun run() {
        ResourceLookups.vanillaBlockModel = buildMap {
            for ([blockType, states] in findStatesToHide()) {
                val definition = getNormalizedDefinition(blockType)
                    ?: continue
                val selectedModels = states.associateWith { state ->
                    definition.getModels(state.properties).mapNotNull { models ->
                        if (models.size > 1)
                            builder.logger.warn("Using the first of ${models.size} models for $state")
                        models.firstOrNull()
                    }
                }
                putAll(createProviders(selectedModels))
                replaceModels(blockType, definition, states)
            }
        }
    }
    
    /**
     * Finds all block states that were requested to be hidden.
     */
    private fun findStatesToHide() = buildMap<BlockType, MutableSet<ProtoBlockState>> {
        val backingBlocks: Set<BlockType> = BackingStateCategory.entries.asSequence()
            .flatMap { it.backingStateConfigTypes.asSequence() }
            .mapTo(HashSet()) { it.of(0).blockType.get() }
        for ([layout, _, states] in BlockModelTask.requests.values) {
            layout as? BlockModelLayout.EntityBacked
                ?: continue
            if (!layout.eraseSelectedVanillaModels)
                continue
            for (protoState in states) {
                val blockData = layout.stateSelector(BlockSelectorScope(protoState))
                val blockType = blockData.blockType
                if (blockType.key.namespace() != "minecraft") {
                    builder.logger.warn("Cannot hide the model of non-vanilla backing state $blockData")
                    continue
                }
                if (blockType in backingBlocks) {
                    builder.logger.warn("Cannot hide the model of $blockData because its block is reserved for state-backed Nova blocks")
                    continue
                }
                getOrPut(blockType, ::HashSet) += ProtoBlockState.from(blockData)
            }
        }
    }
    
    /**
     * Expands partial variants of [blockType] to exact states and retains multipart cases.
     */
    private fun getNormalizedDefinition(blockType: BlockType): BlockStateDefinition? {
        val definition = blockStateContent[ResourcePath.of(ResourceType.BlockStateDefinition, blockType.key)]
        if (definition == null) {
            builder.logger.warn("Cannot hide models of ${blockType.key.asString()}: missing block state definition")
            return null
        }
        
        val variants = LinkedHashMap<BlockStateDefinition.Variant, List<BlockStateDefinition.Model>>()
        for (state in blockType.nmsBlock.stateDefinition.possibleStates) {
            val properties = state.toPropertyStringMap()
            val matching = definition.variants.filter { [variant, _] ->
                variant.properties.all { [name, value] -> properties[name] == value }
            }
            if (matching.size > 1) {
                builder.logger.warn("Cannot hide models of ${blockType.key.asString()}: overlapping variants for $state")
                return null
            }
            matching.values.singleOrNull()?.let { variants[BlockStateDefinition.Variant(properties)] = it }
        }
        return definition.copy(variants = variants)
    }
    
    /**
     * Creates matching [DisplayEntityBlockModelProvider]s for [selectedModels].
     */
    private fun createProviders(
        selectedModels: Map<ProtoBlockState, List<BlockStateDefinition.Model>>
    ): Map<ProtoBlockState, DisplayEntityBlockModelProvider> = selectedModels.mapValues { [state, variants] ->
        val modelBuilder = variants.map { variant ->
            val model = modelContent[variant.model]
                ?: error("Missing block model ${variant.model.asString()}")
            ModelBuilder(model).apply {
                if (variant.x != 0)
                    rotateX(variant.x.toDouble(), variant.uvLock)
                if (variant.y != 0)
                    rotateY(-variant.y.toDouble(), variant.uvLock)
            }
        }.reduceOrNull { builder, other -> builder.add(other) }
        val models = if (modelBuilder != null) {
            val displayBuilder = if (variants.size > 1 || variants.first().uvLock)
                ModelBuilder(modelBuilder.build(modelContent))
            else modelBuilder
            displayBuilder.buildDisplayEntity(modelContent).map { [model, transform] ->
                val modelId = modelContent.getOrPutGenerated(model)
                modelContent.rememberUsage(modelId)
                val itemId = itemModelContent.getOrPutGenerated(ItemModelDefinition(ItemModel.Default(modelId)))
                DisplayEntityBlockModelData.Model(itemId, Matrix4f(transform))
            }
        } else []
        DisplayEntityBlockModelProvider(DisplayEntityBlockModelData(
            waterlogged = state.properties["waterlogged"] == "true",
            models = models,
            colliderProvider = provider(state.toBlockData()),
            extraColliders = [],
            extraHitboxes = []
        ))
    }
    
    /**
     * Writes empty models for [states] while preserving the other models in [definition].
     */
    private fun replaceModels(
        blockType: BlockType,
        definition: BlockStateDefinition,
        states: Set<ProtoBlockState>
    ) {
        val variants = definition.variants.toMutableMap()
        for ((properties) in states) {
            // TODO: Make particles invisible & move server-side
            val parent = definition.getModels(properties).firstOrNull()?.firstOrNull()?.model
            val hiddenModel = BlockStateDefinition.Model(modelContent.getOrPutGenerated(Model(parent = parent, elements = [])))
            modelContent.rememberUsage(hiddenModel.model)
            variants[BlockStateDefinition.Variant(properties)] = [hiddenModel]
        }
        blockStateContent[ResourcePath.of(ResourceType.BlockStateDefinition, blockType.key)] =
            definition.copy(variants = variants)
    }
    
}
