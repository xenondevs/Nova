package xyz.xenondevs.nova.mixin.event.prevention;

import com.destroystokyo.paper.event.entity.EntityKnockbackByEntityEvent;
import io.papermc.paper.event.entity.EntityPushedByEntityAttackEvent;
import io.papermc.paper.plugin.manager.PaperPluginManagerImpl;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.xenondevs.nova.util.EventUtils;
import xyz.xenondevs.nova.util.FakePlayer;

@Mixin(PaperPluginManagerImpl.class)
abstract class PaperPluginManagerImplMixin {
    
    @SuppressWarnings({"ConstantValue", "removal"}) // some plugins might set the player to null
    @Inject(method = "callEvent", at = @At("HEAD"), cancellable = true)
    private void callEvent(Event event, CallbackInfo ci) {
        if (event instanceof PlayerEvent playerEvent) {
            var player = playerEvent.getPlayer();
            if (player != null && ((CraftPlayer) player).getHandle() instanceof FakePlayer fp && !fp.getHasEvents())
                ci.cancel();
        }
        
        if (event instanceof EntityEvent entityEvent) {
            var entity = entityEvent.getEntity();
            if (entity != null && ((CraftEntity) entity).getHandle() instanceof FakePlayer fp && !fp.getHasEvents())
                ci.cancel();
        }
        
        if (event instanceof EntityDamageByEntityEvent damageEvent) {
            var damager = damageEvent.getDamager();
            if (damager != null && ((CraftEntity) damager).getHandle() instanceof FakePlayer fp && !fp.getHasEvents())
                ci.cancel();
        }
        
        if (event instanceof EntityKnockbackByEntityEvent knockbackEvent) {
            var entity = knockbackEvent.getPushedBy();
            if (entity != null && ((CraftEntity) entity).getHandle() instanceof FakePlayer fp && !fp.getHasEvents())
                ci.cancel();
        }
        
        if (event instanceof org.bukkit.event.entity.EntityKnockbackByEntityEvent knockbackEvent) {
            var entity = knockbackEvent.getSourceEntity();
            if (entity != null && ((CraftEntity) entity).getHandle() instanceof FakePlayer fp && !fp.getHasEvents())
                ci.cancel();
        }
        
        if (event instanceof EntityCombustByEntityEvent combustEvent) {
            var entity = combustEvent.getCombuster();
            if (entity != null && ((CraftEntity) entity).getHandle() instanceof FakePlayer fp && !fp.getHasEvents())
                ci.cancel();
        }
        
        if (event instanceof EntityPushedByEntityAttackEvent pushEvent) {
            var entity = pushEvent.getPushedBy();
            if (entity != null && ((CraftEntity) entity).getHandle() instanceof FakePlayer fp && !fp.getHasEvents())
                ci.cancel();
        }
        
        if (event instanceof HangingBreakByEntityEvent breakEvent) {
            var entity = breakEvent.getRemover();
            if (entity != null && ((CraftEntity) entity).getHandle() instanceof FakePlayer fp && !fp.getHasEvents())
                ci.cancel();
        }
        
        if (EventUtils.dropAllEvents.get())
            ci.cancel();
    }
    
}
