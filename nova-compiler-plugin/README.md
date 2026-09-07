# nova-compiler-plugin

K2 FIR compiler inspections for Nova and Nova addons.

The inspections are reported as compiler warnings during normal Kotlin compilation.

## Inspections

- `NOVA_REGISTRY_ENTRY_COMPARISON`: Comparing a `RegistryEntry<T>` with `T`.
- `NOVA_MATERIAL_USAGE`: Using Bukkit's legacy `Material` API instead of `ItemType` or `BlockType`.
- `NOVA_KEY_TO_STRING`: Calling `Key.toString()` explicitly or through string interpolation instead of `asString()`.
  `NamespacedKey` is excluded because its string representation is defined.
- `NOVA_TYPED_KEY_AS_KEY`: Relying on `TypedKey` implementing `Key`.

Inspections can be suppressed using Kotlin's standard `@Suppress` annotation and the diagnostic
name, for example `@Suppress("NOVA_MATERIAL_USAGE")`.
