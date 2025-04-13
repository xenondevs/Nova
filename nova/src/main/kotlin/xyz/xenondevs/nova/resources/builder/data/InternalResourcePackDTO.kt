package xyz.xenondevs.nova.resources.builder.data

/**
 * Opt-in annotation for classes that are low-level data transfer objects (DTOs) for Mojang's resource pack format
 * and have more user-friendly builders available that should be preferred.
 */
@RequiresOptIn("This class is a low-level DTO (Data Transfer Object) for Mojang's resource pack format. Prefer using the higher-level builders for a more user-friendly API.")
annotation class InternalResourcePackDTO