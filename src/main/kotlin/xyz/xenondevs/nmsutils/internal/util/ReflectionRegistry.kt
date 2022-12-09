package xyz.xenondevs.nmsutils.internal.util

import net.minecraft.advancements.Advancement
import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.LighthingBoltPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.server.network.ServerConnectionListener
import net.minecraft.world.BossEvent.BossBarColor
import net.minecraft.world.BossEvent.BossBarOverlay
import org.bukkit.craftbukkit.v1_19_R2.tag.CraftTag
import xyz.xenondevs.nmsutils.internal.util.ReflectionUtils.getClass
import xyz.xenondevs.nmsutils.internal.util.ReflectionUtils.getConstructor
import xyz.xenondevs.nmsutils.internal.util.ReflectionUtils.getField
import java.util.*

@Suppress("MemberVisibilityCanBePrivate")
internal object ReflectionRegistry {
    
    // Classes
    val STATE_PROPERTIES_PREDICATE_CLASS = getClass("SRC(net.minecraft.advancements.critereon.StatePropertiesPredicate)")
    val EXACT_PROPERTY_MATCHER_CLASS = getClass("SRC(net.minecraft.advancements.critereon.StatePropertiesPredicate\$ExactPropertyMatcher)")
    val RANGED_PROPERTY_MATCHER_CLASS = getClass("SRC(net.minecraft.advancements.critereon.StatePropertiesPredicate\$RangedPropertyMatcher)")
    val BOSS_BAR_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$Operation)")
    val BOSS_BAR_ADD_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$AddOperation)")
    val BOSS_BAR_UPDATE_PROGRESS_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateProgressOperation)")
    val BOSS_BAR_UPDATE_NAME_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateNameOperation)")
    val BOSS_BAR_UPDATE_STYLE_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdateStyleOperation)")
    val BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CLASS = getClass("SRC(net.minecraft.network.protocol.game.ClientboundBossEventPacket\$UpdatePropertiesOperation)")
    
    // Constructors
    val ADVANCEMENT_BUILDER_CONSTRUCTOR = getConstructor(Advancement.Builder::class.java, true)
    val STATE_PROPERTIES_PREDICATE_CONSTRUCTOR = getConstructor(STATE_PROPERTIES_PREDICATE_CLASS, true, List::class.java)
    val EXACT_PROPERTY_MATCHER_CONSTRUCTOR = getConstructor(EXACT_PROPERTY_MATCHER_CLASS, true, String::class.java, String::class.java)
    val RANGED_PROPERTY_MATCHER_CONSTRUCTOR = getConstructor(RANGED_PROPERTY_MATCHER_CLASS, true, String::class.java, String::class.java, String::class.java)
    val LIGHTNING_BOLT_PREDICATE_CONSTRUCTOR = getConstructor(LighthingBoltPredicate::class.java, true, MinMaxBounds.Ints::class.java, EntityPredicate::class.java)
    val CLIENTBOUND_BOSS_EVENT_PACKET_CONSTRUCTOR = getConstructor(ClientboundBossEventPacket::class.java, true, UUID::class.java, BOSS_BAR_OPERATION_CLASS)
    val BOSS_BAR_ADD_OPERATION_CONSTRUCTOR = getConstructor(BOSS_BAR_ADD_OPERATION_CLASS, true, FriendlyByteBuf::class.java)
    val BOSS_BAR_UPDATE_PROGRESS_OPERATION_CONSTRUCTOR = getConstructor(BOSS_BAR_UPDATE_PROGRESS_OPERATION_CLASS, true, Float::class.java)
    val BOSS_BAR_UPDATE_NAME_OPERATION_CONSTRUCTOR = getConstructor(BOSS_BAR_UPDATE_NAME_OPERATION_CLASS, true, Component::class.java)
    val BOSS_BAR_UPDATE_STYLE_OPERATION_CONSTRUCTOR = getConstructor(BOSS_BAR_UPDATE_STYLE_OPERATION_CLASS, true, BossBarColor::class.java, BossBarOverlay::class.java)
    val BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CONSTRUCTOR = getConstructor(BOSS_BAR_UPDATE_PROPERTIES_OPERATION_CLASS, true, Boolean::class.java, Boolean::class.java, Boolean::class.java)
    
    // Fields
    val CRAFT_TAG_TAG_KEY_FIELD = getField(CraftTag::class.java, true, "tag")
    val SERVER_CONNECTION_LISTENER_CHANNELS_FIELD = getField(ServerConnectionListener::class.java, true, "SRF(net.minecraft.server.network.ServerConnectionListener channels)")
    val CLIENTBOUND_BOSS_EVENT_PACKET_ID_FIELD = getField(ClientboundBossEventPacket::class.java, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket id)")
    val CLIENTBOUND_BOSS_EVENT_PACKET_OPERATION_FIELD = getField(ClientboundBossEventPacket::class.java, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket operation)")
    val CLIENTBOUND_BOSS_EVENT_PACKET_REMOVE_OPERATION_FIELD = getField(ClientboundBossEventPacket::class.java, true, "SRF(net.minecraft.network.protocol.game.ClientboundBossEventPacket REMOVE_OPERATION)")
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

}