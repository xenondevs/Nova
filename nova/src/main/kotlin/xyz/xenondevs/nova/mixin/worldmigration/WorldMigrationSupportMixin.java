package xyz.xenondevs.nova.mixin.worldmigration;

import io.papermc.paper.world.migration.WorldMigrationSupport;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(WorldMigrationSupport.class)
abstract class WorldMigrationSupportMixin {
    
    @Redirect(
        method = "migrateDimensionDirectories",
        at = @At(
            value = "FIELD",
            target = "Lio/papermc/paper/world/migration/WorldMigrationSupport;DIMENSION_DIRECTORIES:Ljava/util/List;",
            opcode = Opcodes.GETSTATIC
        )
    )
    private static List<String> includeNovaDimensionDirectories() {
        var dirs = new ArrayList<>(WorldMigrationSupport.DIMENSION_DIRECTORIES);
        dirs.add("nova_region");
        dirs.add("nova_network_region");
        return dirs;
    }
    
}
