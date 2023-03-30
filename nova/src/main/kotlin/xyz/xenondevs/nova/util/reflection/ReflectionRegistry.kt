package xyz.xenondevs.nova.util.reflection

import com.mojang.brigadier.tree.CommandNode
import com.mojang.serialization.Codec
import com.mojang.serialization.DynamicOps
import com.mojang.serialization.Lifecycle
import net.minecraft.core.BlockPos
import net.minecraft.core.BlockPos.MutableBlockPos
import net.minecraft.core.MappedRegistry
import net.minecraft.core.NonNullList
import net.minecraft.core.Registry
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.TagType
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.resources.RegistryFileCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.players.PlayerList
import net.minecraft.util.RandomSource
import net.minecraft.world.BossEvent
import net.minecraft.world.Container
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.inventory.ItemCombinerMenu
import net.minecraft.world.item.Equipable
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.BannerDuplicateRecipe
import net.minecraft.world.item.crafting.BookCloningRecipe
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.enchantment.EnchantmentCategory
import net.minecraft.world.item.enchantment.EnchantmentHelper
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
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.level.chunk.ChunkAccess
import net.minecraft.world.level.chunk.HashMapPalette
import net.minecraft.world.level.chunk.LevelChunkSection
import net.minecraft.world.level.chunk.LinearPalette
import net.minecraft.world.level.chunk.PalettedContainer
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
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.craftbukkit.v1_19_R3.block.data.CraftBlockData
import org.bukkit.event.Event
import org.bukkit.event.HandlerList
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import org.bukkit.plugin.SimplePluginManager
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getCB
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getCBClass
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getClass
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getConstructor
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getField
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethod
import java.io.DataInput
import java.security.ProtectionDomain
import java.util.*
import java.util.function.Consumer
import kotlin.jvm.internal.CallableReference
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.EquipmentSlot as MojangEquipmentSlot
import net.minecraft.world.entity.LivingEntity as MojangLivingEntity
import net.minecraft.world.entity.player.Inventory as MojangInventory
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.Item as MojangItem
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.level.block.Block as MojangBlock
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
    val CB_CRAFT_META_APPLY_TO_METHOD = getMethod(CB_CRAFT_META_ITEM_CLASS, true, "applyToItem", CompoundTag::class)
    val FEATURE_SORTER_BUILD_FEATURES_PER_STEP_METHOD = getMethod(FeatureSorter::class, true, "SRM(net.minecraft.world.level.biome.FeatureSorter buildFeaturesPerStep)", List::class, JavaFunction::class, Boolean::class)
    val STATE_HOLDER_CODEC_METHOD = getMethod(StateHolder::class, true, "SRM(net.minecraft.world.level.block.state.StateHolder codec)", Codec::class, JavaFunction::class)
    val LEVEL_CHUNK_SECTION_SET_BLOCK_STATE_METHOD = getMethod(LevelChunkSection::class, true, "SRM(net.minecraft.world.level.chunk.LevelChunkSection setBlockState)", Int::class, Int::class, Int::class, BlockState::class, Boolean::class)
    val CRAFT_BLOCK_DATA_IS_PREFERRED_TOOL_METHOD = getMethod(CraftBlockData::class, true, "isPreferredTool", BlockState::class, MojangStack::class)
    val ITEM_STACK_GET_ATTRIBUTE_MODIFIERS_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack getAttributeModifiers)", MojangEquipmentSlot::class)
    val ITEM_STACK_HURT_AND_BREAK_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack hurtAndBreak)", Int::class, MojangLivingEntity::class, Consumer::class)
    val ITEM_STACK_HURT_ENTITY_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack hurtEnemy)", MojangLivingEntity::class, MojangPlayer::class)
    val PLAYER_ATTACK_METHOD = getMethod(MojangPlayer::class, false, "SRM(net.minecraft.world.entity.player.Player attack)", MojangEntity::class)
    val ANVIL_MENU_CREATE_RESULT_METHOD = getMethod(AnvilMenu::class, false, "SRM(net.minecraft.world.inventory.AnvilMenu createResult)")
    val ENCHANTMENT_HELPER_GET_AVAILABLE_ENCHANTMENT_RESULTS_METHOD = getMethod(EnchantmentHelper::class, false, "SRM(net.minecraft.world.item.enchantment.EnchantmentHelper getAvailableEnchantmentResults)", Int::class, MojangStack::class, Boolean::class)
    val ENCHANTMENT_HELPER_GET_ENCHANTMENT_COST_METHOD = getMethod(EnchantmentHelper::class, true, "SRM(net.minecraft.world.item.enchantment.EnchantmentHelper getEnchantmentCost)", RandomSource::class, Int::class, Int::class, MojangStack::class)
    val ENCHANTMENT_HELPER_SELECT_ENCHANTMENT_METHOD = getMethod(EnchantmentHelper::class, false, "SRM(net.minecraft.world.item.enchantment.EnchantmentHelper selectEnchantment)", RandomSource::class, MojangStack::class, Int::class, Boolean::class)
    val ENCHANTMENT_HELPER_GET_KNOCKBACK_BONUS_METHOD = getMethod(EnchantmentHelper::class, false, "SRM(net.minecraft.world.item.enchantment.EnchantmentHelper getKnockbackBonus)", MojangLivingEntity::class)
    val ENCHANTMENT_CATEGORY_CAN_ENCHANT_METHOD = getMethod(EnchantmentCategory::class, false, "SRM(net.minecraft.world.item.enchantment.EnchantmentCategory canEnchant)", MojangItem::class)
    val ITEM_IS_ENCHANTABLE_METHOD = getMethod(MojangItem::class, false, "SRM(net.minecraft.world.item.Item isEnchantable)", MojangStack::class)
    val ITEM_GET_ENCHANTMENT_VALUE_METHOD = getMethod(MojangItem::class, false, "SRM(net.minecraft.world.item.Item getEnchantmentValue)")
    val EXPERIENCE_ORB_REPAIR_PLAYER_ITEMS_METHOD = getMethod(ExperienceOrb::class, true, "SRM(net.minecraft.world.entity.ExperienceOrb repairPlayerItems)", MojangPlayer::class, Int::class)
    val ENTITY_PLAY_STEP_SOUND_METHOD = getMethod(MojangEntity::class, true, "SRM(net.minecraft.world.entity.Entity playStepSound)", BlockPos::class, BlockState::class)
    val LIVING_ENTITY_PLAY_BLOCK_FALL_SOUND_METHOD = getMethod(LivingEntity::class, true, "SRM(net.minecraft.world.entity.LivingEntity playBlockFallSound)")
    val ITEM_STACK_GET_MAX_STACK_SIZE_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack getMaxStackSize)")
    val PLAYER_LIST_BROADCAST_METHOD = getMethod(PlayerList::class, false, "SRM(net.minecraft.server.players.PlayerList broadcast)", MojangPlayer::class, Double::class, Double::class, Double::class, Double::class, ResourceKey::class, Packet::class)
    val BLOCK_PLAYER_WILL_DESTROY_METHOD = getMethod(MojangBlock::class, false, "SRM(net.minecraft.world.level.block.Block playerWillDestroy)", Level::class, BlockPos::class, BlockState::class, MojangPlayer::class)
    val INVENTORY_HURT_ARMOR_METHOD = getMethod(MojangInventory::class, false, "SRM(net.minecraft.world.entity.player.Inventory hurtArmor)", DamageSource::class, Float::class, IntArray::class)
    val ITEM_GET_DEFAULT_ATTRIBUTE_MODIFIERS_METHOD = getMethod(MojangItem::class, false, "SRM(net.minecraft.world.item.Item getDefaultAttributeModifiers)", MojangEquipmentSlot::class)
    val REGISTRY_FILE_CODEC_DECODE_METHOD = getMethod(RegistryFileCodec::class, false, "SRM(net.minecraft.resources.RegistryFileCodec decode)", DynamicOps::class, Any::class)
    val REGISTRY_BY_NAME_CODEC_METHOD = getMethod(Registry::class, true, "SRM(net.minecraft.core.Registry lambda\$byNameCodec\$1)", ResourceLocation::class)
    val MAPPED_REGISTRY_LIFECYCLE_METHOD = getMethod(MappedRegistry::class, false, "SRM(net.minecraft.core.MappedRegistry lifecycle)", Any::class)
    val MAPPED_REGISTRY_REGISTER_MAPPING_METHOD = getMethod(MappedRegistry::class, false, "SRM(net.minecraft.core.MappedRegistry registerMapping)", Int::class, ResourceKey::class, Any::class, Lifecycle::class)
    val COMPOUND_TAG_READ_NAMED_TAG_DATA_METHOD = getMethod(CompoundTag::class, true, "SRM(net.minecraft.nbt.CompoundTag readNamedTagData)", TagType::class, String::class, DataInput::class, Int::class, NbtAccounter::class)
    val ABSTRACT_FURNACE_BLOCK_ENTITY_IS_FUEL_METHOD = getMethod(AbstractFurnaceBlockEntity::class, false, "SRM(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity isFuel)", MojangStack::class)
    val ABSTRACT_FURNACE_BLOCK_ENTITY_GET_BURN_DURATION_METHOD = getMethod(AbstractFurnaceBlockEntity::class, true, "SRM(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity getBurnDuration)", MojangStack::class)
    val ITEM_GET_CRAFTING_REMAINING_ITEM_METHOD = getMethod(MojangItem::class, false, "SRM(net.minecraft.world.item.Item getCraftingRemainingItem)")
    val ITEM_STACK_GET_ITEM_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack getItem)")
    val RECIPE_GET_REMAINING_ITEMS_METHOD = getMethod(Recipe::class, false, "SRM(net.minecraft.world.item.crafting.Recipe getRemainingItems)", Container::class)
    val BANNER_DUPLICATE_RECIPE_GET_REMAINING_ITEMS_METHOD = getMethod(BannerDuplicateRecipe::class, false, "SRM(net.minecraft.world.item.crafting.BannerDuplicateRecipe getRemainingItems)", CraftingContainer::class)
    val BOOK_CLONING_RECIPE_GET_REMAINING_ITEMS_METHOD = getMethod(BookCloningRecipe::class, false, "SRM(net.minecraft.world.item.crafting.BookCloningRecipe getRemainingItems)", CraftingContainer::class)
    val ABSTRACT_FURNACE_BLOCK_ENTITY_SERVER_TICK_METHOD = getMethod(AbstractFurnaceBlockEntity::class, false, "SRM(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity serverTick)", Level::class, BlockPos::class, BlockState::class, AbstractFurnaceBlockEntity::class)
    val NON_NULL_LIST_SET_METHOD = getMethod(NonNullList::class, false, "SRM(net.minecraft.core.NonNullList set)", Int::class, Any::class)
    val ITEM_HAS_CRAFTING_REMAINING_ITEM_METHOD = getMethod(Item::class, false, "SRM(net.minecraft.world.item.Item hasCraftingRemainingItem)")
    val BREWING_STAND_BLOCK_ENTITY_DO_BREW_METHOD = getMethod(BrewingStandBlockEntity::class, true, "SRM(net.minecraft.world.level.block.entity.BrewingStandBlockEntity doBrew)", Level::class, BlockPos::class, NonNullList::class, BrewingStandBlockEntity::class)
    val ITEM_ENTITY_FIRE_IMMUNE_METHOD = getMethod(ItemEntity::class, false, "SRM(net.minecraft.world.entity.item.ItemEntity fireImmune)")
    val ITEM_IS_FIRE_RESISTANT_METHOD = getMethod(MojangItem::class, false, "SRM(net.minecraft.world.item.Item isFireResistant)")
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
    val CLIENTBOUND_BOSS_EVENT_PACKET_CREATE_ADD_PACKET_METHOD = getMethod(ClientboundBossEventPacket::class, false, "SRM(net.minecraft.network.protocol.game.ClientboundBossEventPacket createAddPacket)", BossEvent::class)
    val EQUIPABLE_GET_METHOD = getMethod(Equipable::class, false, "SRM(net.minecraft.world.item.Equipable get)", MojangStack::class)
    val LIVING_ENTITY_ON_EQUIP_ITEM_METHOD = getMethod(LivingEntity::class, false, "SRM(net.minecraft.world.entity.LivingEntity onEquipItem)", MojangEquipmentSlot::class, MojangStack::class, MojangStack::class, Boolean::class)
    
    // Fields
    val CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD = getField(CB_CRAFT_META_ITEM_CLASS, true, "unhandledTags")
    val COMMAND_NODE_CHILDREN_FIELD = getField(CommandNode::class, true, "children")
    val COMMAND_NODE_LITERALS_FIELD = getField(CommandNode::class, true, "literals")
    val COMMAND_NODE_ARGUMENTS_FIELD = getField(CommandNode::class, true, "arguments")
    val CALLABLE_REFERENCE_RECEIVER_FIELD = getField(CallableReference::class, true, "receiver")
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
    val BLOCK_STATE_CODEC_FIELD = getField(BlockState::class, false, "SRF(net.minecraft.world.level.block.state.BlockState CODEC)")
    val MAPPED_REGISTRY_FROZEN_FIELD = getField(MappedRegistry::class, true, "SRF(net.minecraft.core.MappedRegistry frozen)")
    val BIOME_GENERATION_SETTINGS_FEATURES_FIELD = getField(BiomeGenerationSettings::class, true, "SRF(net.minecraft.world.level.biome.BiomeGenerationSettings features)")
    val LEVEL_CHUNK_SECTION_STATES_FIELD = getField(LevelChunkSection::class, true, "SRF(net.minecraft.world.level.chunk.LevelChunkSection states)")
    val LEVEL_CHUNK_SECTION_J_FIELD = getField(LevelChunkSection::class, true, "j")
    val HOLDER_SET_DIRECT_CONTENTS_FIELD = getField(HOLDER_SET_DIRECT_CLASS, true, "SRF(net.minecraft.core.HolderSet\$Direct contents)")
    val HOLDER_SET_DIRECT_CONTENTS_SET_FIELD = getField(HOLDER_SET_DIRECT_CLASS, true, "SRF(net.minecraft.core.HolderSet\$Direct contentsSet)")
    val ITEM_COMBINER_MENU_INPUT_SLOTS_FIELD = getField(ItemCombinerMenu::class, true, "SRF(net.minecraft.world.inventory.ItemCombinerMenu inputSlots)")
    val ITEM_COMBINER_MENU_PLAYER_FIELD = getField(ItemCombinerMenu::class, true, "SRF(net.minecraft.world.inventory.ItemCombinerMenu player)")
    val ABSTRACT_FURNACE_BLOCK_ENTITY_ITEMS_FIELD = getField(AbstractFurnaceBlockEntity::class, true, "SRF(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity items)")
    val PROCESSOR_RULE_INPUT_PREDICATE_FIELD = getField(ProcessorRule::class, true, "SRF(net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule inputPredicate)")
    val PROCESSOR_RULE_LOC_PREDICATE_FIELD = getField(ProcessorRule::class, true, "SRF(net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule locPredicate)")
    val PROCESSOR_RULE_POS_PREDICATE_FIELD = getField(ProcessorRule::class, true, "SRF(net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule posPredicate)")
    val TARGET_BLOCK_STATE_TARGET_FIELD = getField(TargetBlockState::class, false, "SRF(net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration\$TargetBlockState target)")
    val INVENTORY_ARMOR_FIELD = getField(MojangInventory::class, true, "SRF(net.minecraft.world.inventory.Inventory armor)")
    
}
