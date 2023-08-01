package xyz.xenondevs.nova.data.serialization.configurate

import io.leangen.geantyref.TypeToken
import net.minecraft.core.registries.BuiltInRegistries
import org.spongepowered.configurate.serialize.TypeSerializer
import org.spongepowered.configurate.serialize.TypeSerializerCollection
import xyz.xenondevs.commons.reflection.javaTypeOf
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.byNameTypeSerializer

val NOVA_CONFIGURATE_SERIALIZERS: TypeSerializerCollection = TypeSerializerCollection.builder()
    // -- Nova Serializers --
    .register(BarMatcherSerializer)
    .register(BarMatcherCombinedAnySerializer)
    .register(BarMatcherCombinedAllSerializer)
    .register(BlockLimiterSerializer)
    .register(ComponentSerializer)
    .register(EnumSerializer)
    .register(NamespacedKeySerializer)
    .register(ResourceLocationSerializer)
    .register(ResourcePathSerializer)
    .register(PotionEffectSerializer)
    .register(PotionEffectTypeSerializer)
    .register(ResourceFilterSerializer)
    // -- Vanilla Registries --
    .register(BuiltInRegistries.ATTRIBUTE.byNameTypeSerializer())
    // -- Nova Registries --
    .register(NovaRegistries.NETWORK_TYPE.byNameTypeSerializer())
    .register(NovaRegistries.UPGRADE_TYPE.byNameTypeSerializer())
    .register(NovaRegistries.ABILITY_TYPE.byNameTypeSerializer())
    .register(NovaRegistries.ATTACHMENT_TYPE.byNameTypeSerializer())
    .register(NovaRegistries.RECIPE_TYPE.byNameTypeSerializer())
    .register(NovaRegistries.BLOCK.byNameTypeSerializer())
    .register(NovaRegistries.ITEM.byNameTypeSerializer())
    .register(NovaRegistries.ENCHANTMENT.byNameTypeSerializer())
    .register(NovaRegistries.ENCHANTMENT_CATEGORY.byNameTypeSerializer())
    .register(NovaRegistries.TOOL_CATEGORY.byNameTypeSerializer())
    .register(NovaRegistries.TOOL_TIER.byNameTypeSerializer())
    .build()

private inline fun <reified T> TypeSerializerCollection.Builder.register(serializer: TypeSerializer<T>): TypeSerializerCollection.Builder {
    val type = javaTypeOf<T>()
    register({ it == type }, serializer)
    return this
}

@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> geantyrefTypeTokenOf(): TypeToken<T> =
    TypeToken.get(javaTypeOf<T>()) as TypeToken<T>