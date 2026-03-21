package xyz.xenondevs.nova.serialization.kotlinx

import io.papermc.paper.datacomponent.item.attribute.AttributeModifierDisplay
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction
import io.papermc.paper.datacomponent.item.blocksattacks.ItemDamageFunction
import kotlinx.serialization.modules.SerializersModule
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.NamespacedKey
import org.bukkit.attribute.AttributeModifier
import org.bukkit.inventory.EquipmentSlotGroup
import org.bukkit.inventory.RecipeChoice
import org.bukkit.potion.PotionEffect
import xyz.xenondevs.commons.version.Version
import xyz.xenondevs.nova.resources.builder.ResourceFilter
import java.awt.Color
import java.io.File
import java.util.*

/**
 * A [SerializersModule] for commonly used Nova and Paper types, such as registry elements.
 * Used as the root serializers in [xyz.xenondevs.nova.config.CONFIGS].
 */
val NOVA_SERIALIZERS_MODULE = SerializersModule {
    contextual(AttributeModifier.Operation::class, AttributeModifierOperationSerializer)
    contextual(AttributeModifierDisplay::class, AttributeModifierDisplaySerializer)
    contextual(BlockState::class, BlockStateSerializer)
    contextual(Color::class, ColorStringSerializer)
    contextual(Component::class, ComponentAsMiniMessageSerializer)
    contextual(DamageReduction::class, DamageReductionSerializer)
    contextual(EquipmentSlotGroup::class, EquipmentSlotGroupSerializer)
    contextual(File::class, FileSerializer)
    contextual(Identifier::class, IdentifierSerializer)
    contextual(ItemDamageFunction::class, ItemDamageFunctionKSerializer)
    contextual(Key::class, KeySerializer)
    contextual(NamespacedKey::class, NamespacedKeySerializer)
    contextual(PotionEffect::class, PotionEffectSerializer)
    contextual(RecipeChoice::class, RecipeChoiceSerializer)
    contextual(ResourceFilter::class, ResourceFilterSerializer)
    contextual(UUID::class, UUIDAsStringSerializer)
    contextual(Version::class, VersionSerializer)
    
    contextualPaperRegistryElementSerializers()
    contextualRegistryElementBasedSerializers()
}