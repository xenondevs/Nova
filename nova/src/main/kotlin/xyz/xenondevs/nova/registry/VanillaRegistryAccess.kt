package xyz.xenondevs.nova.registry

import net.minecraft.core.RegistryAccess
import xyz.xenondevs.nova.util.minecraftServer

object VanillaRegistryAccess : RegistryAccess by minecraftServer.registryAccess()