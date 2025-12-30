package xyz.xenondevs.nova.mixin.item.consistentinteractloop;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerGamePacketListenerImpl.EntityInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.util.MixinContexts;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

@Mixin(targets = "net.minecraft.server.network.ServerGamePacketListenerImpl$1")
abstract class ServerGamePacketListenerHandleInteractAnonymousHandlerMixin {
    
    // reference to field added in ServerGamePacketListenerImplMixin
    @Unique
    private static final VarHandle NOVA_STOP_INTERACT_LOOP_REFERENCE;
    
    static {
        try {
            //noinspection JavaLangInvokeHandleSignature
            NOVA_STOP_INTERACT_LOOP_REFERENCE = MethodHandles
                .privateLookupIn(ServerGamePacketListenerImpl.class, MethodHandles.lookup())
                .findVarHandle(ServerGamePacketListenerImpl.class, "nova$stopInteractLoop", boolean.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Shadow
    @Final
    ServerGamePacketListenerImpl this$0;
    
    @Redirect(
        method = "performInteraction",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/network/ServerGamePacketListenerImpl$EntityInteraction;run(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;"
        )
    )
    private InteractionResult run(
        EntityInteraction entityInteraction, 
        ServerPlayer player, 
        Entity entity, 
        InteractionHand hand
    ) {
        var result = entityInteraction.run(player, entity, hand);
        if (!(result instanceof InteractionResult.Pass)) {
            NOVA_STOP_INTERACT_LOOP_REFERENCE.set(this$0, true);
        }
        return null;
    }
    
    @Inject(
        method = "onInteraction(Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)V",
        at = @At("HEAD")
    )
    private void rememberInteractLocation(InteractionHand hand, Vec3 interactionLocation, CallbackInfo ci) {
        MixinContexts.INTERACT_LOCATION.set(interactionLocation);
    }
    
}
