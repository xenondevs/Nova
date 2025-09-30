package xyz.xenondevs.nova.world.item.behavior

import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import org.bukkit.Bukkit
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Horse
import org.bukkit.entity.HumanEntity
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Llama
import org.bukkit.entity.Piglin
import org.bukkit.entity.Player
import org.bukkit.entity.Skeleton
import org.bukkit.entity.Wolf
import org.bukkit.entity.Zombie
import xyz.xenondevs.commons.collections.concurrentHashSet
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.ClientboundSetEquipmentPacket
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.nmsEntity
import xyz.xenondevs.nova.util.nmsEquipmentSlot
import xyz.xenondevs.nova.util.runTaskTimer
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.unwrap
import org.bukkit.inventory.EquipmentSlot as BukkitEquipmentSlot

@InternalInit(stage = InternalInitStage.POST_WORLD)
internal object EquipmentAnimator {
    
    private val ARMOR_EQUIPMENT_SLOTS = listOf(
        BukkitEquipmentSlot.FEET,
        BukkitEquipmentSlot.LEGS,
        BukkitEquipmentSlot.CHEST,
        BukkitEquipmentSlot.HEAD,
        BukkitEquipmentSlot.BODY
    )
    
    private val _tick = mutableProvider(0)
    val tick: Provider<Int> get() = _tick
    
    val animatedBehaviors: MutableSet<ItemBehavior> = concurrentHashSet()
    
    @InitFun
    private fun startAnimationTask() {
        runTaskTimer(0, 1, ::handleTick)
    }
    
    private fun handleTick() {
        if (animatedBehaviors.isEmpty())
            return
        
        _tick.set(_tick.get() + 1)
        
        Bukkit.getWorlds().asSequence()
            .flatMap { it.livingEntities }
            .forEach { entity ->
                when (entity) {
                    is Player -> updatePlayerArmor(entity)
                    
                    is HumanEntity, is Zombie, is Skeleton, is Piglin,
                    is ArmorStand, is Horse, is Llama, is Wolf -> updateNonPlayerArmor(entity)
                }
            }
    }
    
    private fun updatePlayerArmor(player: Player) {
        val serverPlayer = player.serverPlayer
        val updatedEquipment = HashMap<EquipmentSlot, ItemStack>()
        for ((armorSlot, armorStack) in player.equipment.armorContents.withIndex()) {
            if (armorStack?.novaItem?.behaviors?.any { it in animatedBehaviors } == true) {
                serverPlayer.inventoryMenu.setRemoteSlot(8 - armorSlot, ItemStack.EMPTY) // mark as dirty, force update
                updatedEquipment[EquipmentSlot.entries[armorSlot + 2]] = armorStack.unwrap()
            }
        }
        
        if (updatedEquipment.isNotEmpty()) {
            // update for other players
            val packet = ClientboundSetEquipmentPacket(player.entityId, updatedEquipment)
            serverPlayer.level().chunkSource.sendToTrackingPlayers(serverPlayer, packet)
        }
    }
    
    private fun updateNonPlayerArmor(entity: LivingEntity) {
        val equipment = entity.equipment ?: return
        
        val updatedEquipment = HashMap<EquipmentSlot, ItemStack>()
        for (slot in ARMOR_EQUIPMENT_SLOTS) {
            if (!entity.canUseEquipmentSlot(slot))
                continue
            
            val itemStack = equipment.getItem(slot)
            if (itemStack.novaItem?.behaviors?.any { it in animatedBehaviors } == true) {
                updatedEquipment[slot.nmsEquipmentSlot] = itemStack.unwrap()
            }
        }
        
        if (updatedEquipment.isNotEmpty()) {
            val packet = ClientboundSetEquipmentPacket(entity.entityId, updatedEquipment)
            entity.world.serverLevel.chunkSource.sendToTrackingPlayers(entity.nmsEntity, packet)
        }
    }
    
}