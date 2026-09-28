package xyz.xenondevs.nova.serialization.kotlinx

import org.bukkit.attribute.AttributeModifier.Operation

/**
 * Serializes [Operation] with case-insensitive lookup and extra legacy name mappings
 * (`add_value`, `add_multiplied_base`, `add_multiplied_total`).
 */
internal object AttributeModifierOperationSerializer : AliasedEnumSerializer<Operation>(
    "xyz.xenondevs.nova.Operation",
    Operation.entries.toTypedArray(),
    mapOf(
        "add_value" to Operation.ADD_NUMBER,
        "add_multiplied_base" to Operation.ADD_SCALAR,
        "add_multiplied_total" to Operation.MULTIPLY_SCALAR_1
    )
)
