package xyz.xenondevs.nova.world.item

import kotlinx.serialization.Serializable
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.ksp.annotation.GenerateFlatMapExtensions
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.resources.builder.task.RuntimeEquipmentData
import xyz.xenondevs.nova.serialization.kotlinx.EquipmentEntrySerializer
import xyz.xenondevs.nova.serialization.kotlinx.EquipmentEntrySetSerializer
import xyz.xenondevs.nova.serialization.kotlinx.EquipmentSerializer

/**
 * Serializable type alias for `RegistryEntry.Nova<Equipment>` using [EquipmentEntrySerializer].
 */
typealias NovaEquipmentEntry = @Serializable(with = EquipmentEntrySerializer::class) RegistryEntry.Nova<Equipment>

/**
 * Serializable type alias for `RegistryEntrySet.Nova<Equipment>` using [EquipmentEntrySetSerializer].
 */
typealias NovaEquipmentEntrySet = @Serializable(with = EquipmentEntrySetSerializer::class) RegistryEntrySet.Nova<Equipment>

/**
 * Represents a custom armor texture.
 */
@GenerateFlatMapExtensions
@Serializable(with = EquipmentSerializer::class)
class Equipment internal constructor(
    override val entry: RegistryEntry.Nova<Equipment>,
    internal val runtimeData: Provider<RuntimeEquipmentData?>
) : NovaRegistryElement<Equipment> {
    
    override fun toString(): String = key.toString()
    
}