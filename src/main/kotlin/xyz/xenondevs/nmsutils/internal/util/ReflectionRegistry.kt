package xyz.xenondevs.nmsutils.internal.util

import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.server.ServerAdvancementManager
import net.minecraft.server.network.ServerCommonPacketListenerImpl
import net.minecraft.server.network.ServerConnectionListener
import net.minecraft.world.BossEvent.BossBarColor
import net.minecraft.world.BossEvent.BossBarOverlay
import org.bukkit.craftbukkit.v1_20_R2.tag.CraftTag
import xyz.xenondevs.nmsutils.internal.util.ReflectionUtils.getClass
import xyz.xenondevs.nmsutils.internal.util.ReflectionUtils.getConstructor
import xyz.xenondevs.nmsutils.internal.util.ReflectionUtils.getField
import xyz.xenondevs.nmsutils.internal.util.ReflectionUtils.getFieldOrNull
import xyz.xenondevs.nmsutils.internal.util.ReflectionUtils.getMethod
import java.util.*

internal object ReflectionRegistry {
    
    // Classes
    val CONNECTION_PROTOCOL_PACKET_SET_CLASS = getClass("SRC(net.minecraft.network.ConnectionProtocol\$PacketSet)")
    val BOSS_BAR_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$Operation)")
    val BOSS_BAR_ADD_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$AddOperation)")
    val BOSS_BAR_UPDATE_PROGRESS_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateProgressOperation)")
    val BOSS_BAR_UPDATE_NAME_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateNameOperation)")
    val BOSS_BAR_UPDATE_STYLE_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateStyleOperation)")
    val BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdatePropertiesOperation)")
    val SERVERBOUND_INTERACT_PACKET_INTERACTION_ACTION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ServerboundInteractPacket\$InteractionAction)")
    val SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ServerboundInteractPacket\$InteractionAtLocationAction)")
    
    // Constructors
    val CLIENTBOUND_BOSS_EVENT_PACKET_CONSTRUCTOR = getConstructor(ClientboundBossEventPacket::class.java, true, UUID::class.java, BOSS_BAR_OPERATION_CLASS)
    val BOSS_BAR_ADD_OPERATION_CONSTRUCTOR = getConstructor(BOSS_BAR_ADD_OPERATION_CLASS, true, FriendlyByteBuf::class.java)
    val BOSS_BAR_UPDATE_PROGRESS_OPERATION_CONSTRUCTOR = getConstructor(BOSS_BAR_UPDATE_PROGRESS_OPERATION_CLASS, true, Float::class.java)
    val BOSS_BAR_UPDATE_NAME_OPERATION_CONSTRUCTOR = getConstructor(BOSS_BAR_UPDATE_NAME_OPERATION_CLASS, true, Component::class.java)
    val BOSS_BAR_UPDATE_STYLE_OPERATION_CONSTRUCTOR = getConstructor(BOSS_BAR_UPDATE_STYLE_OPERATION_CLASS, true, BossBarColor::class.java, BossBarOverlay::class.java)
    val BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CONSTRUCTOR = getConstructor(BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CLASS, true, Boolean::class.java, Boolean::class.java, Boolean::class.java)
    
    // Methods
    val PACKET_SET_GET_ID_METHOD = getMethod(CONNECTION_PROTOCOL_PACKET_SET_CLASS, true, "SRM(net.minecraft.network.ConnectionProtocol\$PacketSet getId)", Class::class.java)
    
    // Fields
    val CRAFT_TAG_TAG_KEY_FIELD = getField(CraftTag::class.java, true, "tag")
    val SERVER_CONNECTION_LISTENER_CHANNELS_FIELD = getField(ServerConnectionListener::class.java, true, "SRF(net.minecraft.server.network.ServerConnectionListener channels)")
    val SERVER_COMMON_PACKET_LISTENER_IMPL_CONNECTION_FIELD = getField(ServerCommonPacketListenerImpl::class.java, true, "SRF(net.minecraft.server.network.ServerCommonPacketListenerImpl connection)")
    val CLIENTBOUND_BOSS_EVENT_PACKET_ID_FIELD = getField(ClientboundBossEventPacket::class.java, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket id)")
    val CLIENTBOUND_BOSS_EVENT_PACKET_OPERATION_FIELD = getField(ClientboundBossEventPacket::class.java, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket operation)")
    val CLIENTBOUND_BOSS_EVENT_PACKET_REMOVE_OPERATION_FIELD = getField(ClientboundBossEventPacket::class.java, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket REMOVE_OPERATION)")
    val CONNECTION_PROTOCOL_FLOWS_FIELD = getField(ConnectionProtocol::class.java, true, "SRF(net.minecraft.network.ConnectionProtocol flows)")
    val BOSS_BAR_ADD_OPERATION_NAME_FIELD = getField(BOSS_BAR_ADD_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$AddOperation name)")
    val BOSS_BAR_ADD_OPERATION_PROGRESS_FIELD = getField(BOSS_BAR_ADD_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$AddOperation progress)")
    val BOSS_BAR_ADD_OPERATION_COLOR_FIELD = getField(BOSS_BAR_ADD_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$AddOperation color)")
    val BOSS_BAR_ADD_OPERATION_OVERLAY_FIELD = getField(BOSS_BAR_ADD_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$AddOperation overlay)")
    val BOSS_BAR_ADD_OPERATION_DARKEN_SCREEN_FIELD = getField(BOSS_BAR_ADD_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$AddOperation darkenScreen)")
    val BOSS_BAR_ADD_OPERATION_PLAY_MUSIC_FIELD = getField(BOSS_BAR_ADD_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$AddOperation playMusic)")
    val BOSS_BAR_ADD_OPERATION_CREATE_WORLD_FOG_FIELD = getField(BOSS_BAR_ADD_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$AddOperation createWorldFog)")
    val BOSS_BAR_UPDATE_PROGRESS_OPERATION_PROGRESS_FIELD = getField(BOSS_BAR_UPDATE_PROGRESS_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateProgressOperation progress)")
    val BOSS_BAR_UPDATE_NAME_OPERATION_NAME_FIELD = getField(BOSS_BAR_UPDATE_NAME_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateNameOperation name)")
    val BOSS_BAR_UPDATE_STYLE_OPERATION_COLOR_FIELD = getField(BOSS_BAR_UPDATE_STYLE_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateStyleOperation color)")
    val BOSS_BAR_UPDATE_STYLE_OPERATION_OVERLAY_FIELD = getField(BOSS_BAR_UPDATE_STYLE_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateStyleOperation overlay)")
    val BOSS_BAR_UPDATE_PROPERTIES_OPERATION_DARKEN_SCREEN_FIELD = getField(BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdatePropertiesOperation darkenScreen)")
    val BOSS_BAR_UPDATE_PROPERTIES_OPERATION_PLAY_MUSIC_FIELD = getField(BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdatePropertiesOperation playMusic)")
    val BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CREATE_WORLD_FOG_FIELD = getField(BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdatePropertiesOperation createWorldFog)")
    val SERVERBOUND_INTERACT_PACKET_ENTITY_ID_FIELD = getField(ServerboundInteractPacket::class.java, true, "SRF(net.minecraft.network.protocol.game.ServerboundInteractPacket entityId)")
    val SERVERBOUND_INTERACT_PACKET_ACTION_FIELD = getField(ServerboundInteractPacket::class.java, true, "SRF(net.minecraft.network.protocol.game.ServerboundInteractPacket action)")
    val SERVERBOUND_INTERACT_PACKET_INTERACTION_ACTION_HAND_FIELD = getField(SERVERBOUND_INTERACT_PACKET_INTERACTION_ACTION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ServerboundInteractPacket\$InteractionAction hand)")
    val SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_HAND_FIELD = getField(SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ServerboundInteractPacket\$InteractionAtLocationAction hand)")
    val SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_LOCATION_FIELD = getField(SERVERBOUND_INTERACT_PACKET_INTERACTION_AT_LOCATION_ACTION_CLASS, true, "SRF(net.minecraft.network.protocol.game.ServerboundInteractPacket\$InteractionAtLocationAction location)")
    val SERVER_ADVANCEMENT_MANAGER_TREE_FIELD = getField(ServerAdvancementManager::class.java, true, "SRF(net.minecraft.server.ServerAdvancementManager tree)")
    val CODEC_DATA_PACKET_SET_FIELD = getField(ConnectionProtocol.CodecData::class.java, true, "SRF(net.minecraft.network.ConnectionProtocol\$CodecData packetSet)")
    
    // Paper exclusive fields
    val CLIENTBOUND_SYSTEM_CHAT_PACKET_ADVENTURE_CONTENT_FIELD = getFieldOrNull(ClientboundSystemChatPacket::class.java, true, "adventure\$content")
    val CLIENTBOUND_SET_ACTION_BAR_TEXT_PACKET_ADVENTURE_TEXT_FIELD = getFieldOrNull(ClientboundSetActionBarTextPacket::class.java, true, "adventure\$text")
    val CLIENTBOUND_SET_ACTION_BAR_TEXT_PACKET_COMPONENTS_FIELD = getFieldOrNull(ClientboundSetActionBarTextPacket::class.java, true, "components")
    
}