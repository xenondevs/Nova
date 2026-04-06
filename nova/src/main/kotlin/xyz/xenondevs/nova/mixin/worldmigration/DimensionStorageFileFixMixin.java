package xyz.xenondevs.nova.mixin.worldmigration;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.util.filefix.fixes.DimensionStorageFileFix;
import net.minecraft.util.filefix.operations.FileFixOperations;
import net.minecraft.util.filefix.operations.Move;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

@Mixin(DimensionStorageFileFix.class)
abstract class DimensionStorageFileFixMixin {
    
    // List.of(FileFixOperations.moveSimple("region"), FileFixOperations.moveSimple("entities"), FileFixOperations.moveSimple("poi")
    // -> List.of(FileFixOperations.moveSimple("region"), FileFixOperations.moveSimple("entities"), FileFixOperations.moveSimple("poi"), FileFixOperations.moveSimple("nova_region"), FileFixOperations.moveSimple("nova_network_region"))
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Definition(id = "of", method = "Ljava/util/List;of(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;")
    @Expression("of(?, ?, ?)")
    @ModifyExpressionValue(
        method = "makeFixer",
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private <E> List<E> x(List<E> original) {
        if (original.getFirst() instanceof Move m && m.from().equals("region")) {
            var list = new ArrayList(original);
            list.add(FileFixOperations.moveSimple("nova_region"));
            list.add(FileFixOperations.moveSimple("nova_network_region"));
            return list;
        } else {
            return original;
        }
    }
    
}
