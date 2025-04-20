package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.registry.RegistryKey
import org.bukkit.Keyed
import org.bukkit.attribute.AttributeModifier.Operation
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import xyz.xenondevs.commons.reflection.javaTypeOf
import xyz.xenondevs.nova.registry.NovaRegistries

internal val NOVA_CONFIGURATE_SERIALIZERS: TypeSerializerCollection = TypeSerializerCollection.builder()
    // -- Registries --
    .registerForRegistry(RegistryKey.ATTRIBUTE)
    .registerForRegistry(RegistryKey.BANNER_PATTERN)
    .registerForRegistry(RegistryKey.BIOME)
    .registerForRegistry(RegistryKey.BLOCK)
    .registerForRegistry(RegistryKey.CAT_VARIANT)
    .registerForRegistry(RegistryKey.CHICKEN_VARIANT)
    .registerForRegistry(RegistryKey.COW_VARIANT)
    .registerForRegistry(RegistryKey.DAMAGE_TYPE)
    .registerForRegistry(RegistryKey.DATA_COMPONENT_TYPE)
    .registerForRegistry(RegistryKey.ENCHANTMENT)
    .registerForRegistry(RegistryKey.ENTITY_TYPE)
    .registerForRegistry(RegistryKey.FLUID)
    .registerForRegistry(RegistryKey.FROG_VARIANT)
    .registerForRegistry(RegistryKey.GAME_EVENT)
    .registerForRegistry(RegistryKey.INSTRUMENT)
    .registerForRegistry(RegistryKey.ITEM)
    .registerForRegistry(RegistryKey.JUKEBOX_SONG)
    .registerForRegistry(RegistryKey.MAP_DECORATION_TYPE)
    .registerForRegistry(RegistryKey.MEMORY_MODULE_TYPE)
    .registerForRegistry(RegistryKey.MENU)
    .registerForRegistry(RegistryKey.MOB_EFFECT)
    .registerForRegistry(RegistryKey.PAINTING_VARIANT)
    .registerForRegistry(RegistryKey.PARTICLE_TYPE)
    .registerForRegistry(RegistryKey.PIG_VARIANT)
    .registerForRegistry(RegistryKey.POTION)
    .registerForRegistry(RegistryKey.SOUND_EVENT)
    .registerForRegistry(RegistryKey.STRUCTURE)
    .registerForRegistry(RegistryKey.STRUCTURE_TYPE)
    .registerForRegistry(RegistryKey.TRIM_MATERIAL)
    .registerForRegistry(RegistryKey.TRIM_PATTERN)
    .registerForRegistry(RegistryKey.VILLAGER_PROFESSION)
    .registerForRegistry(RegistryKey.VILLAGER_TYPE)
    .registerForRegistry(RegistryKey.WOLF_SOUND_VARIANT)
    .registerForRegistry(RegistryKey.WOLF_VARIANT)
    // -- Nova Registries --
    .register(RegistryEntrySerializer(NovaRegistries.NETWORK_TYPE))
    .register(RegistryEntrySerializer(NovaRegistries.ABILITY_TYPE))
    .register(RegistryEntrySerializer(NovaRegistries.ATTACHMENT_TYPE))
    .register(RegistryEntrySerializer(NovaRegistries.RECIPE_TYPE))
    .register(RegistryEntrySerializer(NovaRegistries.BLOCK))
    .register(RegistryEntrySerializer(NovaRegistries.ITEM))
    .register(RegistryEntrySerializer(NovaRegistries.TOOL_CATEGORY))
    .register(RegistryEntrySerializer(NovaRegistries.TOOL_TIER))
    // -- Misc --
    .register(ExtraMappingEnumSerializer(
        "add_value" to Operation.ADD_NUMBER,
        "add_multiplied_base" to Operation.ADD_SCALAR,
        "add_multiplied_total" to Operation.MULTIPLY_SCALAR_1
    ))
    .register(ColorSerializer)
    .register(BarMatcherSerializer)
    .register(BarMatcherCombinedAnySerializer)
    .register(BarMatcherCombinedAllSerializer)
    .register(BlockLimiterSerializer)
    .register(ComponentSerializer)
    .register(EnumSerializer)
    .register(KeySerializer)
    .register(NamespacedKeySerializer)
    .register(ResourceLocationSerializer)
    .register(ResourcePathSerializer)
    .register(PotionEffectSerializer)
    .register(PotionEffectTypeSerializer)
    .register(RecipeChoiceSerializer)
    .register(ResourceFilterSerializer)
    .register(ItemCategorySerializer)
    .build()

private inline fun <reified T> TypeSerializerCollection.Builder.register(serializer: TypeSerializer<T>): TypeSerializerCollection.Builder {
    val type = javaTypeOf<T>()
    register({ it == type }, serializer)
    return this
}

private inline fun <reified T: Keyed> TypeSerializerCollection.Builder.registerForRegistry(key: RegistryKey<T>): TypeSerializerCollection.Builder {
    register(BukkitRegistryEntrySerializer(key))
    register(TagKeySerializer(key))
    register(RegistryKeySetSerializer(key))
    return this
}