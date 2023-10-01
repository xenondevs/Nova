@file:Suppress("UNCHECKED_CAST", "unused")

package xyz.xenondevs.nmsutils.network

import net.minecraft.network.ConnectionProtocol
import net.minecraft.network.protocol.BundleDelimiterPacket
import net.minecraft.network.protocol.PacketFlow
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.common.ClientboundResourcePackPacket
import net.minecraft.network.protocol.common.ClientboundUpdateTagsPacket
import net.minecraft.network.protocol.common.ServerboundClientInformationPacket
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket
import net.minecraft.network.protocol.common.ServerboundPongPacket
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket
import net.minecraft.network.protocol.configuration.ClientboundFinishConfigurationPacket
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket
import net.minecraft.network.protocol.configuration.ClientboundUpdateEnabledFeaturesPacket
import net.minecraft.network.protocol.configuration.ServerboundFinishConfigurationPacket
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundAddExperienceOrbPacket
import net.minecraft.network.protocol.game.ClientboundAnimatePacket
import net.minecraft.network.protocol.game.ClientboundAwardStatsPacket
import net.minecraft.network.protocol.game.ClientboundBlockChangedAckPacket
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundBlockEventPacket
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket
import net.minecraft.network.protocol.game.ClientboundBossEventPacket
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket
import net.minecraft.network.protocol.game.ClientboundChunkBatchFinishedPacket
import net.minecraft.network.protocol.game.ClientboundChunkBatchStartPacket
import net.minecraft.network.protocol.game.ClientboundChunksBiomesPacket
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket
import net.minecraft.network.protocol.game.ClientboundCommandsPacket
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket
import net.minecraft.network.protocol.game.ClientboundCooldownPacket
import net.minecraft.network.protocol.game.ClientboundCustomChatCompletionsPacket
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ClientboundDeleteChatPacket
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket
import net.minecraft.network.protocol.game.ClientboundGameEventPacket
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket
import net.minecraft.network.protocol.game.ClientboundPlaceGhostRecipePacket
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoRemovePacket
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundRecipePacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.network.protocol.game.ClientboundSectionBlocksUpdatePacket
import net.minecraft.network.protocol.game.ClientboundSelectAdvancementsTabPacket
import net.minecraft.network.protocol.game.ClientboundServerDataPacket
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket
import net.minecraft.network.protocol.game.ClientboundSetBorderCenterPacket
import net.minecraft.network.protocol.game.ClientboundSetBorderLerpSizePacket
import net.minecraft.network.protocol.game.ClientboundSetBorderSizePacket
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDelayPacket
import net.minecraft.network.protocol.game.ClientboundSetBorderWarningDistancePacket
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheCenterPacket
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket
import net.minecraft.network.protocol.game.ClientboundSetDefaultSpawnPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityLinkPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket
import net.minecraft.network.protocol.game.ClientboundSetObjectivePacket
import net.minecraft.network.protocol.game.ClientboundSetPassengersPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.protocol.game.ClientboundSetScorePacket
import net.minecraft.network.protocol.game.ClientboundSetSimulationDistancePacket
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.network.protocol.game.ClientboundStartConfigurationPacket
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ClientboundTabListPacket
import net.minecraft.network.protocol.game.ClientboundTagQueryPacket
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQuery
import net.minecraft.network.protocol.game.ServerboundChangeDifficultyPacket
import net.minecraft.network.protocol.game.ServerboundChatAckPacket
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket
import net.minecraft.network.protocol.game.ServerboundChatPacket
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket
import net.minecraft.network.protocol.game.ServerboundChunkBatchReceivedPacket
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket
import net.minecraft.network.protocol.game.ServerboundConfigurationAcknowledgedPacket
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket
import net.minecraft.network.protocol.game.ServerboundEditBookPacket
import net.minecraft.network.protocol.game.ServerboundEntityTagQuery
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundJigsawGeneratePacket
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket
import net.minecraft.network.protocol.game.ServerboundPaddleBoatPacket
import net.minecraft.network.protocol.game.ServerboundPickItemPacket
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket
import net.minecraft.network.protocol.game.ServerboundRecipeBookChangeSettingsPacket
import net.minecraft.network.protocol.game.ServerboundRecipeBookSeenRecipePacket
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket
import net.minecraft.network.protocol.game.ServerboundSeenAdvancementsPacket
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket
import net.minecraft.network.protocol.game.ServerboundSetBeaconPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket
import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket
import net.minecraft.network.protocol.game.ServerboundSetJigsawBlockPacket
import net.minecraft.network.protocol.game.ServerboundSetStructureBlockPacket
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.network.protocol.handshake.ClientIntentionPacket
import net.minecraft.network.protocol.login.ClientboundCustomQueryPacket
import net.minecraft.network.protocol.login.ClientboundGameProfilePacket
import net.minecraft.network.protocol.login.ClientboundHelloPacket
import net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket
import net.minecraft.network.protocol.login.ServerboundCustomQueryAnswerPacket
import net.minecraft.network.protocol.login.ServerboundHelloPacket
import net.minecraft.network.protocol.login.ServerboundKeyPacket
import net.minecraft.network.protocol.login.ServerboundLoginAcknowledgedPacket
import net.minecraft.network.protocol.status.ClientboundPongResponsePacket
import net.minecraft.network.protocol.status.ClientboundStatusResponsePacket
import net.minecraft.network.protocol.status.ServerboundPingRequestPacket
import net.minecraft.network.protocol.status.ServerboundStatusRequestPacket
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry
import kotlin.reflect.KClass

object PacketIdRegistry {
    
    val HANDSHAKING_CLIENT_INTENTION_PACKET = getPacketId(ConnectionProtocol.HANDSHAKING, PacketFlow.SERVERBOUND, ClientIntentionPacket::class)
    val PLAY_SERVERBOUND_ACCEPT_TELEPORTATION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundAcceptTeleportationPacket::class)
    val PLAY_SERVERBOUND_BLOCK_ENTITY_TAG_QUERY = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundBlockEntityTagQuery::class)
    val PLAY_SERVERBOUND_CHANGE_DIFFICULTY_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundChangeDifficultyPacket::class)
    val PLAY_SERVERBOUND_CHAT_ACK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundChatAckPacket::class)
    val PLAY_SERVERBOUND_CHAT_COMMAND_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundChatCommandPacket::class)
    val PLAY_SERVERBOUND_CHAT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundChatPacket::class)
    val PLAY_SERVERBOUND_CHAT_SESSION_UPDATE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundChatSessionUpdatePacket::class)
    val PLAY_SERVERBOUND_CHUNK_BATCH_RECEIVED_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundChunkBatchReceivedPacket::class)
    val PLAY_SERVERBOUND_CLIENT_COMMAND_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundClientCommandPacket::class)
    val PLAY_SERVERBOUND_CLIENT_INFORMATION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundClientInformationPacket::class)
    val PLAY_SERVERBOUND_COMMAND_SUGGESTION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundCommandSuggestionPacket::class)
    val PLAY_SERVERBOUND_CONFIGURATION_ACKNOWLEDGED_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundConfigurationAcknowledgedPacket::class)
    val PLAY_SERVERBOUND_CONTAINER_BUTTON_CLICK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundContainerButtonClickPacket::class)
    val PLAY_SERVERBOUND_CONTAINER_CLICK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundContainerClickPacket::class)
    val PLAY_SERVERBOUND_CONTAINER_CLOSE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundContainerClosePacket::class)
    val PLAY_SERVERBOUND_CUSTOM_PAYLOAD_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundCustomPayloadPacket::class)
    val PLAY_SERVERBOUND_EDIT_BOOK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundEditBookPacket::class)
    val PLAY_SERVERBOUND_ENTITY_TAG_QUERY = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundEntityTagQuery::class)
    val PLAY_SERVERBOUND_INTERACT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundInteractPacket::class)
    val PLAY_SERVERBOUND_JIGSAW_GENERATE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundJigsawGeneratePacket::class)
    val PLAY_SERVERBOUND_KEEP_ALIVE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundKeepAlivePacket::class)
    val PLAY_SERVERBOUND_LOCK_DIFFICULTY_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundLockDifficultyPacket::class)
    val PLAY_SERVERBOUND_MOVE_PLAYER_PACKET_POS = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundMovePlayerPacket.Pos::class)
    val PLAY_SERVERBOUND_MOVE_PLAYER_PACKET_POS_ROT = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundMovePlayerPacket.PosRot::class)
    val PLAY_SERVERBOUND_MOVE_PLAYER_PACKET_ROT = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundMovePlayerPacket.Rot::class)
    val PLAY_SERVERBOUND_MOVE_PLAYER_PACKET_STATUS_ONLY = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundMovePlayerPacket.StatusOnly::class)
    val PLAY_SERVERBOUND_MOVE_VEHICLE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundMoveVehiclePacket::class)
    val PLAY_SERVERBOUND_PADDLE_BOAT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundPaddleBoatPacket::class)
    val PLAY_SERVERBOUND_PICK_ITEM_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundPickItemPacket::class)
    val PLAY_SERVERBOUND_PING_REQUEST_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundPingRequestPacket::class)
    val PLAY_SERVERBOUND_PLACE_RECIPE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundPlaceRecipePacket::class)
    val PLAY_SERVERBOUND_PLAYER_ABILITIES_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundPlayerAbilitiesPacket::class)
    val PLAY_SERVERBOUND_PLAYER_ACTION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundPlayerActionPacket::class)
    val PLAY_SERVERBOUND_PLAYER_COMMAND_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundPlayerCommandPacket::class)
    val PLAY_SERVERBOUND_PLAYER_INPUT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundPlayerInputPacket::class)
    val PLAY_SERVERBOUND_PONG_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundPongPacket::class)
    val PLAY_SERVERBOUND_RECIPE_BOOK_CHANGE_SETTINGS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundRecipeBookChangeSettingsPacket::class)
    val PLAY_SERVERBOUND_RECIPE_BOOK_SEEN_RECIPE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundRecipeBookSeenRecipePacket::class)
    val PLAY_SERVERBOUND_RENAME_ITEM_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundRenameItemPacket::class)
    val PLAY_SERVERBOUND_RESOURCE_PACK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundResourcePackPacket::class)
    val PLAY_SERVERBOUND_SEEN_ADVANCEMENTS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSeenAdvancementsPacket::class)
    val PLAY_SERVERBOUND_SELECT_TRADE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSelectTradePacket::class)
    val PLAY_SERVERBOUND_SET_BEACON_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSetBeaconPacket::class)
    val PLAY_SERVERBOUND_SET_CARRIED_ITEM_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSetCarriedItemPacket::class)
    val PLAY_SERVERBOUND_SET_COMMAND_BLOCK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSetCommandBlockPacket::class)
    val PLAY_SERVERBOUND_SET_COMMAND_MINECART_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSetCommandMinecartPacket::class)
    val PLAY_SERVERBOUND_SET_CREATIVE_MODE_SLOT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSetCreativeModeSlotPacket::class)
    val PLAY_SERVERBOUND_SET_JIGSAW_BLOCK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSetJigsawBlockPacket::class)
    val PLAY_SERVERBOUND_SET_STRUCTURE_BLOCK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSetStructureBlockPacket::class)
    val PLAY_SERVERBOUND_SIGN_UPDATE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSignUpdatePacket::class)
    val PLAY_SERVERBOUND_SWING_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundSwingPacket::class)
    val PLAY_SERVERBOUND_TELEPORT_TO_ENTITY_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundTeleportToEntityPacket::class)
    val PLAY_SERVERBOUND_USE_ITEM_ON_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundUseItemOnPacket::class)
    val PLAY_SERVERBOUND_USE_ITEM_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.SERVERBOUND, ServerboundUseItemPacket::class)
    val PLAY_BUNDLE_DELIMITER_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, BundleDelimiterPacket::class)
    val PLAY_CLIENTBOUND_ADD_ENTITY_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundAddEntityPacket::class)
    val PLAY_CLIENTBOUND_ADD_EXPERIENCE_ORB_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundAddExperienceOrbPacket::class)
    val PLAY_CLIENTBOUND_ANIMATE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundAnimatePacket::class)
    val PLAY_CLIENTBOUND_AWARD_STATS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundAwardStatsPacket::class)
    val PLAY_CLIENTBOUND_BLOCK_CHANGED_ACK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundBlockChangedAckPacket::class)
    val PLAY_CLIENTBOUND_BLOCK_DESTRUCTION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundBlockDestructionPacket::class)
    val PLAY_CLIENTBOUND_BLOCK_ENTITY_DATA_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundBlockEntityDataPacket::class)
    val PLAY_CLIENTBOUND_BLOCK_EVENT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundBlockEventPacket::class)
    val PLAY_CLIENTBOUND_BLOCK_UPDATE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundBlockUpdatePacket::class)
    val PLAY_CLIENTBOUND_BOSS_EVENT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundBossEventPacket::class)
    val PLAY_CLIENTBOUND_CHANGE_DIFFICULTY_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundChangeDifficultyPacket::class)
    val PLAY_CLIENTBOUND_CHUNKS_BIOMES_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundChunksBiomesPacket::class)
    val PLAY_CLIENTBOUND_CHUNK_BATCH_FINISHED_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundChunkBatchFinishedPacket::class)
    val PLAY_CLIENTBOUND_CHUNK_BATCH_START_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundChunkBatchStartPacket::class)
    val PLAY_CLIENTBOUND_CLEAR_TITLES_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundClearTitlesPacket::class)
    val PLAY_CLIENTBOUND_COMMANDS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundCommandsPacket::class)
    val PLAY_CLIENTBOUND_COMMAND_SUGGESTIONS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundCommandSuggestionsPacket::class)
    val PLAY_CLIENTBOUND_CONTAINER_CLOSE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundContainerClosePacket::class)
    val PLAY_CLIENTBOUND_CONTAINER_SET_CONTENT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundContainerSetContentPacket::class)
    val PLAY_CLIENTBOUND_CONTAINER_SET_DATA_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundContainerSetDataPacket::class)
    val PLAY_CLIENTBOUND_CONTAINER_SET_SLOT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundContainerSetSlotPacket::class)
    val PLAY_CLIENTBOUND_COOLDOWN_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundCooldownPacket::class)
    val PLAY_CLIENTBOUND_CUSTOM_CHAT_COMPLETIONS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundCustomChatCompletionsPacket::class)
    val PLAY_CLIENTBOUND_CUSTOM_PAYLOAD_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundCustomPayloadPacket::class)
    val PLAY_CLIENTBOUND_DAMAGE_EVENT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundDamageEventPacket::class)
    val PLAY_CLIENTBOUND_DELETE_CHAT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundDeleteChatPacket::class)
    val PLAY_CLIENTBOUND_DISCONNECT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundDisconnectPacket::class)
    val PLAY_CLIENTBOUND_DISGUISED_CHAT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundDisguisedChatPacket::class)
    val PLAY_CLIENTBOUND_ENTITY_EVENT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundEntityEventPacket::class)
    val PLAY_CLIENTBOUND_EXPLODE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundExplodePacket::class)
    val PLAY_CLIENTBOUND_FORGET_LEVEL_CHUNK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundForgetLevelChunkPacket::class)
    val PLAY_CLIENTBOUND_GAME_EVENT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundGameEventPacket::class)
    val PLAY_CLIENTBOUND_HORSE_SCREEN_OPEN_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundHorseScreenOpenPacket::class)
    val PLAY_CLIENTBOUND_HURT_ANIMATION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundHurtAnimationPacket::class)
    val PLAY_CLIENTBOUND_INITIALIZE_BORDER_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundInitializeBorderPacket::class)
    val PLAY_CLIENTBOUND_KEEP_ALIVE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundKeepAlivePacket::class)
    val PLAY_CLIENTBOUND_LEVEL_CHUNK_WITH_LIGHT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundLevelChunkWithLightPacket::class)
    val PLAY_CLIENTBOUND_LEVEL_EVENT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundLevelEventPacket::class)
    val PLAY_CLIENTBOUND_LEVEL_PARTICLES_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundLevelParticlesPacket::class)
    val PLAY_CLIENTBOUND_LIGHT_UPDATE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundLightUpdatePacket::class)
    val PLAY_CLIENTBOUND_LOGIN_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundLoginPacket::class)
    val PLAY_CLIENTBOUND_MAP_ITEM_DATA_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundMapItemDataPacket::class)
    val PLAY_CLIENTBOUND_MERCHANT_OFFERS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundMerchantOffersPacket::class)
    val PLAY_CLIENTBOUND_MOVE_ENTITY_PACKET_POS = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundMoveEntityPacket.Pos::class)
    val PLAY_CLIENTBOUND_MOVE_ENTITY_PACKET_POS_ROT = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundMoveEntityPacket.PosRot::class)
    val PLAY_CLIENTBOUND_MOVE_ENTITY_PACKET_ROT = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundMoveEntityPacket.Rot::class)
    val PLAY_CLIENTBOUND_MOVE_VEHICLE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundMoveVehiclePacket::class)
    val PLAY_CLIENTBOUND_OPEN_BOOK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundOpenBookPacket::class)
    val PLAY_CLIENTBOUND_OPEN_SCREEN_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundOpenScreenPacket::class)
    val PLAY_CLIENTBOUND_OPEN_SIGN_EDITOR_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundOpenSignEditorPacket::class)
    val PLAY_CLIENTBOUND_PING_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPingPacket::class)
    val PLAY_CLIENTBOUND_PLACE_GHOST_RECIPE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPlaceGhostRecipePacket::class)
    val PLAY_CLIENTBOUND_PLAYER_ABILITIES_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPlayerAbilitiesPacket::class)
    val PLAY_CLIENTBOUND_PLAYER_CHAT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPlayerChatPacket::class)
    val PLAY_CLIENTBOUND_PLAYER_COMBAT_END_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPlayerCombatEndPacket::class)
    val PLAY_CLIENTBOUND_PLAYER_COMBAT_ENTER_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPlayerCombatEnterPacket::class)
    val PLAY_CLIENTBOUND_PLAYER_COMBAT_KILL_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPlayerCombatKillPacket::class)
    val PLAY_CLIENTBOUND_PLAYER_INFO_REMOVE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPlayerInfoRemovePacket::class)
    val PLAY_CLIENTBOUND_PLAYER_INFO_UPDATE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPlayerInfoUpdatePacket::class)
    val PLAY_CLIENTBOUND_PLAYER_LOOK_AT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPlayerLookAtPacket::class)
    val PLAY_CLIENTBOUND_PLAYER_POSITION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPlayerPositionPacket::class)
    val PLAY_CLIENTBOUND_PONG_RESPONSE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundPongResponsePacket::class)
    val PLAY_CLIENTBOUND_RECIPE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundRecipePacket::class)
    val PLAY_CLIENTBOUND_REMOVE_ENTITIES_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundRemoveEntitiesPacket::class)
    val PLAY_CLIENTBOUND_REMOVE_MOB_EFFECT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundRemoveMobEffectPacket::class)
    val PLAY_CLIENTBOUND_RESOURCE_PACK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundResourcePackPacket::class)
    val PLAY_CLIENTBOUND_RESPAWN_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundRespawnPacket::class)
    val PLAY_CLIENTBOUND_ROTATE_HEAD_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundRotateHeadPacket::class)
    val PLAY_CLIENTBOUND_SECTION_BLOCKS_UPDATE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSectionBlocksUpdatePacket::class)
    val PLAY_CLIENTBOUND_SELECT_ADVANCEMENTS_TAB_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSelectAdvancementsTabPacket::class)
    val PLAY_CLIENTBOUND_SERVER_DATA_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundServerDataPacket::class)
    val PLAY_CLIENTBOUND_SET_ACTION_BAR_TEXT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetActionBarTextPacket::class)
    val PLAY_CLIENTBOUND_SET_BORDER_CENTER_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetBorderCenterPacket::class)
    val PLAY_CLIENTBOUND_SET_BORDER_LERP_SIZE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetBorderLerpSizePacket::class)
    val PLAY_CLIENTBOUND_SET_BORDER_SIZE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetBorderSizePacket::class)
    val PLAY_CLIENTBOUND_SET_BORDER_WARNING_DELAY_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetBorderWarningDelayPacket::class)
    val PLAY_CLIENTBOUND_SET_BORDER_WARNING_DISTANCE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetBorderWarningDistancePacket::class)
    val PLAY_CLIENTBOUND_SET_CAMERA_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetCameraPacket::class)
    val PLAY_CLIENTBOUND_SET_CARRIED_ITEM_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetCarriedItemPacket::class)
    val PLAY_CLIENTBOUND_SET_CHUNK_CACHE_CENTER_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetChunkCacheCenterPacket::class)
    val PLAY_CLIENTBOUND_SET_CHUNK_CACHE_RADIUS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetChunkCacheRadiusPacket::class)
    val PLAY_CLIENTBOUND_SET_DEFAULT_SPAWN_POSITION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetDefaultSpawnPositionPacket::class)
    val PLAY_CLIENTBOUND_SET_DISPLAY_OBJECTIVE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetDisplayObjectivePacket::class)
    val PLAY_CLIENTBOUND_SET_ENTITY_DATA_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetEntityDataPacket::class)
    val PLAY_CLIENTBOUND_SET_ENTITY_LINK_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetEntityLinkPacket::class)
    val PLAY_CLIENTBOUND_SET_ENTITY_MOTION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetEntityMotionPacket::class)
    val PLAY_CLIENTBOUND_SET_EQUIPMENT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetEquipmentPacket::class)
    val PLAY_CLIENTBOUND_SET_EXPERIENCE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetExperiencePacket::class)
    val PLAY_CLIENTBOUND_SET_HEALTH_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetHealthPacket::class)
    val PLAY_CLIENTBOUND_SET_OBJECTIVE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetObjectivePacket::class)
    val PLAY_CLIENTBOUND_SET_PASSENGERS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetPassengersPacket::class)
    val PLAY_CLIENTBOUND_SET_PLAYER_TEAM_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetPlayerTeamPacket::class)
    val PLAY_CLIENTBOUND_SET_SCORE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetScorePacket::class)
    val PLAY_CLIENTBOUND_SET_SIMULATION_DISTANCE_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetSimulationDistancePacket::class)
    val PLAY_CLIENTBOUND_SET_SUBTITLE_TEXT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetSubtitleTextPacket::class)
    val PLAY_CLIENTBOUND_SET_TIME_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetTimePacket::class)
    val PLAY_CLIENTBOUND_SET_TITLES_ANIMATION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetTitlesAnimationPacket::class)
    val PLAY_CLIENTBOUND_SET_TITLE_TEXT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSetTitleTextPacket::class)
    val PLAY_CLIENTBOUND_SOUND_ENTITY_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSoundEntityPacket::class)
    val PLAY_CLIENTBOUND_SOUND_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSoundPacket::class)
    val PLAY_CLIENTBOUND_START_CONFIGURATION_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundStartConfigurationPacket::class)
    val PLAY_CLIENTBOUND_STOP_SOUND_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundStopSoundPacket::class)
    val PLAY_CLIENTBOUND_SYSTEM_CHAT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundSystemChatPacket::class)
    val PLAY_CLIENTBOUND_TAB_LIST_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundTabListPacket::class)
    val PLAY_CLIENTBOUND_TAG_QUERY_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundTagQueryPacket::class)
    val PLAY_CLIENTBOUND_TAKE_ITEM_ENTITY_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundTakeItemEntityPacket::class)
    val PLAY_CLIENTBOUND_TELEPORT_ENTITY_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundTeleportEntityPacket::class)
    val PLAY_CLIENTBOUND_UPDATE_ADVANCEMENTS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundUpdateAdvancementsPacket::class)
    val PLAY_CLIENTBOUND_UPDATE_ATTRIBUTES_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundUpdateAttributesPacket::class)
    val PLAY_CLIENTBOUND_UPDATE_MOB_EFFECT_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundUpdateMobEffectPacket::class)
    val PLAY_CLIENTBOUND_UPDATE_RECIPES_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundUpdateRecipesPacket::class)
    val PLAY_CLIENTBOUND_UPDATE_TAGS_PACKET = getPacketId(ConnectionProtocol.PLAY, PacketFlow.CLIENTBOUND, ClientboundUpdateTagsPacket::class)
    val STATUS_SERVERBOUND_PING_REQUEST_PACKET = getPacketId(ConnectionProtocol.STATUS, PacketFlow.SERVERBOUND, ServerboundPingRequestPacket::class)
    val STATUS_SERVERBOUND_STATUS_REQUEST_PACKET = getPacketId(ConnectionProtocol.STATUS, PacketFlow.SERVERBOUND, ServerboundStatusRequestPacket::class)
    val STATUS_CLIENTBOUND_PONG_RESPONSE_PACKET = getPacketId(ConnectionProtocol.STATUS, PacketFlow.CLIENTBOUND, ClientboundPongResponsePacket::class)
    val STATUS_CLIENTBOUND_STATUS_RESPONSE_PACKET = getPacketId(ConnectionProtocol.STATUS, PacketFlow.CLIENTBOUND, ClientboundStatusResponsePacket::class)
    val LOGIN_SERVERBOUND_CUSTOM_QUERY_ANSWER_PACKET = getPacketId(ConnectionProtocol.LOGIN, PacketFlow.SERVERBOUND, ServerboundCustomQueryAnswerPacket::class)
    val LOGIN_SERVERBOUND_HELLO_PACKET = getPacketId(ConnectionProtocol.LOGIN, PacketFlow.SERVERBOUND, ServerboundHelloPacket::class)
    val LOGIN_SERVERBOUND_KEY_PACKET = getPacketId(ConnectionProtocol.LOGIN, PacketFlow.SERVERBOUND, ServerboundKeyPacket::class)
    val LOGIN_SERVERBOUND_LOGIN_ACKNOWLEDGED_PACKET = getPacketId(ConnectionProtocol.LOGIN, PacketFlow.SERVERBOUND, ServerboundLoginAcknowledgedPacket::class)
    val LOGIN_CLIENTBOUND_CUSTOM_QUERY_PACKET = getPacketId(ConnectionProtocol.LOGIN, PacketFlow.CLIENTBOUND, ClientboundCustomQueryPacket::class)
    val LOGIN_CLIENTBOUND_GAME_PROFILE_PACKET = getPacketId(ConnectionProtocol.LOGIN, PacketFlow.CLIENTBOUND, ClientboundGameProfilePacket::class)
    val LOGIN_CLIENTBOUND_HELLO_PACKET = getPacketId(ConnectionProtocol.LOGIN, PacketFlow.CLIENTBOUND, ClientboundHelloPacket::class)
    val LOGIN_CLIENTBOUND_LOGIN_COMPRESSION_PACKET = getPacketId(ConnectionProtocol.LOGIN, PacketFlow.CLIENTBOUND, ClientboundLoginCompressionPacket::class)
    val LOGIN_CLIENTBOUND_LOGIN_DISCONNECT_PACKET = getPacketId(ConnectionProtocol.LOGIN, PacketFlow.CLIENTBOUND, ClientboundLoginDisconnectPacket::class)
    val CONFIGURATION_SERVERBOUND_CLIENT_INFORMATION_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.SERVERBOUND, ServerboundClientInformationPacket::class)
    val CONFIGURATION_SERVERBOUND_CUSTOM_PAYLOAD_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.SERVERBOUND, ServerboundCustomPayloadPacket::class)
    val CONFIGURATION_SERVERBOUND_FINISH_CONFIGURATION_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.SERVERBOUND, ServerboundFinishConfigurationPacket::class)
    val CONFIGURATION_SERVERBOUND_KEEP_ALIVE_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.SERVERBOUND, ServerboundKeepAlivePacket::class)
    val CONFIGURATION_SERVERBOUND_PONG_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.SERVERBOUND, ServerboundPongPacket::class)
    val CONFIGURATION_SERVERBOUND_RESOURCE_PACK_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.SERVERBOUND, ServerboundResourcePackPacket::class)
    val CONFIGURATION_CLIENTBOUND_CUSTOM_PAYLOAD_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.CLIENTBOUND, ClientboundCustomPayloadPacket::class)
    val CONFIGURATION_CLIENTBOUND_DISCONNECT_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.CLIENTBOUND, ClientboundDisconnectPacket::class)
    val CONFIGURATION_CLIENTBOUND_FINISH_CONFIGURATION_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.CLIENTBOUND, ClientboundFinishConfigurationPacket::class)
    val CONFIGURATION_CLIENTBOUND_KEEP_ALIVE_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.CLIENTBOUND, ClientboundKeepAlivePacket::class)
    val CONFIGURATION_CLIENTBOUND_PING_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.CLIENTBOUND, ClientboundPingPacket::class)
    val CONFIGURATION_CLIENTBOUND_REGISTRY_DATA_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.CLIENTBOUND, ClientboundRegistryDataPacket::class)
    val CONFIGURATION_CLIENTBOUND_RESOURCE_PACK_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.CLIENTBOUND, ClientboundResourcePackPacket::class)
    val CONFIGURATION_CLIENTBOUND_UPDATE_ENABLED_FEATURES_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.CLIENTBOUND, ClientboundUpdateEnabledFeaturesPacket::class)
    val CONFIGURATION_CLIENTBOUND_UPDATE_TAGS_PACKET = getPacketId(ConnectionProtocol.CONFIGURATION, PacketFlow.CLIENTBOUND, ClientboundUpdateTagsPacket::class)
    
    private fun getPacketId(protocol: ConnectionProtocol, flow: PacketFlow, packet: KClass<*>): Int {
        val packetSet = ReflectionRegistry.CODEC_DATA_PACKET_SET_FIELD.get(protocol.codec(flow))
        return ReflectionRegistry.PACKET_SET_GET_ID_METHOD.invoke(packetSet, packet.java) as Int
    }
    
}