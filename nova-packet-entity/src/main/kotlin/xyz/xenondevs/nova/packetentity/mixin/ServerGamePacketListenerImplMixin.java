package xyz.xenondevs.nova.packetentity.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import xyz.xenondevs.nova.packetentity.PacketEntityManager;
import xyz.xenondevs.nova.world.InteractionResult;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1100)
abstract class ServerGamePacketListenerImplMixin {
    
    @Shadow
    public ServerPlayer player;
    
    @Shadow
    @Dynamic("Added by consistentuseloop.ServerGamePacketListenerImplMixin")
    private boolean nova$stopClickLoop;
    
    @Redirect(
        method = "handleInteract",
        at = @At(
            value = "INVOKE",
            target = "Lorg/bukkit/craftbukkit/event/CraftEventFactory;callPlayerUseUnknownEntityEvent(Lnet/minecraft/world/entity/player/Player;IZLnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void handlePacketEntityInteract(
        net.minecraft.world.entity.player.Player player,
        int entityId,
        boolean attack,
        InteractionHand hand,
        Vec3 vector
    ) {
        var result = PacketEntityManager.handleInteract(this.player.getBukkitEntity(), entityId, hand, vector);
        if (result == null) {
            CraftEventFactory.callPlayerUseUnknownEntityEvent(player, entityId, attack, hand, vector);
        } else if (!(result instanceof InteractionResult.Pass)) {
            nova$stopClickLoop = true;
        }
    }
    
    @Redirect(
        method = "handleAttack",
        at = @At(
            value = "INVOKE",
            target = "Lorg/bukkit/craftbukkit/event/CraftEventFactory;callPlayerUseUnknownEntityEvent(Lnet/minecraft/world/entity/player/Player;IZLnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void handlePacketEntityAttack(
        net.minecraft.world.entity.player.Player player,
        int entityId,
        boolean attack,
        InteractionHand hand,
        Vec3 vector
    ) {
        if (!PacketEntityManager.handleAttack(this.player.getBukkitEntity(), entityId))
            CraftEventFactory.callPlayerUseUnknownEntityEvent(player, entityId, attack, hand, vector);
    }
    
}
