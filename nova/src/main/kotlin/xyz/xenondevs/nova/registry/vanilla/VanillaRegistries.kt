package xyz.xenondevs.nova.registry.vanilla

import net.minecraft.core.registries.Registries
import xyz.xenondevs.nova.registry.vanilla.VanillaRegistryAccess.registryOrThrow

object VanillaRegistries {
    
    @JvmField
    val ACTIVITY = registryOrThrow(Registries.ACTIVITY)
    @JvmField
    val ATTRIBUTE = registryOrThrow(Registries.ATTRIBUTE)
    @JvmField
    val BANNER_PATTERN = registryOrThrow(Registries.BANNER_PATTERN)
    @JvmField
    val BIOME = registryOrThrow(Registries.BIOME)
    @JvmField
    val BIOME_SOURCE = registryOrThrow(Registries.BIOME_SOURCE)
    @JvmField
    val BLOCK = registryOrThrow(Registries.BLOCK)
    @JvmField
    val BLOCK_ENTITY_TYPE = registryOrThrow(Registries.BLOCK_ENTITY_TYPE)
    @JvmField
    val BLOCK_PREDICATE_TYPE = registryOrThrow(Registries.BLOCK_PREDICATE_TYPE)
    @JvmField
    val BLOCK_STATE_PROVIDER_TYPE = registryOrThrow(Registries.BLOCK_STATE_PROVIDER_TYPE)
    @JvmField
    val CARVER = registryOrThrow(Registries.CARVER)
    @JvmField
    val CAT_VARIANT = registryOrThrow(Registries.CAT_VARIANT)
    @JvmField
    val CHAT_TYPE = registryOrThrow(Registries.CHAT_TYPE)
    @JvmField
    val CHUNK_GENERATOR = registryOrThrow(Registries.CHUNK_GENERATOR)
    @JvmField
    val CHUNK_STATUS = registryOrThrow(Registries.CHUNK_STATUS)
    @JvmField
    val COMMAND_ARGUMENT_TYPE = registryOrThrow(Registries.COMMAND_ARGUMENT_TYPE)
    @JvmField
    val CONFIGURED_CARVER = registryOrThrow(Registries.CONFIGURED_CARVER)
    @JvmField
    val CONFIGURED_FEATURE = registryOrThrow(Registries.CONFIGURED_FEATURE)
    @JvmField
    val CUSTOM_STAT = registryOrThrow(Registries.CUSTOM_STAT)
    @JvmField
    val DAMAGE_TYPE = registryOrThrow(Registries.DAMAGE_TYPE)
    @JvmField
    val DECORATED_POT_PATTERNS = registryOrThrow(Registries.DECORATED_POT_PATTERN)
    @JvmField
    val DENSITY_FUNCTION = registryOrThrow(Registries.DENSITY_FUNCTION)
    @JvmField
    val DENSITY_FUNCTION_TYPE = registryOrThrow(Registries.DENSITY_FUNCTION_TYPE)
    @JvmField
    val DIMENSION = registryOrThrow(Registries.DIMENSION)
    @JvmField
    val DIMENSION_TYPE = registryOrThrow(Registries.DIMENSION_TYPE)
    @JvmField
    val ENCHANTMENT = registryOrThrow(Registries.ENCHANTMENT)
    @JvmField
    val ENTITY_TYPE = registryOrThrow(Registries.ENTITY_TYPE)
    @JvmField
    val FEATURE = registryOrThrow(Registries.FEATURE)
    @JvmField
    val FEATURE_SIZE_TYPE = registryOrThrow(Registries.FEATURE_SIZE_TYPE)
    @JvmField
    val FLAT_LEVEL_GENERATOR_PRESET = registryOrThrow(Registries.FLAT_LEVEL_GENERATOR_PRESET)
    @JvmField
    val FLOAT_PROVIDER_TYPE = registryOrThrow(Registries.FLOAT_PROVIDER_TYPE)
    @JvmField
    val FLUID = registryOrThrow(Registries.FLUID)
    @JvmField
    val FOLIAGE_PLACER_TYPE = registryOrThrow(Registries.FOLIAGE_PLACER_TYPE)
    @JvmField
    val FROG_VARIANT = registryOrThrow(Registries.FROG_VARIANT)
    @JvmField
    val GAME_EVENT = registryOrThrow(Registries.GAME_EVENT)
    @JvmField
    val HEIGHT_PROVIDER_TYPE = registryOrThrow(Registries.HEIGHT_PROVIDER_TYPE)
    @JvmField
    val INSTRUMENT = registryOrThrow(Registries.INSTRUMENT)
    @JvmField
    val INT_PROVIDER_TYPE = registryOrThrow(Registries.INT_PROVIDER_TYPE)
    @JvmField
    val ITEM = registryOrThrow(Registries.ITEM)
    @JvmField
    val LEVEL_STEM = registryOrThrow(Registries.LEVEL_STEM)
    @JvmField
    val LOOT_CONDITION_TYPE = registryOrThrow(Registries.LOOT_CONDITION_TYPE)
    @JvmField
    val LOOT_FUNCTION_TYPE = registryOrThrow(Registries.LOOT_FUNCTION_TYPE)
    @JvmField
    val LOOT_NBT_PROVIDER_TYPE = registryOrThrow(Registries.LOOT_NBT_PROVIDER_TYPE)
    @JvmField
    val LOOT_NUMBER_PROVIDER_TYPE = registryOrThrow(Registries.LOOT_NUMBER_PROVIDER_TYPE)
    @JvmField
    val LOOT_POOL_ENTRY_TYPE = registryOrThrow(Registries.LOOT_POOL_ENTRY_TYPE)
    @JvmField
    val LOOT_SCORE_PROVIDER_TYPE = registryOrThrow(Registries.LOOT_SCORE_PROVIDER_TYPE)
    @JvmField
    val MATERIAL_CONDITION = registryOrThrow(Registries.MATERIAL_CONDITION)
    @JvmField
    val MATERIAL_RULE = registryOrThrow(Registries.MATERIAL_RULE)
    @JvmField
    val MEMORY_MODULE_TYPE = registryOrThrow(Registries.MEMORY_MODULE_TYPE)
    @JvmField
    val MENU = registryOrThrow(Registries.MENU)
    @JvmField
    val MOB_EFFECT = registryOrThrow(Registries.MOB_EFFECT)
    @JvmField
    val MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST = registryOrThrow(Registries.MULTI_NOISE_BIOME_SOURCE_PARAMETER_LIST)
    @JvmField
    val NOISE = registryOrThrow(Registries.NOISE)
    @JvmField
    val NOISE_SETTINGS = registryOrThrow(Registries.NOISE_SETTINGS)
    @JvmField
    val PAINTING_VARIANT = registryOrThrow(Registries.PAINTING_VARIANT)
    @JvmField
    val PARTICLE_TYPE = registryOrThrow(Registries.PARTICLE_TYPE)
    @JvmField
    val PLACED_FEATURE = registryOrThrow(Registries.PLACED_FEATURE)
    @JvmField
    val PLACEMENT_MODIFIER_TYPE = registryOrThrow(Registries.PLACEMENT_MODIFIER_TYPE)
    @JvmField
    val POINT_OF_INTEREST_TYPE = registryOrThrow(Registries.POINT_OF_INTEREST_TYPE)
    @JvmField
    val POSITION_SOURCE_TYPE = registryOrThrow(Registries.POSITION_SOURCE_TYPE)
    @JvmField
    val POS_RULE_TEST = registryOrThrow(Registries.POS_RULE_TEST)
    @JvmField
    val POTION = registryOrThrow(Registries.POTION)
    @JvmField
    val PROCESSOR_LIST = registryOrThrow(Registries.PROCESSOR_LIST)
    @JvmField
    val RECIPE_SERIALIZER = registryOrThrow(Registries.RECIPE_SERIALIZER)
    @JvmField
    val RECIPE_TYPE = registryOrThrow(Registries.RECIPE_TYPE)
    @JvmField
    val ROOT_PLACER_TYPE = registryOrThrow(Registries.ROOT_PLACER_TYPE)
    @JvmField
    val RULE_TEST = registryOrThrow(Registries.RULE_TEST)
    @JvmField
    val SCHEDULE = registryOrThrow(Registries.SCHEDULE)
    @JvmField
    val SENSOR_TYPE = registryOrThrow(Registries.SENSOR_TYPE)
    @JvmField
    val SOUND_EVENT = registryOrThrow(Registries.SOUND_EVENT)
    @JvmField
    val STAT_TYPE = registryOrThrow(Registries.STAT_TYPE)
    @JvmField
    val STRUCTURE = registryOrThrow(Registries.STRUCTURE)
    @JvmField
    val STRUCTURE_PIECE = registryOrThrow(Registries.STRUCTURE_PIECE)
    @JvmField
    val STRUCTURE_PLACEMENT = registryOrThrow(Registries.STRUCTURE_PLACEMENT)
    @JvmField
    val STRUCTURE_POOL_ELEMENT = registryOrThrow(Registries.STRUCTURE_POOL_ELEMENT)
    @JvmField
    val STRUCTURE_PROCESSOR = registryOrThrow(Registries.STRUCTURE_PROCESSOR)
    @JvmField
    val STRUCTURE_SET = registryOrThrow(Registries.STRUCTURE_SET)
    @JvmField
    val STRUCTURE_TYPE = registryOrThrow(Registries.STRUCTURE_TYPE)
    @JvmField
    val TEMPLATE_POOL = registryOrThrow(Registries.TEMPLATE_POOL)
    @JvmField
    val TREE_DECORATOR_TYPE = registryOrThrow(Registries.TREE_DECORATOR_TYPE)
    @JvmField
    val TRIM_MATERIAL = registryOrThrow(Registries.TRIM_MATERIAL)
    @JvmField
    val TRIM_PATTERN = registryOrThrow(Registries.TRIM_PATTERN)
    @JvmField
    val TRUNK_PLACER_TYPE = registryOrThrow(Registries.TRUNK_PLACER_TYPE)
    @JvmField
    val VILLAGER_PROFESSION = registryOrThrow(Registries.VILLAGER_PROFESSION)
    @JvmField
    val VILLAGER_TYPE = registryOrThrow(Registries.VILLAGER_TYPE)
    @JvmField
    val WORLD_PRESET = registryOrThrow(Registries.WORLD_PRESET)
    
}