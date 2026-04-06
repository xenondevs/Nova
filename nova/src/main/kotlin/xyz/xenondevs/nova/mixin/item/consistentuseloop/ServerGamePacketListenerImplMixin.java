package xyz.xenondevs.nova.mixin.item.consistentuseloop;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.util.MixinContext;

/// This mixin is intended to establish a consistent right-click loop,
/// independent of client-side predictions.
///
/// Normally, the client goes through the following cases when right-clicking on a block:
/// 1. use on block (main-hand)
/// 2. use item (main-hand), only if non-empty
/// 3. use block (off-hand)
/// 4. use item (off-hand), only if non-empty
///
/// or when right-clicking an entity:
/// 1. use on entity (main-hand)
/// 2. use item (main-hand), only if non-empty
/// 3. use on entity (off-hand)
/// 4. use item (off-hand), only if non-empty
///
/// or when right-clicking in the air:
/// 1. use item (main-hand), only if non-empty
/// 2. use item (off-hand), only if non-empty
///
/// Each of these steps is sent as a separate packet to the server. However, if the client predicts an action
/// to occur (i.e. InteractionResult.Success or InteractionResult.Fail), it will not send the subsequent packets.
/// This then leads to stuff like PlayerInteractEvent not being fired for the off-hand, even though the main-hand
/// event was canceled or PlayerInteractEvent being fired for the off-hand even though an action was performed with
/// the main hand, but the client did not predict it.
///
/// This mixin changes the packet listener to drop these subsequent packets and instead only listen for the initial one.
/// Then, it will re-inject the subsequent packets from the server side, depending on the actual outcome of the interaction.
///
/// This is done the following way:
///
/// Relevant incoming packets are filtered in a "rememberClickInitiationOrSkip" phase.
/// If it is the first packet of this click loop, it is remembered and processed normally. If the outcome is
/// InteractionResult.Pass, the next packet is injected in the "maybeContinueClickLoop" phase.
/// All other incoming packets from the client are discarded until the client tick ends, after which the next click
/// loop packet will be interpreted as the new click loop initiation packet again.
///
@Mixin(ServerGamePacketListenerImpl.class)
abstract class ServerGamePacketListenerImplMixin {
    
    @Shadow
    public ServerPlayer player;
    
    @Unique
    private Packet<?> nova$clickInitiationPacket = null;
    
    @Unique
    private boolean nova$stopClickLoop = false;
    
    @Unique
    private int nova$isInClickLoop = 0;
    
    //<editor-fold desc="use item on block">
    @Inject(
        method = "handleUseItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;ackBlockChangesUpTo(I)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void rememberClickInitiationOrSkip(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (nova$clickInitiationPacket == null) {
            // initial packet, click on block
            nova$clickInitiationPacket = packet;
        } else if (nova$isInClickLoop <= 0) {
            // not in click loop -> secondary packet from client, skip
            ci.cancel();
        }
        // else: click loop injected packet, continue processing
    }
    
    @Redirect(
        method = "handleUseItemOn",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItemOn(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult useItemOn(
        ServerPlayerGameMode gameMode,
        ServerPlayer player,
        Level level,
        ItemStack itemInHand,
        InteractionHand hand,
        BlockHitResult hitResult
    ) {
        var result = gameMode.useItemOn(player, level, itemInHand, hand, hitResult);
        if (!(result instanceof InteractionResult.Pass))
            nova$stopClickLoop = true;
        return result;
    }
    
    @Inject(method = "handleUseItemOn", at = @At("RETURN"))
    private void maybeContinueClickLoop(ServerboundUseItemOnPacket packet, CallbackInfo ci) {
        if (nova$stopClickLoop)
            return;
        try {
            nova$isInClickLoop++;
            
            var listener = ((ServerGamePacketListener) this);
            var useItemPacket = new ServerboundUseItemPacket(packet.getHand(), packet.getSequence(), player.getYRot(), player.getXRot());
            listener.handleUseItem(useItemPacket);
        } finally {
            nova$isInClickLoop--;
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="use item on entity">
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
        if (nova$clickInitiationPacket == null) {
            // initial packet
            nova$clickInitiationPacket = packet;
        } else if (nova$isInClickLoop <= 0) {
            // not in interact loop -> secondary packet from client, skip
            ci.cancel();
        }
        // else: interact loop injected packet, continue processing
    }
    
    @Redirect(
        method = "handleInteract",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayer;interactOn(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult interactOn(
        ServerPlayer player,
        Entity entity,
        InteractionHand hand,
        Vec3 loc,
        @Local(argsOnly = true) ServerboundInteractPacket packet
    ) {
        var result = ScopedValue
            .where(MixinContext.IS_USING_SECONDARY_ACTION, packet.usingSecondaryAction())
            .call(() -> player.interactOn(entity, hand, loc));
        if (!(result instanceof InteractionResult.Pass))
            nova$stopClickLoop = true;
        return result;
    }
    
    @Inject(
        method = "handleInteract",
        at = @At(value = "RETURN")
    )
    private void maybeContinueInteractLoop(ServerboundInteractPacket packet, CallbackInfo ci) {
        // TODO: prevent desyncs
        if (nova$stopClickLoop)
            return;
        
        try {
            nova$isInClickLoop++;
            
            var nextPacket = new ServerboundUseItemPacket(
                packet.hand(),
                0, // predicted block changes are acked by skipped packet
                player.getYRot(),
                player.getXRot()
            );
            
            ((ServerGamePacketListener) this).handleUseItem(nextPacket);
        } finally {
            nova$isInClickLoop--;
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="use item">
    @Inject(
        method = "handleUseItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl;ackBlockChangesUpTo(I)V",
            shift = At.Shift.AFTER
        ),
        cancellable = true
    )
    private void rememberClickInitiationOrSkip(ServerboundUseItemPacket packet, CallbackInfo ci) {
        if (nova$clickInitiationPacket == null) {
            // initial packet, click in air
            nova$clickInitiationPacket = packet;
        } else if (nova$isInClickLoop <= 0) {
            // not in click loop -> secondary packet from client, skip
            ci.cancel();
            
            // but still update player rotation with new info (probably not that important)
            var xRot = Mth.wrapDegrees(packet.getXRot());
            var yRot = Mth.wrapDegrees(packet.getYRot());
            if (xRot != player.getXRot() || yRot != player.getYRot())
                player.absSnapRotationTo(yRot, xRot);
        }
        // else: click loop injected packet, continue processing
    }
    
    @Redirect(
        method = "handleUseItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerPlayerGameMode;useItem(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult useItem(
        ServerPlayerGameMode gameMode,
        ServerPlayer player,
        Level level,
        ItemStack itemInHand,
        InteractionHand hand
    ) {
        var result = gameMode.useItem(player, level, itemInHand, hand);
        // since UseItem for off-hand is handled last, only continue the click loop if main-hand returned Pass
        if (!(result instanceof InteractionResult.Pass) || hand == InteractionHand.OFF_HAND)
            nova$stopClickLoop = true;
        return result;
    }
    
    @Inject(method = "handleUseItem", at = @At("RETURN"))
    private void maybeContinueClickLoop(ServerboundUseItemPacket packet, CallbackInfo ci) {
        // use item for off-hand is the last in the click loop
        if (nova$stopClickLoop || packet.getHand() == InteractionHand.OFF_HAND)
            return;
        try {
            nova$isInClickLoop++;
            
            var listener = ((ServerGamePacketListener) this);
            if (nova$clickInitiationPacket instanceof ServerboundUseItemOnPacket originalPacket) {
                var nextPacket = new ServerboundUseItemOnPacket(
                    InteractionHand.OFF_HAND,
                    originalPacket.getHitResult(),
                    originalPacket.getSequence()
                );
                listener.handleUseItemOn(nextPacket);
            } else if (nova$clickInitiationPacket instanceof ServerboundInteractPacket originalPacket) {
                var nextPacket = new ServerboundInteractPacket(
                    originalPacket.entityId(),
                    InteractionHand.OFF_HAND,
                    originalPacket.location(),
                    originalPacket.usingSecondaryAction()
                );
                listener.handleInteract(nextPacket);
            } else {
                var nextPacket = new ServerboundUseItemPacket(
                    InteractionHand.OFF_HAND,
                    packet.getSequence(),
                    packet.getYRot(),
                    packet.getXRot()
                );
                listener.handleUseItem(nextPacket);
            }
        } finally {
            nova$isInClickLoop--;
        }
    }
    //</editor-fold>
    
    @Inject(
        method = "handleClientTickEnd",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/server/level/ServerLevel;)V",
            shift = At.Shift.AFTER
        )
    )
    private void clear(ServerboundClientTickEndPacket packet, CallbackInfo ci) {
        nova$clickInitiationPacket = null;
        nova$stopClickLoop = false;
        nova$isInClickLoop = 0;
    }
    
}
