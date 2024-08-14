package xyz.xenondevs.nova.util.reflection

import io.papermc.paper.plugin.manager.PaperPluginManagerImpl
import net.minecraft.core.BlockPos
import net.minecraft.core.BlockPos.MutableBlockPos
import net.minecraft.core.Holder
import net.minecraft.core.MappedRegistry
import net.minecraft.core.NonNullList
import net.minecraft.core.Registry
import net.minecraft.util.RandomSource
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.crafting.RecipeInput
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelHeightAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.WorldGenLevel
import net.minecraft.world.level.biome.Biome
import net.minecraft.world.level.biome.Biome.ClimateSettings
import net.minecraft.world.level.biome.Biome.TemperatureModifier
import net.minecraft.world.level.biome.BiomeGenerationSettings
import net.minecraft.world.level.biome.BiomeSpecialEffects
import net.minecraft.world.level.biome.BiomeSpecialEffects.GrassColorModifier
import net.minecraft.world.level.biome.MobSpawnSettings
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.UpgradeData
import net.minecraft.world.level.levelgen.blending.BlendingData
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext
import net.minecraft.world.level.levelgen.feature.OreFeature
import net.minecraft.world.level.levelgen.feature.ReplaceBlockFeature
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration.TargetBlockState
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate
import org.bukkit.craftbukkit.block.data.CraftBlockData
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.plugin.Plugin
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getClass
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getConstructor
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getField
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethod
import java.security.ProtectionDomain
import java.util.*
import net.minecraft.world.entity.LivingEntity as MojangLivingEntity
import net.minecraft.world.item.ItemStack as MojangStack
import java.util.function.Function as JavaFunction

// TODO: Retire ReflectionRegistry, put the fields as top level constants in the files that use them instead
@Suppress("MemberVisibilityCanBePrivate")
internal object ReflectionRegistry {
    
    // Classes
    val HOLDER_SET_DIRECT_CLASS = getClass("net.minecraft.core.HolderSet\$Direct")
    val PAPER_EVENT_MANAGER_CLASS = getClass("io.papermc.paper.plugin.manager.PaperEventManager")
    
    // Constructors
    val ENUM_MAP_CONSTRUCTOR = getConstructor(EnumMap::class, false, Class::class)
    val ITEM_STACK_CONSTRUCTOR = getConstructor(MojangStack::class, false, ItemLike::class)
    val CHUNK_ACCESS_CONSTRUCTOR = getConstructor(ChunkAccess::class, false, ChunkPos::class, UpgradeData::class, LevelHeightAccessor::class, Registry::class, Long::class, Array<LevelChunkSection>::class, BlendingData::class)
    val BIOME_CLIMATE_SETTINGS_CONSTRUCTOR = getConstructor(ClimateSettings::class, true, Boolean::class, Float::class, TemperatureModifier::class, Float::class)
    val BIOME_SPECIAL_EFFECTS_CONSTRUCTOR = getConstructor(BiomeSpecialEffects::class, true, Int::class, Int::class, Int::class, Int::class, Optional::class, Optional::class, GrassColorModifier::class, Optional::class, Optional::class, Optional::class, Optional::class, Optional::class)
    val BIOME_GENERATION_SETTINGS_CONSTRUCTOR = getConstructor(BiomeGenerationSettings::class, true, Map::class, List::class)
    val MOB_SPAWN_SETTINGS_CONSTRUCTOR = getConstructor(MobSpawnSettings::class, true, Float::class, Map::class, Map::class)
    val BIOME_CONSTRUCTOR = getConstructor(Biome::class, true, ClimateSettings::class, BiomeSpecialEffects::class, BiomeGenerationSettings::class, MobSpawnSettings::class)
    
    // Methods
    val CLASS_LOADER_PARENT_FIELD by lazy { getField(ClassLoader::class.java, true, "parent") }
    val CLASS_LOADER_DEFINE_CLASS_METHOD by lazy { getMethod(ClassLoader::class, true, "defineClass", String::class, ByteArray::class, Int::class, Int::class, ProtectionDomain::class) }
    val LEVEL_CHUNK_SECTION_SET_BLOCK_STATE_METHOD = getMethod(LevelChunkSection::class, true, "setBlockState", Int::class, Int::class, Int::class, BlockState::class, Boolean::class)
    val CRAFT_BLOCK_DATA_IS_PREFERRED_TOOL_METHOD = getMethod(CraftBlockData::class, true, "isPreferredTool", BlockState::class, MojangStack::class)
    val LIVING_ENTITY_PLAY_BLOCK_FALL_SOUND_METHOD = getMethod(MojangLivingEntity::class, true, "playBlockFallSound")
    val ABSTRACT_FURNACE_BLOCK_ENTITY_GET_BURN_DURATION_METHOD = getMethod(AbstractFurnaceBlockEntity::class, true, "getBurnDuration", MojangStack::class)
    val RECIPE_GET_REMAINING_ITEMS_METHOD = getMethod(Recipe::class, false, "getRemainingItems", RecipeInput::class)
    val NON_NULL_LIST_SET_METHOD = getMethod(NonNullList::class, false, "set", Int::class, Any::class)
    val BREWING_STAND_BLOCK_ENTITY_DO_BREW_METHOD = getMethod(BrewingStandBlockEntity::class, true, "doBrew", Level::class, BlockPos::class, NonNullList::class, BrewingStandBlockEntity::class)
    val RULE_PROCESSOR_PROCESS_BLOCK_METHOD = getMethod(RuleProcessor::class, false, "processBlock", LevelReader::class, BlockPos::class, BlockPos::class, StructureTemplate.StructureBlockInfo::class, StructureTemplate.StructureBlockInfo::class, StructurePlaceSettings::class)
    val PROCESSOR_RULE_TEST_METHOD = getMethod(ProcessorRule::class, false, "test", BlockState::class, BlockState::class, BlockPos::class, BlockPos::class, BlockPos::class, RandomSource::class)
    val ORE_FEATURE_CAN_PLACE_ORE_METHOD = getMethod(OreFeature::class, true, "canPlaceOre", BlockState::class, JavaFunction::class, RandomSource::class, OreConfiguration::class, TargetBlockState::class, MutableBlockPos::class)
    val ORE_FEATURE_DO_PLACE_METHOD = getMethod(OreFeature::class, true, "doPlace", WorldGenLevel::class, RandomSource::class, OreConfiguration::class, Double::class, Double::class, Double::class, Double::class, Double::class, Double::class, Int::class, Int::class, Int::class, Int::class, Int::class)
    val REPLACE_BLOCK_PLACE_METHOD = getMethod(ReplaceBlockFeature::class, false, "place", FeaturePlaceContext::class)
    val RULE_TEST_TEST_METHOD = getMethod(RuleTest::class, false, "test", BlockState::class, RandomSource::class)
    val BLOCK_GETTER_GET_BLOCK_STATE_METHOD = getMethod(BlockGetter::class, false, "getBlockState", BlockPos::class)
    val FEATURE_PLACE_CONTEXT_RANDOM_METHOD = getMethod(FeaturePlaceContext::class, false, "random")
    val HOLDER_REFERENCE_BIND_VALUE_METHOD = getMethod(Holder.Reference::class, true, "bindValue", Any::class)
    val PAPER_EVENT_MANAGER_CREATE_REGISTERED_LISTENERS_METHOD = getMethod(PAPER_EVENT_MANAGER_CLASS, true, "createRegisteredListeners", Listener::class, Plugin::class)
    
    // Fields
    val PREPARE_ITEM_CRAFT_EVENT_MATRIX_FIELD = getField(PrepareItemCraftEvent::class, true, "matrix")
    val HANDLER_LIST_HANDLERS_FIELD = getField(HandlerList::class, true, "handlers")
    val HANDLER_LIST_HANDLER_SLOTS_FIELD = getField(HandlerList::class, true, "handlerslots")
    val BLOCK_DEFAULT_BLOCK_STATE_FIELD = getField(Block::class, true, "defaultBlockState")
    val MAPPED_REGISTRY_FROZEN_FIELD = getField(MappedRegistry::class, true, "frozen")
    val BIOME_GENERATION_SETTINGS_FEATURES_FIELD = getField(BiomeGenerationSettings::class, true, "features")
    val HOLDER_SET_DIRECT_CONTENTS_FIELD = getField(HOLDER_SET_DIRECT_CLASS, true, "contents")
    val HOLDER_SET_DIRECT_CONTENTS_SET_FIELD = getField(HOLDER_SET_DIRECT_CLASS, true, "contentsSet")
    val ABSTRACT_FURNACE_BLOCK_ENTITY_ITEMS_FIELD = getField(AbstractFurnaceBlockEntity::class, true, "items")
    val PROCESSOR_RULE_INPUT_PREDICATE_FIELD = getField(ProcessorRule::class, true, "inputPredicate")
    val PROCESSOR_RULE_LOC_PREDICATE_FIELD = getField(ProcessorRule::class, true, "locPredicate")
    val PROCESSOR_RULE_POS_PREDICATE_FIELD = getField(ProcessorRule::class, true, "posPredicate")
    val TARGET_BLOCK_STATE_TARGET_FIELD = getField(TargetBlockState::class, false, "target")
    val ITEM_STACK_ITEM_FIELD = getField(MojangStack::class, true, "item")
    val PAPER_PLUGIN_MANAGER_IMPL_PAPER_EVENT_MANAGER_FIELD = getField(PaperPluginManagerImpl::class, true, "paperEventManager")
    
}
