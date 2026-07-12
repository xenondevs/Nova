package xyz.xenondevs.nova.mixin.block.rewrite;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

// excludes nova tile entities from the ClientboundLevelChunkPacketData packet
@Mixin(ClientboundLevelChunkPacketData.class)
abstract class ClientboundLevelChunkPacketDataMixin {
    
    @SuppressWarnings("deprecation")
    @Definition(id = "add", method = "Ljava/util/List;add(Ljava/lang/Object;)Z")
    @Definition(id = "blockEntitiesData", field = "Lnet/minecraft/network/protocol/game/ClientboundLevelChunkPacketData;blockEntitiesData:Ljava/util/List;")
    @Expression("this.blockEntitiesData.add(?)")
    @Redirect(
        method = "<init>(Lnet/minecraft/world/level/chunk/LevelChunk;Lio/papermc/paper/antixray/ChunkPacketInfo;)V",
        at = @At("MIXINEXTRAS:EXPRESSION")
    )
    private boolean addIfVanillaTileEntity(List<Object> instance, Object e) {
        if (!(e instanceof ClientboundLevelChunkPacketData.BlockEntityInfo info))
            return true;
        
        if (info.type.builtInRegistryHolder().key().identifier().getNamespace().equals("minecraft"))
            instance.add(e);
        
        return true;
    }
    
}
