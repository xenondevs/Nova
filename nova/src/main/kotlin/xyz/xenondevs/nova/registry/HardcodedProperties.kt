package xyz.xenondevs.nova.registry

/**
 * It is generally recommended to make properties like tool tiers, material options, recipes, etc. configurable.
 *
 * If you still want to hardcode specific properties, you can use this annotation to opt-in.
 */
@RequiresOptIn
annotation class HardcodedProperties
