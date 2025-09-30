package xyz.xenondevs.nova.util

import net.kyori.adventure.text.Component
import net.minecraft.network.chat.ComponentSerialization
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.world.BossEvent
import xyz.xenondevs.commons.collections.enumSetOf
import xyz.xenondevs.nova.util.component.adventure.toAdventureComponent
import xyz.xenondevs.nova.util.component.adventure.toNMSComponent
import java.util.*
import net.kyori.adventure.bossbar.BossBar as AdventureBossBar

internal class BossBar(
    val id: UUID,
    name: Component = Component.text(""),
    progress: Float = 0.0f,
    color: BossEvent.BossBarColor = BossEvent.BossBarColor.WHITE,
    overlay: BossEvent.BossBarOverlay = BossEvent.BossBarOverlay.PROGRESS,
    darkenScreen: Boolean = false,
    playMusic: Boolean = false,
    createWorldFog: Boolean = false
) {
    
    var name: Component = name
        set(value) {
            field = value
            
            _addOperation = null
            _updateNameOperation = null
            
            _addPacket = null
            _updateNamePacket = null
        }
    var progress: Float = progress
        set(value) {
            field = value
            
            _addOperation = null
            _updateProgressOperation = null
            
            _addPacket = null
            _updateProgressPacket = null
        }
    var color: BossEvent.BossBarColor = color
        set(value) {
            field = value
            
            _addOperation = null
            _updateStyleOperation = null
            
            _addPacket = null
            _updateStylePacket = null
        }
    var overlay: BossEvent.BossBarOverlay = overlay
        set(value) {
            field = value
            
            _addOperation = null
            _updateStyleOperation = null
            
            _addPacket = null
            _updateStylePacket = null
        }
    var darkenScreen: Boolean = darkenScreen
        set(value) {
            field = value
            
            _addOperation = null
            _updatePropertiesOperation = null
            
            _addPacket = null
            _updatePropertiesPacket = null
        }
    var playMusic: Boolean = playMusic
        set(value) {
            field = value
            
            _addOperation = null
            _updatePropertiesOperation = null
            
            _addPacket = null
            _updatePropertiesPacket = null
        }
    var createWorldFog: Boolean = createWorldFog
        set(value) {
            field = value
            
            _addOperation = null
            _updatePropertiesOperation = null
            
            _addPacket = null
            _updatePropertiesPacket = null
        }
    
    private var _addOperation: ClientboundBossEventPacket.AddOperation? = null
    val addOperation: ClientboundBossEventPacket.AddOperation
        get() {
            if (_addOperation == null) {
                var properties = 0
                if (darkenScreen)
                    properties = properties or 1
                if (playMusic)
                    properties = properties or 2
                if (createWorldFog)
                    properties = properties or 4
                
                val buf = RegistryFriendlyByteBuf()
                ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, name.toNMSComponent())
                buf.writeFloat(progress)
                buf.writeEnum(color)
                buf.writeEnum(overlay)
                buf.writeByte(properties)
                
                _addOperation = ClientboundBossEventPacket.AddOperation(buf)
            }
            return _addOperation!!
        }
    
    private var _updateNameOperation: ClientboundBossEventPacket.UpdateNameOperation? = null
    val updateNameOperation: ClientboundBossEventPacket.UpdateNameOperation
        get() {
            if (_updateNameOperation == null) {
                _updateNameOperation = ClientboundBossEventPacket.UpdateNameOperation(this.name.toNMSComponent())
            }
            return _updateNameOperation!!
        }
    
    private var _updateProgressOperation: ClientboundBossEventPacket.UpdateProgressOperation? = null
    val updateProgressOperation: ClientboundBossEventPacket.UpdateProgressOperation
        get() {
            if (_updateProgressOperation == null) {
                _updateProgressOperation = ClientboundBossEventPacket.UpdateProgressOperation(progress)
            }
            return _updateProgressOperation!!
        }
    
    private var _updateStyleOperation: ClientboundBossEventPacket.UpdateStyleOperation? = null
    val updateStyleOperation: ClientboundBossEventPacket.UpdateStyleOperation
        get() {
            if (_updateStyleOperation == null) {
                _updateStyleOperation = ClientboundBossEventPacket.UpdateStyleOperation(color, overlay)
            }
            return _updateStyleOperation!!
        }
    
    private var _updatePropertiesOperation: ClientboundBossEventPacket.UpdatePropertiesOperation? = null
    val updatePropertiesOperation: ClientboundBossEventPacket.UpdatePropertiesOperation
        get() {
            if (_updatePropertiesOperation == null) {
                _updatePropertiesOperation = ClientboundBossEventPacket.UpdatePropertiesOperation(darkenScreen, playMusic, createWorldFog)
            }
            return _updatePropertiesOperation!!
        }
    
    val removeOperation: ClientboundBossEventPacket.Operation = ClientboundBossEventPacket.REMOVE_OPERATION
    
    private var _addPacket: ClientboundBossEventPacket? = null
    val addPacket: ClientboundBossEventPacket
        get() {
            if (_addPacket == null) {
                _addPacket = ClientboundBossEventPacket(id, addOperation)
            }
            return _addPacket!!
        }
    
    private var _updateNamePacket: ClientboundBossEventPacket? = null
    val updateNamePacket: ClientboundBossEventPacket
        get() {
            if (_updateNamePacket == null) {
                _updateNamePacket = ClientboundBossEventPacket(id, updateNameOperation)
            }
            return _updateNamePacket!!
        }
    
    private var _updateProgressPacket: ClientboundBossEventPacket? = null
    val updateProgressPacket: ClientboundBossEventPacket
        get() {
            if (_updateProgressPacket == null) {
                _updateProgressPacket = ClientboundBossEventPacket(id, updateProgressOperation)
            }
            return _updateProgressPacket!!
        }
    
    private var _updateStylePacket: ClientboundBossEventPacket? = null
    val updateStylePacket: ClientboundBossEventPacket
        get() {
            if (_updateStylePacket == null) {
                _updateStylePacket = ClientboundBossEventPacket(id, updateStyleOperation)
            }
            return _updateStylePacket!!
        }
    
    private var _updatePropertiesPacket: ClientboundBossEventPacket? = null
    val updatePropertiesPacket: ClientboundBossEventPacket
        get() {
            if (_updatePropertiesPacket == null) {
                _updatePropertiesPacket = ClientboundBossEventPacket(id, updatePropertiesOperation)
            }
            return _updatePropertiesPacket!!
        }
    
    val removePacket = ClientboundBossEventPacket(id, removeOperation)
    
    fun toAdventure(): AdventureBossBar {
        val adventureColor = when (color) {
            BossEvent.BossBarColor.PINK -> AdventureBossBar.Color.PINK
            BossEvent.BossBarColor.BLUE -> AdventureBossBar.Color.BLUE
            BossEvent.BossBarColor.RED -> AdventureBossBar.Color.RED
            BossEvent.BossBarColor.GREEN -> AdventureBossBar.Color.GREEN
            BossEvent.BossBarColor.YELLOW -> AdventureBossBar.Color.YELLOW
            BossEvent.BossBarColor.PURPLE -> AdventureBossBar.Color.PURPLE
            BossEvent.BossBarColor.WHITE -> AdventureBossBar.Color.WHITE
        }
        
        val adventureOverlay = when (overlay) {
            BossEvent.BossBarOverlay.PROGRESS -> AdventureBossBar.Overlay.PROGRESS
            BossEvent.BossBarOverlay.NOTCHED_6 -> AdventureBossBar.Overlay.NOTCHED_6
            BossEvent.BossBarOverlay.NOTCHED_10 -> AdventureBossBar.Overlay.NOTCHED_10
            BossEvent.BossBarOverlay.NOTCHED_12 -> AdventureBossBar.Overlay.NOTCHED_12
            BossEvent.BossBarOverlay.NOTCHED_20 -> AdventureBossBar.Overlay.NOTCHED_20
        }
        
        val adventureFlags = enumSetOf<AdventureBossBar.Flag>()
        if (darkenScreen)
            adventureFlags.add(AdventureBossBar.Flag.DARKEN_SCREEN)
        if (playMusic)
            adventureFlags.add(AdventureBossBar.Flag.PLAY_BOSS_MUSIC)
        if (createWorldFog)
            adventureFlags.add(AdventureBossBar.Flag.CREATE_WORLD_FOG)
        
        return AdventureBossBar.bossBar(name, progress, adventureColor, adventureOverlay, adventureFlags)
    }
    
    companion object {
        
        fun of(id: UUID, operation: ClientboundBossEventPacket.AddOperation) = BossBar(
            id,
            operation.name.toAdventureComponent(),
            operation.progress,
            operation.color,
            operation.overlay,
            operation.darkenScreen,
            operation.playMusic,
            operation.createWorldFog
        )
        
    }
    
}