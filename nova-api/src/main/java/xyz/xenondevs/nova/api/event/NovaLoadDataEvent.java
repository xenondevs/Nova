package xyz.xenondevs.nova.api.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NovaLoadDataEvent extends Event {
    
    private static final @NotNull HandlerList HANDLERS = new HandlerList();
    
    public NovaLoadDataEvent() {
        super(true);
    }
    
    public static @NotNull HandlerList getHandlerList() {
        return HANDLERS;
    }
    
    @Override
    public @NotNull HandlerList getHandlers() {
        return HANDLERS;
    }
    
}