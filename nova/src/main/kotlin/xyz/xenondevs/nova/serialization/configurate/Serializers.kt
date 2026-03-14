package xyz.xenondevs.nova.serialization.configurate

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Keyed
import org.bukkit.attribute.AttributeModifier.Operation
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import xyz.xenondevs.commons.reflection.javaTypeOf
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet

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
    .registerForRegistry(RegistryKey.DIALOG)
    .registerForRegistry(RegistryKey.ENCHANTMENT)
    .registerForRegistry(RegistryKey.ENTITY_TYPE)
    .registerForRegistry(RegistryKey.FLUID)
    .registerForRegistry(RegistryKey.FROG_VARIANT)
    .registerForRegistry(RegistryKey.GAME_EVENT)
    .registerForRegistry(RegistryKey.GAME_RULE)
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
    .registerForRegistry(RegistryKey.ZOMBIE_NAUTILUS_VARIANT)
    // -- Nova Registries --
    .registerForRegistry { NovaRegistries.BLOCK }
    .registerForRegistry { NovaRegistries.ITEM }
    .registerForRegistry { NovaRegistries.EQUIPMENT }
    .registerForRegistry { NovaRegistries.TOOL_TIER }
    .registerForRegistry { NovaRegistries.TOOL_CATEGORY }
    .registerForRegistry { NovaRegistries.NETWORK_TYPE }
    .registerForRegistry { NovaRegistries.ABILITY_TYPE }
    .registerForRegistry { NovaRegistries.ATTACHMENT_TYPE }
    .registerForRegistry { NovaRegistries.RECIPE_TYPE }
    .registerForRegistry { NovaRegistries.GUI_TEXTURE }
    .registerForRegistry { NovaRegistries.WAILA_INFO_PROVIDER }
    .registerForRegistry { NovaRegistries.WAILA_TOOL_ICON_PROVIDER }
    .registerForRegistry { NovaRegistries.ITEM_FILTER_TYPE }
    .registerForRegistry { NovaRegistries.TOOLTIP_STYLE }
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
    .register(KeyConfigurateSerializer)
    .register(NamespacedKeyConfigurateSerializer)
    .register(IdentifierSerializer)
    .register(ResourcePathSerializer)
    .register(PotionEffectSerializer)
    .register(PotionEffectTypeSerializer)
    .register(RecipeChoiceSerializer)
    .register(ResourceFilterSerializer)
    .register(ItemCategorySerializer)
    .register(DamageReductionSerializer)
    .register(ItemDamageFunctionSerializer)
    .register(AttributeModifierDisplaySerializer)
    .build()

private inline fun <reified T> TypeSerializerCollection.Builder.register(serializer: TypeSerializer<T>): TypeSerializerCollection.Builder {
    val type = javaTypeOf<T>()
    register({ it == type }, serializer)
    return this
}

private inline fun <reified T : Keyed> TypeSerializerCollection.Builder.registerForRegistry(register: RegistryKey<T>): TypeSerializerCollection.Builder {
    register<T>(PaperRegistryElementConfigurateSerializer(register))
    register<RegistryEntry.Paper<T>>(PaperRegistryEntryConfigurateSerializer(register))
    register<RegistryEntrySet.Paper<T>>(PaperRegistryEntrySetConfigurateSerializer(register))
    register<TypedKey<T>>(TypedKeyConfigurateSerializer(register))
    register<TagKey<T>>(TagKeyConfigurateSerializer(register))
    register<RegistryKeySet<T>>(RegistryKeySetConfigurateSerializer(register))
    return this
}

private inline fun <reified T : NovaRegistryElement<T>> TypeSerializerCollection.Builder.registerForRegistry(
    crossinline registry: () -> NovaRegistry<T>
): TypeSerializerCollection.Builder {
    register<T>(NovaRegistryElementConfigurateSerializer(registry()))
    register<RegistryEntry.Nova<T>>(NovaRegistryEntryConfigurateSerializer(registry()))
    register<RegistryEntrySet.Nova<T>>(NovaRegistryEntrySetConfigurateSerializer(registry()))
    return this
}