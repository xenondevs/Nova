# nova-detekt-rules

A custom [detekt](https://detekt.dev/) ruleset that flags common pitfalls when using Nova.

## Gradle

```kotlin
repositories {
    maven("https://repo.xenondevs.xyz/releases/")
}

dependencies {
    detektPlugins("xyz.xenondevs.nova:nova-detekt-rules")
}
```

## Rules

Currently, there is only one rule. More rules may be added in the future.

* `RegistryEntryComparison`: Flags comparisons between `RegistryEntry<T>` and `T`, which will never succeed.