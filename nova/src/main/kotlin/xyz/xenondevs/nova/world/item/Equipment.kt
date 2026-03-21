package xyz.xenondevs.nova.world.item

import kotlinx.serialization.Serializable
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.resources.builder.task.RuntimeEquipmentData
import xyz.xenondevs.nova.serialization.kotlinx.EquipmentSerializer

/**
 * Represents a custom armor texture.
 */
@Serializable(with = EquipmentSerializer::class)
class Equipment internal constructor(
    override val entry: RegistryEntry.Nova<Equipment>,
    internal val runtimeData: Provider<RuntimeEquipmentData?>
) : NovaRegistryElement<Equipment> {
    
    override fun toString(): String = key.toString()
    
}