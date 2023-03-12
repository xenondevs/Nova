package xyz.xenondevs.nova.ui.overlay.character;

import net.kyori.adventure.text.BuildableComponent;

class MovedFontsRawTypes {
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static BuildableComponent moveVertically(BuildableComponent component, int distance, boolean addDistance) {
        // Kotlin does not have raw types
        return MovedFonts.INSTANCE.moveVerticallyInternal$nova(component, distance, addDistance);
    }
    
}
