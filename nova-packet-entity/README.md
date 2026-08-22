# nova-packet-entity

Nova's packet entity module includes reactive, packet-only entities.

Note that this module cannot be used standalone as it uses mixins.

## Packet Entities: Usage

1. Create a packet entity using one of the generated builder functions
2. Call `spawn()` to make it visible to players
3. Update its properties reactively or imperatively
4. Call `despawn()` when it should no longer exist

### Reactive API

Properties bound to providers are observed and automatically synchronized with viewers.

```kotlin
val display = packetItemDisplay {
    location by locationProvider
    
    metadata {
        itemStack by itemProvider
    }
    
    onInteract {
        player.sendMessage("Interacted")
        InteractionResult.Success()
    }
}

display.spawn()
```

### Imperative API

The returned packet entity and its metadata can also be updated directly.

```kotlin
val display = packetItemDisplay {
    location by player.location
    
    metadata {
        itemStack by item
    }
}

display.spawn()

display.location = newLocation
display.metadata.itemStack = newItem

display.despawn()
```
