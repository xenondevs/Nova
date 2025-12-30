package xyz.xenondevs.nova.mixin.item.consistentinteractloop;

import net.minecraft.network.protocol.game.ServerGamePacketListener;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket.InteractionAction;
import net.minecraft.network.protocol.game.ServerboundInteractPacket.InteractionAtLocationAction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/// This mixin is intended to establish a consistent interact (right-click entity) loop,
/// independent of client-side predictions.
///
/// Normally, the client goes through the following cases when right-clicking an entity:
/// 1. interact at location (main-hand)
/// 2. interact (main-hand)
/// 3. interact at location (off-hand)
/// 4. interact (off-hand)
///
/// For attack, the client only sends one ATTACK action.
///
/// Each of these steps is sent as a separate packet to the server. However, if the client predicts an action
/// to occur (i.e. InteractionResult.Success or InteractionResult.Fail), it will not send the subsequent packets.
/// This then leads to stuff like PlayerInteractEntityEvent not being fired for the off-hand, even though the main-hand
/// event was canceled or PlayerInteractEntityEvent being fired for the off-hand even though an action was performed with
/// the main hand, but the client did not predict it.
///
/// This mixin changes the packet listener to drop these subsequent packets and instead only listen for the initial one.
/// Then, it will re-inject the subsequent packets from the server side, depending on the actual outcome of the interaction.
///
/// @see xyz.xenondevs.nova.mixin.item.consistentuseloop
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {
    
    @Shadow
    public ServerPlayer player;
    
    @Unique
    private ServerboundInteractPacket nova$interactInitiationPacket = null;
    
    @Unique
    private boolean nova$stopInteractLoop = false;
    
    @Unique
    private int nova$isInInteractLoop = 0;
    
    @Inject(
        method = "handleInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void rememberInteractInitiationOrSkip(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (nova$interactInitiationPacket == null) {
            // initial packet
            nova$interactInitiationPacket = packet;
        } else if (nova$isInInteractLoop <= 0) {
            // not in interact loop -> secondary packet from client, skip
            ci.cancel();
        }
        // else: interact loop injected packet, continue processing
    }
    
    @Inject(
        method = "handleInteract",
        at = @At(value = "RETURN")
    )
    private void maybeContinueInteractLoop(ServerboundInteractPacket packet, CallbackInfo ci) {
        if (nova$stopInteractLoop
            || packet.action == ServerboundInteractPacket.ATTACK_ACTION
            || (packet.action instanceof InteractionAction ia && ia.hand == InteractionHand.OFF_HAND)
        ) {
            // prevent desyncs due to unpredicted inventory changes
            player.inventoryMenu.sendAllDataToRemote();
            return;
        }
        
        try {
            nova$isInInteractLoop++;
            
            ServerboundInteractPacket nextPacket = null;
            var action = packet.action;
            if (action instanceof InteractionAtLocationAction iAction) {
                // InteractAtLocation -> Interact
                nextPacket = new ServerboundInteractPacket(
                    packet.getEntityId(),
                    packet.isUsingSecondaryAction(),
                    new InteractionAction(iAction.hand)
                );
            } else if (action instanceof InteractionAction iAction && iAction.hand == InteractionHand.MAIN_HAND) {
                // Interact (main-hand) -> InteractAtLocation (off-hand)
                Vec3 originalLocation = Vec3.ZERO;
                if (nova$interactInitiationPacket.action instanceof InteractionAtLocationAction oiAction) {
                    originalLocation = oiAction.location;
                }
                nextPacket = new ServerboundInteractPacket(
                    packet.getEntityId(),
                    packet.isUsingSecondaryAction(),
                    new InteractionAtLocationAction(InteractionHand.OFF_HAND, originalLocation)
                );
            }
            
            if (nextPacket != null) {
                ((ServerGamePacketListener) this).handleInteract(nextPacket);
            }
        } finally {
            nova$isInInteractLoop--;
        }
    }
    
    @Inject(
        method = "handleClientTickEnd",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER
        )
    )
    private void clear(ServerboundClientTickEndPacket packet, CallbackInfo ci) {
        nova$interactInitiationPacket = null;
        nova$stopInteractLoop = false;
        nova$isInInteractLoop = 0;
    }
    
}
