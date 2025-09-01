@file:OptIn(InternalResourcePackDTO::class)

package xyz.xenondevs.nova.resources.builder.task

import org.joml.Matrix4f
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.nova.config.MAIN_CONFIG
import xyz.xenondevs.nova.config.entry
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.BlockStateDefinition
import xyz.xenondevs.nova.resources.builder.data.InternalResourcePackDTO
import xyz.xenondevs.nova.resources.builder.data.ItemModel
import xyz.xenondevs.nova.resources.builder.data.ItemModelDefinition
import xyz.xenondevs.nova.resources.builder.data.ParticleDefinition
import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelSelectorScope
import xyz.xenondevs.nova.resources.builder.layout.block.DEFAULT_BLOCK_STATE_SELECTOR
import xyz.xenondevs.nova.resources.builder.layout.block.ItemDefinitionConfigurator
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.resources.builder.task.basepack.BasePacks
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfigType
import xyz.xenondevs.nova.world.block.state.model.BlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelData
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.LeavesBackingStateConfigType
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider
import xyz.xenondevs.nova.world.block.state.property.DefaultBlockStateProperties.WATERLOGGED

private val DISABLED_BACKING_STATE_CATEGORIES: Set<BackingStateCategory> by MAIN_CONFIG.entry("resource_pack", "generation", "disabled_backing_state_categories")

/**
 * Deals with generating and assigning custom block models to block states
 * (i.e. creating block state variant entries) or items.
 */
class BlockModelTask(private val builder: ResourcePackBuilder) : PackTask {
    
    override val stage = BuildStage.PRE_WORLD
    override val runsAfter = setOf(BasePacks.Include::class, ExtractTask::class, ModelContent.DiscoverAllModels::class, BlockStateContent.PreLoadAll::class)
    override val runsBefore = setOf(ModelContent.Write::class, ItemModelContent.Write::class, BlockStateContent.Write::class)
    
    private val variantByConfig = HashMap<BackingStateConfig, BlockStateDefinition.Model>()
    private val configsByVariant = HashMap<BlockStateDefinition.Model, ArrayList<BackingStateConfig>>()
    
    private val blockStatePosition = HashMap<BackingStateConfigType<*>, Int>()
    
    private val basePacks by builder.getBuildDataLazily<BasePacks>()
    private val modelContent by builder.getBuildDataLazily<ModelContent>()
    private val itemModelContent by builder.getBuildDataLazily<ItemModelContent>()
    private val blockStateContent by builder.getBuildDataLazily<BlockStateContent>()
    
    override suspend fun run() {
        loadExistingBlockStateVariants()
        assignBlockModels()
        writeBlockStateFiles()
    }
    
    /**
     * Tries to load existing block state variants into [variantByConfig] and [configsByVariant]
     * in order to not override them. Ignores variants that do not seem to be intended for custom blocks,
     * e.g. variants that cover multiple block states.
     */
    private fun loadExistingBlockStateVariants() {
        BackingStateCategory.entries.asSequence()
            .filter { it !in DISABLED_BACKING_STATE_CATEGORIES }
            .flatMap { it.backingStateConfigTypes }
            .forEach { type ->
                val bsdId = ResourcePath.of(ResourceType.BlockStateDefinition, type.fileName)
                val bsd = blockStateContent.getCustom(bsdId)
                    ?: return@forEach
                
                if (bsd.multipart.isNotEmpty()) {
                    builder.logger.warn("Block state file $bsdId contains multipart block states, which are not supported. " +
                            "Block states defined in this file will be ignored and potentially overwritten.")
                    return@forEach
                }
                
                for ((variant, models) in bsd.variants) {
                    if (variant.properties.keys != type.properties) {
                        builder.logger.warn("Variant '$variant' in block state file $bsdId does not specify all properties explicitly " +
                            "(got ${variant.properties.keys}, expected ${type.properties}). " +
                            "This variant will be ignored and potentially overwritten.")
                        continue
                    }
                    
                    if (models.size != 1) {
                        builder.logger.warn("Variant '$variant' in block state file $bsdId has ${models.size} models, " +
                            "but only one is supported. This variant will be ignored and potentially overwritten.")
                        continue
                    }
                    
                    val config = type.of(variant.properties)
                    variantByConfig[config] = models[0]
                    configsByVariant.getOrPut(models[0], ::ArrayList) += config
                }
            }
    }
    
    /**
     * Assigns models to all custom block states.
     */
    private fun assignBlockModels() {
        val lookup = HashMap<NovaBlockState, BlockModelProvider>()
        
        // state-backed blocks, sort by:
        // 1. state backed priority (descending)
        // 2. amount of available states (ascending)
        // 3. id
        assignBlockModels<BlockModelLayout.StateBacked>(
            compareByDescending<Pair<NovaBlock, BlockModelLayout.StateBacked>> { (_, layout) -> layout.priority }
                .thenBy { (_, layout) -> layout.configTypes.sumOf { it.maxId - it.blockedIds.size } }
                .thenBy { (block, _) -> block.id }
        ) { blockState, layout, scope ->
            val modelBuilder = layout.modelSelector(scope)
            val cfg = assignModelToVanillaBlockState(layout, modelBuilder, blockState[WATERLOGGED] == true)
            if (cfg != null) {
                lookup[blockState] = BackingStateBlockModelProvider(cfg)
            } else {
                builder.logger.warn("No more block states for $blockState with layout $layout, falling back to display entity")
                val data = DisplayEntityBlockModelData(blockState, assignModelToItem(modelBuilder), DEFAULT_BLOCK_STATE_SELECTOR(scope))
                lookup[blockState] = DisplayEntityBlockModelProvider(data)
            }
        }
        
        // entity-backed blocks
        assignBlockModels<BlockModelLayout.EntityBacked> { blockState, layout, scope ->
            val scope = BlockModelSelectorScope(blockState, builder, modelContent)
            val models = when (layout) {
                is BlockModelLayout.SimpleEntityBacked -> assignModelToItem(layout.modelSelector(scope))
                is BlockModelLayout.ItemEntityBacked -> listOf(assignModelToItem(scope, layout.definitionConfigurator))
            }
            
            val data = DisplayEntityBlockModelData(blockState, models, layout.stateSelector(scope))
            lookup[blockState] = DisplayEntityBlockModelProvider(data)
        }
        
        // model-less blocks
        assignBlockModels<BlockModelLayout.ModelLess> { blockState, layout, scope ->
            lookup[blockState] = ModelLessBlockModelProvider(layout.stateSelector(scope))
        }
        
        ResourceLookups.BLOCK_MODEL = lookup
    }
    
    private inline fun <reified L : BlockModelLayout> assignBlockModels(
        comparator: Comparator<Pair<NovaBlock, L>> = compareBy { (block, _) -> block.id },
        assigner: (blockState: NovaBlockState, layout: L, scope: BlockModelSelectorScope) -> Unit
    ) {
        NovaRegistries.BLOCK
            .mapNotNull { if (it.layout is L) it to it.layout else null }
            .sortedWith(comparator)
            .forEach { (block, layout) ->
                for (blockState in block.blockStates) {
                    try {
                        val scope = BlockModelSelectorScope(blockState, builder, modelContent)
                        assigner(blockState, layout, scope)
                    } catch (e: Exception) {
                        throw Exception("Failed to assign model to $blockState", e)
                    }
                }
            }
    }
    
    /**
     * Tries to assign the block model generated by [modelBuilder] to a vanilla block state using the given [layout],
     * potentially re-using existing block state variants.
     * Then returns the [BackingStateConfig] the model was assigned to, or null if it wasn't assigned to any.
     */
    private fun assignModelToVanillaBlockState(layout: BlockModelLayout.StateBacked, modelBuilder: ModelBuilder, waterlogged: Boolean): BackingStateConfig? {
        val (model, rotations) = modelBuilder.buildBlockStateVariant(modelContent)
        val variant = BlockStateDefinition.Model(modelContent.getOrPutGenerated(model), rotations.x(), rotations.y())
        
        var cfg: BackingStateConfig? = null
        for (configType in layout.configTypes) {
            if (DISABLED_BACKING_STATE_CATEGORIES.any { configType in it.backingStateConfigTypes })
                continue
            
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
        
        val occupiedSet = basePacks.occupiedSolidIds[type]
        val blockedSet = type.blockedIds
        
        while (pos in blockedSet || (occupiedSet != null && pos in occupiedSet))
            pos++
        
        blockStatePosition[type] = pos
        
        return if (pos <= type.maxId) type.of(pos, waterlogged) else null
    }
    
    /**
     * Generates the item model definition using [scope] and [configureDefinition] and returns
     * the display entity configuration used to display it.
     */
    private fun assignModelToItem(scope: BlockModelSelectorScope, configureDefinition: ItemDefinitionConfigurator): DisplayEntityBlockModelData.Model {
        val itemDefinition = ItemModelDefinitionBuilder(builder) { modelSelector ->
            val builder = modelSelector(scope)
            val id = modelContent.getOrPutGenerated(builder.build(modelContent))
            modelContent.rememberUsage(id)
            id
        }.apply(configureDefinition).build()
        
        val itemId = itemModelContent.getOrPutGenerated(itemDefinition)
        return DisplayEntityBlockModelData.Model(itemId, Matrix4f())
    }
    
    /**
     * Assigns the block models generated by [modelBuilder] to generated item definitions and returns
     * all display entity configurations required to display the given [modelBuilder] using an item display entity.
     */
    private fun assignModelToItem(modelBuilder: ModelBuilder): List<DisplayEntityBlockModelData.Model> {
        return modelBuilder.buildDisplayEntity(modelContent).map { (model, transform) ->
            val modelId = modelContent.getOrPutGenerated(model)
            modelContent.rememberUsage(modelId)
            
            val itemDef = ItemModelDefinition(ItemModel.Default(modelId))
            val itemId = itemModelContent.getOrPutGenerated(itemDef)
            
            DisplayEntityBlockModelData.Model(itemId, Matrix4f(transform))
        }
    }
    
    /**
     * Writes all block state files to the resource pack.
     */
    private fun writeBlockStateFiles() {
        variantByConfig.entries
            // group by file id, because some backing state config types share the same file
            .groupBy { (cfg, _) -> ResourcePath.of(ResourceType.BlockStateDefinition, cfg.type.fileName) }
            .forEach { (bsdId, cfgModelEntries) ->
                blockStateContent[bsdId] = BlockStateDefinition(
                    variants = cfgModelEntries.associate { (cfg, model) ->
                        BlockStateDefinition.Variant(cfg.variantMap) to listOf(model) 
                    }
                )
            }
        
        // disable leaf particles if leaves backing state configs are used
        val particles = variantByConfig.keys.mapNotNullTo(HashSet()) { (it.type as? LeavesBackingStateConfigType<*>)?.particleType }
        for (particle in particles) {
            builder.logger.info("Disabling $particle particles because their leaves are used as backing states")
            builder.writeJson(
                ResourcePath.of(ResourceType.ParticleDefinition, particle),
                ParticleDefinition(listOf(ResourcePath(ResourceType.ParticleTexture, "nova", "empty")))
            )
        }
    }
    
}