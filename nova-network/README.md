# nova-network

Nova's network module includes packet events and general network utilities.

Note that this module cannot be used standalone as it uses mixins.

## Packet Events: Usage

1. In `JavaPlugin.onEnable`, call `installPacketHandler(JavaPlugin)` (Nova already does this, not required for addons)
2. Implement `PacketListener` in a file and register it via `PacketListener.registerPacketListener()`
3. Annotate a method with `@PacketHandler` that receives a `PacketEvent` parameter (just like Bukkit's event system)

```kotlin
class MyPlugin : JavaPlugin(), PacketListener {
    
    override fun onEnable() {
        installPacketHandler(this)
        registerPacketListener()
    }
    
    @PacketHandler
    fun onPacket(event: ServerboundClientTickEndPacket) {
        println("Tick end")
    }

}
```