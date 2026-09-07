package xyz.xenondevs.nova.registry

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.tag.TagKey
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.minecraft.world.level.block.state.BlockBehaviour.Properties
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.Property
import org.bukkit.Color
import org.bukkit.block.BlockType
import org.bukkit.block.PistonMoveReaction
import org.bukkit.block.data.BlockData
import org.bukkit.inventory.ItemType
import xyz.xenondevs.commons.collections.flatMap
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.commons.provider.uninitializedProvider
import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.registry.tags.BlockTypeTags
import xyz.xenondevs.nova.resources.builder.layout.block.BackingStateCategory
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelLayout
import xyz.xenondevs.nova.resources.builder.layout.block.BlockModelSelectorScope
import xyz.xenondevs.nova.resources.builder.layout.block.BlockSelectorScope
import xyz.xenondevs.nova.resources.builder.layout.item.ItemModelDefinitionBuilder
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.resources.builder.task.BlockModelTask
import xyz.xenondevs.nova.serialization.kotlinx.BlockTypeEntrySerializer
import xyz.xenondevs.nova.util.nmsPushReaction
import xyz.xenondevs.nova.util.toNmsMapColor
import xyz.xenondevs.nova.util.toPropertyStringMap
import xyz.xenondevs.nova.util.toResourceKey
import xyz.xenondevs.nova.world.block.ColliderCube
import xyz.xenondevs.nova.world.block.FluidFlowMode
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorFactory
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorHolder
import xyz.xenondevs.nova.world.block.behavior.DefaultBlockBehavior
import xyz.xenondevs.nova.world.block.sound.SoundGroup
import xyz.xenondevs.nova.world.block.state.property.BlockStateProperty
import xyz.xenondevs.nova.world.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.world.item.tool.VanillaToolTiers

internal data class FlammableSettings(val igniteOdds: Int, val burnOdds: Int, val ignitedByLava: Boolean)

internal abstract class AbstractNovaBlockBuilder<T : NovaBlock>(
    override val entry: RegistryEntry.Paper<BlockType>
) : NovaBlockBuilder, RegistryElementBuilder.RerunnableVanilla<BlockType, T> {
    
    protected val key: Key = entry.key
    
    private val explicitTags = mutableProvider<Set<RegistryEntrySet.Paper.Tag<BlockType>>>(emptySet())
    protected val _configId = uninitializedProvider<String>()
    protected val _style = uninitializedProvider<Style>()
    protected val _name = uninitializedProvider<Component>()
    protected val _item = uninitializedProvider<RegistryEntry.Paper<ItemType>?>()
    protected val _behaviorHolders = uninitializedProvider<List<BlockBehaviorHolder>>()
    protected val _stateProperties = uninitializedProvider<List<BlockStateProperty<*>>>()
    protected val _layout = uninitializedProvider<BlockModelLayout>()
    protected val _hardness = uninitializedProvider<Double>()
    protected val _requiresToolForDrops = uninitializedProvider<Boolean>()
    protected val _breakParticles = uninitializedProvider<RegistryEntry.Paper<ItemType>?>()
    protected val _showBreakAnimation = uninitializedProvider<Boolean>()
    protected val _soundGroup = uninitializedProvider<SoundGroup?>()
    protected val _pistonReaction = uninitializedProvider<PistonMoveReaction>()
    protected val _selectLightEmission = uninitializedProvider<BlockSelectorScope.() -> Int>()
    protected val _explosionResistance = uninitializedProvider<Float>()
    protected val _flammable = uninitializedProvider<FlammableSettings>()
    protected val _mapColor = uninitializedProvider<Color?>()
    protected val _selectFluidFlowMode = uninitializedProvider<BlockSelectorScope.() -> FluidFlowMode>()
    
    protected val _behaviors = combinedProvider(
        _behaviorHolders, _configId
    ) { holders, configId ->
        val cfg = CONFIGS[configId]
        buildList {
            this += DefaultBlockBehavior()
            for (holder in holders) {
                this += when (holder) {
                    is BlockBehaviorFactory<*> -> holder.create(entry, cfg)
                    is BlockBehavior -> holder
                }
            }
        }
    }
    
    override val tags = combinedProvider(_behaviors, explicitTags) { behaviors, explicitTags ->
        combinedProvider(behaviors.map { behavior -> behavior.tags }) { tagSets ->
            tagSets.flatMapTo(HashSet()) { it } + explicitTags
        }
    }.flatten()
    
    protected val _effectiveStateProperties = combinedProvider(
        _stateProperties, _behaviors
    ) { properties, behaviors ->
        (properties + behaviors.flatMap(BlockBehavior::stateProperties))
            .distinctBy(BlockStateProperty<*>::name)
    }
    
    protected val _properties = combinedProvider(
        _hardness, _requiresToolForDrops, _pistonReaction, _selectLightEmission,
        _explosionResistance, _flammable, _mapColor
    ) { hardness, requiresToolForDrops, pistonReaction, selectLightEmission, explosionResistance, flammable, mapColor ->
        Properties.of()
            .setId(entry.key.toResourceKey())
            .pushReaction(pistonReaction.nmsPushReaction)
            .explosionResistance(explosionResistance)
            .mapColor(mapColor.toNmsMapColor())
            .lightLevel { state ->
                val proto = ProtoBlockState(
                    entry,
                    state.toPropertyStringMap()
                )
                BlockSelectorScope(proto).selectLightEmission()
            }
            .destroyTime(hardness.toFloat())
            .apply {
                if (requiresToolForDrops)
                    requiresCorrectToolForDrops()
                if (flammable.ignitedByLava)
                    ignitedByLava()
            }
    }
    
    protected var configId: String by _configId
    protected var style: Style by _style
    protected var name: Component by _name
    protected var item: RegistryEntry.Paper<ItemType>? by _item
    protected var behaviors: List<BlockBehaviorHolder> by _behaviorHolders
    protected var stateProperties: List<BlockStateProperty<*>> by _stateProperties
    protected var layout: BlockModelLayout by _layout
    protected var hardness: Double by _hardness
    protected var requiresToolForDrops: Boolean by _requiresToolForDrops
    protected var breakParticles: RegistryEntry.Paper<ItemType>? by _breakParticles
    protected var showBreakAnimation: Boolean by _showBreakAnimation
    protected var soundGroup: SoundGroup? by _soundGroup
    protected var pistonReaction: PistonMoveReaction by _pistonReaction
    protected var selectLightEmission: BlockSelectorScope.() -> Int by _selectLightEmission
    protected var explosionResistance: Float by _explosionResistance
    protected var flammable: FlammableSettings by _flammable
    protected var mapColor: Color? by _mapColor
    protected var selectFluidFlowMode: BlockSelectorScope.() -> FluidFlowMode by _selectFluidFlowMode
    protected val effectiveStateProperties by _effectiveStateProperties
    
    override fun reset() {
        explicitTags.set(emptySet())
        configId = key.asString()
        style = (Style.empty())
        name = Component.translatable("block.${key.namespace()}.${key.value()}")
        item = null
        behaviors = emptyList()
        stateProperties = emptyList()
        layout = BlockModelLayout.DEFAULT
        hardness = -1.0
        requiresToolForDrops = false
        breakParticles = null
        showBreakAnimation = true
        soundGroup = null
        pistonReaction = PistonMoveReaction.MOVE
        selectLightEmission = { 0 }
        explosionResistance = 0f
        flammable = FlammableSettings(0, 0, false)
        mapColor = null
        selectFluidFlowMode = { FluidFlowMode.BLOCK }
    }
    
    override fun tags(vararg tags: RegistryEntrySet.Paper.Tag<BlockType>) {
        this.explicitTags.set(this.explicitTags.get() + tags)
    }
    
    override fun config(name: String) {
        this.configId = key.namespace() + ":" + name
    }
    
    override fun rawConfig(id: String) {
        this.configId = id
    }
    
    override fun style(style: Style) {
        this.style = style
    }
    
    override fun name(name: Component) {
        this.name = name
    }
    
    override fun item(item: RegistryEntry.Paper<ItemType>) {
        this.item = item
    }
    
    override fun behaviors(vararg behaviors: BlockBehaviorHolder) {
        this.behaviors += behaviors
    }
    
    override fun breakable(
        hardness: Double,
        toolCategories: Set<Key>,
        toolTier: Key?,
        requiresToolForDrops: Boolean,
        breakParticles: RegistryEntry.Paper<ItemType>?,
        showBreakAnimation: Boolean
    ) {
        this.hardness = hardness
        this.requiresToolForDrops = requiresToolForDrops
        this.breakParticles = breakParticles
        this.showBreakAnimation = showBreakAnimation
        
        toolCategories.forEach { category ->
            val tag = when (category) {
                VanillaToolCategories.SWORD -> BlockTypeTags.SWORD_EFFICIENT
                VanillaToolCategories.SHEARS -> BlockTypeTags.SHEARS_MAJOR_BREAKING_SPEED
                else -> registryEntrySetOf(TagKey.create(RegistryKey.BLOCK, Key.key(category.namespace(), "mineable/${category.value()}")))
            }
            tags(tag)
        }
        toolTier?.let { tier ->
            val tagTier = when (tier) {
                VanillaToolTiers.WOOD, VanillaToolTiers.GOLD -> null
                else -> tier.value()
            }
            if (tagTier != null) {
                tags(registryEntrySetOf(TagKey.create(RegistryKey.BLOCK, Key.key(tier.namespace(), "needs_${tagTier}_tool"))))
            }
        }
    }
    
    override fun sounds(soundGroup: SoundGroup) {
        this.soundGroup = soundGroup
    }
    
    override fun stateProperties(vararg stateProperties: BlockStateProperty<*>) {
        this.stateProperties += stateProperties
    }
    
    override fun stateBacked(
        priority: Int,
        category: BackingStateCategory,
        vararg categories: BackingStateCategory,
        modelSelector: BlockModelSelectorScope.() -> ModelBuilder
    ) {
        layout = BlockModelLayout.StateBacked(
            priority,
            listOf(category, *categories).flatMap { it.backingStateConfigTypes },
            modelSelector
        )
    }
    
    override fun entityBacked(
        stateSelector: BlockSelectorScope.() -> BlockData,
        extraColliderSelector: BlockSelectorScope.() -> List<ColliderCube>,
        modelSelector: BlockModelSelectorScope.() -> ModelBuilder
    ) {
        layout = BlockModelLayout.SimpleEntityBacked(stateSelector, extraColliderSelector, modelSelector)
    }
    
    override fun entityItemBacked(
        stateSelector: BlockSelectorScope.() -> BlockData,
        extraColliderSelector: BlockSelectorScope.() -> List<ColliderCube>,
        itemSelector: ItemModelDefinitionBuilder<BlockModelSelectorScope>.() -> Unit
    ) {
        layout = BlockModelLayout.ItemEntityBacked(stateSelector, extraColliderSelector, itemSelector)
    }
    
    override fun modelLess(stateSelector: BlockSelectorScope.() -> BlockData) {
        layout = BlockModelLayout.ModelLess(stateSelector)
    }
    
    override fun pistonReaction(pistonReaction: PistonMoveReaction) {
        this.pistonReaction = pistonReaction
    }
    
    override fun lightEmission(selectLightEmission: BlockSelectorScope.() -> Int) {
        this.selectLightEmission = selectLightEmission
    }
    
    override fun explosionResistance(explosionResistance: Float) {
        this.explosionResistance = explosionResistance
    }
    
    override fun flammable(igniteOdds: Int, burnOdds: Int, ignitedByLava: Boolean) {
        flammable = FlammableSettings(igniteOdds, burnOdds, ignitedByLava)
    }
    
    override fun mapColor(mapColor: Color) {
        this.mapColor = mapColor
    }
    
    override fun fluidFlowMode(selectFluidFlowMode: BlockSelectorScope.() -> FluidFlowMode) {
        this.selectFluidFlowMode = selectFluidFlowMode
    }
    
    override fun prepareBuild() {
        BlockModelTask.request(entry, layout, ProtoBlockState.createBlockStates(entry, effectiveStateProperties))
    }
    
}

@Serializable
internal class ProtoBlockState(
    @Serializable(with = BlockTypeEntrySerializer::class)
    val entry: RegistryEntry.Paper<BlockType>,
    val properties: Map<String, String>
) {
    
    operator fun <T : Comparable<T>> get(property: BlockStateProperty<T>): T? =
        properties[property.nmsProperty.name]?.let(property::stringToValue)
    
    @Suppress("UNCHECKED_CAST")
    fun <T : Comparable<T>> getOrThrow(property: BlockStateProperty<T>): T =
        get(property) ?: throw NoSuchElementException("Property $property not present")
    
    fun toBlockState(defaultBlockState: BlockState): BlockState {
        var blockState = defaultBlockState
        
        @Suppress("UNCHECKED_CAST")
        fun <T : Comparable<T>> BlockState.setValue(property: Property<T>, value: String): BlockState =
            setValue(property, property.getValue(value).orElseThrow())
        
        for ([propertyName, value] in properties) {
            val property = blockState.properties.first { it.name == propertyName }
            blockState = blockState.setValue(property, value)
        }
        
        return blockState
    }
    
    companion object {
        
        fun createBlockStates(
            entry: RegistryEntry.Paper<BlockType>,
            properties: List<BlockStateProperty<*>>
        ): List<ProtoBlockState> {
            if (properties.isEmpty())
                return listOf(ProtoBlockState(entry, emptyMap()))
            
            val states = ArrayList<ProtoBlockState>()
            val values = LinkedHashMap<String, String>()
            
            fun create(index: Int) {
                if (index == properties.size) {
                    states += ProtoBlockState(entry, LinkedHashMap(values))
                    return
                }
                
                val property = properties[index]
                fun <T : Comparable<T>> setValues(property: BlockStateProperty<T>) {
                    for (value in property.values) {
                        values[property.name] = property.valueToString(value)
                        create(index + 1)
                    }
                }
                setValues(property)
                values.remove(property.name)
            }
            
            create(0)
            return states
        }
        
    }
    
}
