package xyz.xenondevs.nova.resources.lookup

internal class ResourceLookupException(lookup: String, cause: Throwable) : Exception(
    "Failed to load resource lookup '$lookup'. " +
        "Resource lookups invalidated. " +
        "Restart the server to trigger a resource pack rebuild.",
    cause
)