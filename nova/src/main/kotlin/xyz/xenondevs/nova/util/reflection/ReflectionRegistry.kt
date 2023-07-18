package xyz.xenondevs.nova.util.reflection

import com.mojang.brigadier.tree.CommandNode
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.Lifecycle
import net.minecraft.core.BlockPos
import net.minecraft.core.BlockPos.MutableBlockPos
import net.minecraft.core.DefaultedMappedRegistry
import net.minecraft.core.Holder
import net.minecraft.core.HolderGetter
import net.minecraft.core.MappedRegistry
import net.minecraft.core.NonNullList
import net.minecraft.core.Registry
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.TagType
import net.minecraft.resources.RegistryFileCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.util.ThreadingDetector
import net.minecraft.world.Container
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.inventory.ItemCombinerMenu
import net.minecraft.world.item.crafting.Recipe
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
import net.minecraft.world.level.biome.FeatureSorter
import net.minecraft.world.level.biome.MobSpawnSettings
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DispenserBlock
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.HashMapPalette
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.LinearPalette
import net.minecraft.world.level.chunk.PalettedContainer
import net.minecraft.world.level.chunk.UpgradeData
import net.minecraft.world.level.levelgen.NoiseRouterData
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
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.craftbukkit.v1_20_R1.block.data.CraftBlockData
import org.bukkit.craftbukkit.v1_20_R1.inventory.CraftItemStack
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.plugin.SimplePluginManager
import xyz.xenondevs.nova.util.ServerSoftware
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getCB
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getCBClass
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getClass
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getConstructor
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getField
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethod
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getServerSoftwareField
import java.io.DataInput
import java.security.ProtectionDomain
import java.util.*
import java.util.concurrent.Semaphore
import java.util.function.Consumer
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.LivingEntity as MojangLivingEntity
import net.minecraft.world.entity.player.Inventory as MojangInventory
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.ItemStack as MojangStack
import org.bukkit.inventory.ItemStack as BukkitStack
import java.util.function.Function as JavaFunction

@Suppress("MemberVisibilityCanBePrivate")
internal object ReflectionRegistry {
    
    // Paths
    val CB_PACKAGE_PATH = getCB()
    
    // Classes
    val CB_CRAFT_META_ITEM_CLASS = getCBClass("inventory.CraftMetaItem")
    val SECTION_PATH_DATA_CLASS = getClass("org.bukkit.configuration.SectionPathData")
    val PALETTED_CONTAINER_DATA_CLASS = getClass("SRC(net.minecraft.world.level.chunk.PalettedContainer\$Data)")
    val HOLDER_SET_DIRECT_CLASS = getClass("SRC(net.minecraft.core.HolderSet\$Direct)")
    
    // Constructors
    val ENUM_MAP_CONSTRUCTOR = getConstructor(EnumMap::class, false, Class::class)
    val MEMORY_SECTION_CONSTRUCTOR = getConstructor(MemorySection::class, true, ConfigurationSection::class, String::class)
    val SECTION_PATH_DATA_CONSTRUCTOR = getConstructor(SECTION_PATH_DATA_CLASS, true, Any::class)
    val ITEM_STACK_CONSTRUCTOR = getConstructor(MojangStack::class, false, ItemLike::class)
    val CHUNK_ACCESS_CONSTRUCTOR = getConstructor(ChunkAccess::class, false, ChunkPos::class, UpgradeData::class, LevelHeightAccessor::class, Registry::class, Long::class, Array<LevelChunkSection>::class, BlendingData::class)
    val INVENTORY_CONSTRUCTOR = getConstructor(MojangInventory::class, false, MojangPlayer::class)
    val BIOME_CLIMATE_SETTINGS_CONSTRUCTOR = getConstructor(ClimateSettings::class, true, Boolean::class, Float::class, TemperatureModifier::class, Float::class)
    val BIOME_SPECIAL_EFFECTS_CONSTRUCTOR = getConstructor(BiomeSpecialEffects::class, true, Int::class, Int::class, Int::class, Int::class, Optional::class, Optional::class, GrassColorModifier::class, Optional::class, Optional::class, Optional::class, Optional::class, Optional::class)
    val BIOME_GENERATION_SETTINGS_CONSTRUCTOR = getConstructor(BiomeGenerationSettings::class, true, Map::class, List::class)
    val MOB_SPAWN_SETTINGS_CONSTRUCTOR = getConstructor(MobSpawnSettings::class, true, Float::class, Map::class, Map::class)
    val BIOME_CONSTRUCTOR = getConstructor(Biome::class, true, ClimateSettings::class, BiomeSpecialEffects::class, BiomeGenerationSettings::class, MobSpawnSettings::class)
    
    // Methods
    val CLASS_LOADER_PARENT_FIELD by lazy { getField(ClassLoader::class.java, true, "parent") }
    val CLASS_LOADER_DEFINE_CLASS_METHOD by lazy { getMethod(ClassLoader::class, true, "defineClass", String::class, ByteArray::class, Int::class, Int::class, ProtectionDomain::class) }
    val CB_CRAFT_META_APPLY_TO_ITEM_METHOD = getMethod(CB_CRAFT_META_ITEM_CLASS, true, "applyToItem", CompoundTag::class)
    val FEATURE_SORTER_BUILD_FEATURES_PER_STEP_METHOD = getMethod(FeatureSorter::class, true, "SRM(net.minecraft.world.level.biome.FeatureSorter buildFeaturesPerStep)", List::class, JavaFunction::class, Boolean::class)
    val LEVEL_CHUNK_SECTION_SET_BLOCK_STATE_METHOD = getMethod(LevelChunkSection::class, true, "SRM(net.minecraft.world.level.chunk.LevelChunkSection setBlockState)", Int::class, Int::class, Int::class, BlockState::class, Boolean::class)
    val CRAFT_BLOCK_DATA_IS_PREFERRED_TOOL_METHOD = getMethod(CraftBlockData::class, true, "isPreferredTool", BlockState::class, MojangStack::class)
    val ITEM_STACK_HURT_AND_BREAK_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack hurtAndBreak)", Int::class, MojangLivingEntity::class, Consumer::class)
    val EXPERIENCE_ORB_REPAIR_PLAYER_ITEMS_METHOD = getMethod(ExperienceOrb::class, true, "SRM(net.minecraft.world.entity.ExperienceOrb repairPlayerItems)", MojangPlayer::class, Int::class)
    val ENTITY_PLAY_STEP_SOUND_METHOD = getMethod(MojangEntity::class, true, "SRM(net.minecraft.world.entity.Entity playStepSound)", BlockPos::class, BlockState::class)
    val LIVING_ENTITY_PLAY_BLOCK_FALL_SOUND_METHOD = getMethod(MojangLivingEntity::class, true, "SRM(net.minecraft.world.entity.LivingEntity playBlockFallSound)")
    val REGISTRY_FILE_CODEC_DECODE_METHOD = getMethod(RegistryFileCodec::class, false, "SRM(net.minecraft.resources.RegistryFileCodec decode)", DynamicOps::class, Any::class)
    val REGISTRY_BY_NAME_CODEC_METHOD = getMethod(Registry::class, true, "SRM(net.minecraft.core.Registry lambda\$byNameCodec\$1)", ResourceLocation::class)
    val MAPPED_REGISTRY_LIFECYCLE_METHOD = getMethod(MappedRegistry::class, false, "SRM(net.minecraft.core.MappedRegistry lifecycle)", Any::class)
    val MAPPED_REGISTRY_REGISTER_MAPPING_METHOD = getMethod(MappedRegistry::class, false, "SRM(net.minecraft.core.MappedRegistry registerMapping)", Int::class, ResourceKey::class, Any::class, Lifecycle::class)
    val DEFAULTED_MAPPED_REGISTRY_GET_METHOD = getMethod(DefaultedMappedRegistry::class, false, "SRM(net.minecraft.core.DefaultedMappedRegistry get)", ResourceLocation::class)
    val COMPOUND_TAG_READ_NAMED_TAG_DATA_METHOD = getMethod(CompoundTag::class, true, "SRM(net.minecraft.nbt.CompoundTag readNamedTagData)", TagType::class, String::class, DataInput::class, Int::class, NbtAccounter::class)
    val ABSTRACT_FURNACE_BLOCK_ENTITY_GET_BURN_DURATION_METHOD = getMethod(AbstractFurnaceBlockEntity::class, true, "SRM(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity getBurnDuration)", MojangStack::class)
    val RECIPE_GET_REMAINING_ITEMS_METHOD = getMethod(Recipe::class, false, "SRM(net.minecraft.world.item.crafting.Recipe getRemainingItems)", Container::class)
    val NON_NULL_LIST_SET_METHOD = getMethod(NonNullList::class, false, "SRM(net.minecraft.core.NonNullList set)", Int::class, Any::class)
    val BREWING_STAND_BLOCK_ENTITY_DO_BREW_METHOD = getMethod(BrewingStandBlockEntity::class, true, "doBrew", Level::class, BlockPos::class, NonNullList::class, BrewingStandBlockEntity::class)
    val SIMPLE_PLUGIN_MANAGER_FIRE_EVENT_METHOD = getMethod(SimplePluginManager::class, true, "fireEvent", Event::class)
    val ITEM_STACK_LOAD_METHOD = getMethod(MojangStack::class, true, "load", CompoundTag::class)
    val RULE_PROCESSOR_PROCESS_BLOCK_METHOD = getMethod(RuleProcessor::class, false, "SRM(net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor processBlock)", LevelReader::class, BlockPos::class, BlockPos::class, StructureTemplate.StructureBlockInfo::class, StructureTemplate.StructureBlockInfo::class, StructurePlaceSettings::class)
    val PROCESSOR_RULE_TEST_METHOD = getMethod(ProcessorRule::class, false, "SRM(net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule test)", BlockState::class, BlockState::class, BlockPos::class, BlockPos::class, BlockPos::class, RandomSource::class)
    val ORE_FEATURE_CAN_PLACE_ORE_METHOD = getMethod(OreFeature::class, true, "SRM(net.minecraft.world.level.levelgen.feature.OreFeature canPlaceOre)", BlockState::class, JavaFunction::class, RandomSource::class, OreConfiguration::class, TargetBlockState::class, MutableBlockPos::class)
    val ORE_FEATURE_DO_PLACE_METHOD = getMethod(OreFeature::class, true, "SRM(net.minecraft.world.level.levelgen.feature.OreFeature doPlace)", WorldGenLevel::class, RandomSource::class, OreConfiguration::class, Double::class, Double::class, Double::class, Double::class, Double::class, Double::class, Int::class, Int::class, Int::class, Int::class, Int::class)
    val REPLACE_BLOCK_PLACE_METHOD = getMethod(ReplaceBlockFeature::class, false, "SRM(net.minecraft.world.level.levelgen.feature.ReplaceBlockFeature place)", FeaturePlaceContext::class)
    val RULE_TEST_TEST_METHOD = getMethod(RuleTest::class, false, "SRM(net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest test)", BlockState::class, RandomSource::class)
    val BLOCK_GETTER_GET_BLOCK_STATE_METHOD = getMethod(BlockGetter::class, false, "SRM(net.minecraft.world.level.BlockGetter getBlockState)", BlockPos::class)
    val FEATURE_PLACE_CONTEXT_RANDOM_METHOD = getMethod(FeaturePlaceContext::class, false, "SRM(net.minecraft.world.level.levelgen.feature.FeaturePlaceContext random)")
    val DISPENSER_BLOCK_GET_DISPENSE_METHOD_METHOD = getMethod(DispenserBlock::class, true, "SRM(net.minecraft.world.level.block.DispenserBlock getDispenseMethod)", MojangStack::class)
    val HOLDER_REFERENCE_BIND_VALUE_METHOD = getMethod(Holder.Reference::class, true, "SRM(net.minecraft.core.Holder\$Reference bindValue)", Any::class)
    val NOISE_ROUTER_DATA_OVERWORLD_METHOD = getMethod(NoiseRouterData::class, true, "SRM(net.minecraft.world.level.levelgen.NoiseRouterData overworld)", HolderGetter::class, HolderGetter::class, Boolean::class, Boolean::class)
    val SEMAPHORE_ACQUIRE_METHOD = getMethod(Semaphore::class, false, "acquire")
    val CRAFT_META_ITEM_CLONE_METHOD = getMethod(CB_CRAFT_META_ITEM_CLASS, true, "clone")
    
    // Fields
    val CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD = getField(CB_CRAFT_META_ITEM_CLASS, true, "unhandledTags")
    val COMMAND_NODE_CHILDREN_FIELD = getField(CommandNode::class, true, "children")
    val COMMAND_NODE_LITERALS_FIELD = getField(CommandNode::class, true, "literals")
    val COMMAND_NODE_ARGUMENTS_FIELD = getField(CommandNode::class, true, "arguments")
    val PREPARE_ITEM_CRAFT_EVENT_MATRIX_FIELD = getField(PrepareItemCraftEvent::class, true, "matrix")
    val MEMORY_SECTION_MAP_FIELD = getField(MemorySection::class, true, "map")
    val HANDLER_LIST_HANDLERS_FIELD = getField(HandlerList::class, true, "handlers")
    val HANDLER_LIST_HANDLER_SLOTS_FIELD = getField(HandlerList::class, true, "handlerslots")
    val BLOCK_PHYSICS_EVENT_CHANGED_FIELD = getField(BlockPhysicsEvent::class, true, "changed")
    val SECTION_PATH_DATA_DATA_FIELD = getField(SECTION_PATH_DATA_CLASS, true, "data")
    val PALETTED_CONTAINER_DATA_FIELD = getField(PalettedContainer::class, true, "SRF(net.minecraft.world.level.chunk.PalettedContainer data)")
    val PALETTED_CONTAINER_DATA_PALETTE_FIELD = getField(PALETTED_CONTAINER_DATA_CLASS, true, "SRF(net.minecraft.world.level.chunk.PalettedContainer\$Data palette)")
    val PALETTED_CONTAINER_DATA_STORAGE_FIELD = getField(PALETTED_CONTAINER_DATA_CLASS, true, "SRF(net.minecraft.world.level.chunk.PalettedContainer\$Data storage)")
    val LINEAR_PALETTE_VALUES_FIELD = getField(LinearPalette::class, true, "SRF(net.minecraft.world.level.chunk.LinearPalette values)")
    val HASH_MAP_PALETTE_VALUES_FIELD = getField(HashMapPalette::class, true, "SRF(net.minecraft.world.level.chunk.HashMapPalette values)")
    val BLOCK_DEFAULT_BLOCK_STATE_FIELD = getField(Block::class, true, "SRF(net.minecraft.world.level.block.Block defaultBlockState)")
    val MAPPED_REGISTRY_FROZEN_FIELD = getField(MappedRegistry::class, true, "SRF(net.minecraft.core.MappedRegistry frozen)")
    val BIOME_GENERATION_SETTINGS_FEATURES_FIELD = getField(BiomeGenerationSettings::class, true, "SRF(net.minecraft.world.level.biome.BiomeGenerationSettings features)")
    val LEVEL_CHUNK_SECTION_STATES_FIELD = getField(LevelChunkSection::class, true, "SRF(net.minecraft.world.level.chunk.LevelChunkSection states)")
    val LEVEL_CHUNK_SECTION_BIOMES_FIELD = getField(LevelChunkSection::class, true, "biomes")
    @JvmField
    val LEVEL_CHUNK_SECTION_NON_EMPTY_BLOCK_COUNT_FIELD = getField(LevelChunkSection::class, true, "SRF(net.minecraft.world.level.chunk.LevelChunkSection nonEmptyBlockCount)")
    @JvmField
    val LEVEL_CHUNK_SECTION_SPECIAL_COLLIDING_BLOCKS_FIELD = getField(LevelChunkSection::class, true, "specialCollidingBlocks")
    @JvmField
    val LEVEL_CHUNK_SECTION_KNOWN_BLOCK_COLLISION_DATA_FIELD = getField(LevelChunkSection::class, true, "knownBlockCollisionData")
    @JvmField
    val LEVEL_CHUNK_SECTION_TICKING_LIST_FIELD = getField(LevelChunkSection::class, true, "tickingList")
    @JvmField
    val LEVEL_CHUNK_SECTION_FLUID_STATE_COUNT_FIELD = getServerSoftwareField(LevelChunkSection::class, true, "fluidStateCount", ServerSoftware.PUFFERFISH)
    val HOLDER_SET_DIRECT_CONTENTS_FIELD = getField(HOLDER_SET_DIRECT_CLASS, true, "SRF(net.minecraft.core.HolderSet\$Direct contents)")
    val HOLDER_SET_DIRECT_CONTENTS_SET_FIELD = getField(HOLDER_SET_DIRECT_CLASS, true, "SRF(net.minecraft.core.HolderSet\$Direct contentsSet)")
    val ITEM_COMBINER_MENU_INPUT_SLOTS_FIELD = getField(ItemCombinerMenu::class, true, "SRF(net.minecraft.world.inventory.ItemCombinerMenu inputSlots)")
    val ITEM_COMBINER_MENU_PLAYER_FIELD = getField(ItemCombinerMenu::class, true, "SRF(net.minecraft.world.inventory.ItemCombinerMenu player)")
    val ABSTRACT_FURNACE_BLOCK_ENTITY_ITEMS_FIELD = getField(AbstractFurnaceBlockEntity::class, true, "SRF(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity items)")
    val PROCESSOR_RULE_INPUT_PREDICATE_FIELD = getField(ProcessorRule::class, true, "SRF(net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule inputPredicate)")
    val PROCESSOR_RULE_LOC_PREDICATE_FIELD = getField(ProcessorRule::class, true, "SRF(net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule locPredicate)")
    val PROCESSOR_RULE_POS_PREDICATE_FIELD = getField(ProcessorRule::class, true, "SRF(net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule posPredicate)")
    val TARGET_BLOCK_STATE_TARGET_FIELD = getField(TargetBlockState::class, false, "SRF(net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration\$TargetBlockState target)")
    val INVENTORY_ARMOR_FIELD = getField(MojangInventory::class, true, "SRF(net.minecraft.world.entity.player.Inventory armor)")
    val THREADING_DETECTOR_LOCK_FIELD = getField(ThreadingDetector::class, true, "SRF(net.minecraft.util.ThreadingDetector lock)")
    val ITEM_STACK_ITEM_META_FIELD = getField(BukkitStack::class, true, "meta")
    val CRAFT_ITEM_STACK_HANDLE_FIELD = getField(CraftItemStack::class, true, "handle")
    val ITEM_STACK_ITEM_FIELD = getField(MojangStack::class, true, "SRF(net.minecraft.world.item.ItemStack item)")
    
}
