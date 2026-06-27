package xyz.xenondevs.nova.packetentity

import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.animal.cow.MushroomCow
import net.minecraft.world.entity.animal.parrot.Parrot
import net.minecraft.world.entity.animal.rabbit.Rabbit
import net.minecraft.world.entity.npc.villager.Villager
import net.minecraft.world.entity.npc.villager.VillagerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

internal object DefaultValues {

    fun <T : Any> holderDefault(registryKey: ResourceKey<Registry<T>>): Holder<T> =
        MinecraftServer.getServer().registryAccess().lookupOrThrow(registryKey).getAny().orElseThrow()

    val ENTITY_AIR_SUPPLY: Int = 300
    val ABSTRACT_MINECART_DISPLAY_OFFSET: Int = 6
    val EYE_OF_ENDER_ITEM_STACK: ItemStack = ItemStack(Items.ENDER_EYE)
    val FIREBALL_ITEM_STACK: ItemStack = ItemStack(Items.FIRE_CHARGE)
    val THROWABLE_ITEM_PROJECTILE_ITEM_STACK: ItemStack = ItemStack.EMPTY
    val ZOMBIE_VILLAGER_VILLAGER_DATA: VillagerData = Villager.createDefaultVillagerData()
    val MUSHROOM_COW_TYPE: Int = MushroomCow.Variant.DEFAULT.id
    val PARROT_VARIANT: Int = Parrot.Variant.DEFAULT.id
    val RABBIT_TYPE: Int = Rabbit.Variant.DEFAULT.id
    
}
