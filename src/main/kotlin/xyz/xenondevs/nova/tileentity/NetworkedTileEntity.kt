package xyz.xenondevs.nova.tileentity

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.network.*
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.util.*
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.actualDelegate
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class NetworkedTileEntity(
    uuid: UUID,
    data: CompoundElement,
    material: NovaMaterial,
    ownerUUID: UUID,
    armorStand: FakeArmorStand,
) : TileEntity(uuid, data, material, ownerUUID, armorStand), NetworkEndPoint {
    
    final override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { emptyEnumMap() }
    final override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> =
        NetworkType.values().associateWithTo(emptyEnumMap()) { emptyEnumMap() }
    final override val holders: MutableMap<NetworkType, EndPointDataHolder> by lazy {
        val map: EnumMap<NetworkType, EndPointDataHolder> = emptyEnumMap()
        if (::energyHolder.actualDelegate !is PlaceholderProperty) map[NetworkType.ENERGY] = energyHolder
        if (::itemHolder.actualDelegate !is PlaceholderProperty) map[NetworkType.ITEMS] = itemHolder
        if (::fluidHolder.actualDelegate !is PlaceholderProperty) map[NetworkType.FLUID] = fluidHolder
        return@lazy map
    }
    
    open val energyHolder: EnergyHolder by PlaceholderProperty
    open val itemHolder: ItemHolder by PlaceholderProperty
    open val fluidHolder: FluidHolder by PlaceholderProperty
    
    override fun saveData() {
        super.saveData()
        holders.values.forEach(EndPointDataHolder::saveData)
    }
    
    override fun handleInitialized(first: Boolean) {
        NetworkManager.runAsync { it.handleEndPointAdd(this) }
    }
    
    final override fun handleRightClick(event: PlayerInteractEvent) {
        if (event.handItems.any { it.novaMaterial == NovaMaterialRegistry.WRENCH }) {
            handleWrenchClick(event)
        } else {
            val holder = holders[NetworkType.FLUID]
            if (holder is NovaFluidHolder) {
                val player = event.player
                val hand = event.hand!!
                
                val success = when (player.inventory.getItem(hand).type) {
                    Material.BUCKET -> fillBucket(holder, player, hand)
                    Material.WATER_BUCKET, Material.LAVA_BUCKET -> emptyBucket(holder, player, hand)
                    else -> false
                }
                
                if (success) return
            }
            
            handleUnknownRightClick(event)
        }
    }
    
    private fun handleWrenchClick(event: PlayerInteractEvent) {
        val face = event.blockFace
        
        NetworkManager.runAsync {
            val itemHolder = holders[NetworkType.ITEMS]
            if (itemHolder is ItemHolder)
                itemHolder.cycleItemConfig(it, face, true)
        }
    }
    
    private fun emptyBucket(holder: NovaFluidHolder, player: Player, hand: EquipmentSlot): Boolean {
        val bucket = player.inventory.getItem(hand)
        val type = FluidType.values().first { bucket.isSimilar(it.bucket) }
        
        val container = holder.availableContainers.values.firstOrNull { it.accepts(type, 1000) && holder.allowedConnectionTypes[it]!!.insert }
        if (container != null) {
            container.addFluid(type, 1000)
            if (player.gameMode != GameMode.CREATIVE)
                player.inventory.setItem(hand, ItemStack(Material.BUCKET))
            
            return true
        }
        
        return false
    }
    
    private fun fillBucket(holder: NovaFluidHolder, player: Player, hand: EquipmentSlot): Boolean {
        val inventory = player.inventory
        val targetLocation = player.eyeLocation.getTargetLocation(0.25, 8.0)
        val face = BlockFaceUtils.determineBlockFace(location.block, targetLocation)
        
        val container = holder.containerConfig[face]
            ?.takeUnless { holder.connectionConfig[face] != NetworkConnectionType.NONE || it.amount < 1000 || !holder.allowedConnectionTypes[it]!!.extract }
            ?: holder.availableContainers.values.firstOrNull { it.amount >= 1000 && holder.allowedConnectionTypes[it]!!.extract }
        
        if (container != null) {
            if (player.gameMode != GameMode.CREATIVE) {
                val bucket = container.type!!.bucket!!
                if (inventory.getItem(hand).amount == 1) {
                    inventory.setItem(hand, bucket)
                } else {
                    inventory.getItem(hand).amount -= 1
                    inventory.addItem(bucket)
                }
            }
            
            when (container.type) {
                FluidType.LAVA -> player.playSound(player.location, Sound.ITEM_BUCKET_FILL_LAVA, 1f, 1f)
                FluidType.WATER -> player.playSound(player.location, Sound.ITEM_BUCKET_FILL, 1f, 1f)
                else -> throw IllegalStateException()
            }
            
            container.takeFluid(1000)
            player.swingHand(hand)
            
            return true
        }
        
        return false
    }
    
    open fun handleUnknownRightClick(event: PlayerInteractEvent) {
        super.handleRightClick(event)
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        
        val task: NetworkManagerTask = { it.handleEndPointRemove(this, unload) }
        if (NOVA.isEnabled) NetworkManager.runAsync(task) else NetworkManager.runNow(task)
    }
    
    override fun destroy(dropItems: Boolean): ArrayList<ItemStack> {
        val items = super.destroy(dropItems)
        if (dropItems) {
            val itemHolder = holders[NetworkType.ITEMS]
            if (itemHolder is ItemHolder) {
                items += (itemHolder.insertFilters.values.asSequence() + itemHolder.extractFilters.values.asSequence())
                    .map(ItemFilter::createFilterItem)
                
                itemHolder.insertFilters.clear()
                itemHolder.extractFilters.clear()
            }
        }
        
        return items
    }
    
}

private object PlaceholderProperty : ReadOnlyProperty<Any?, Nothing> {
    
    override fun getValue(thisRef: Any?, property: KProperty<*>): Nothing {
        throw UnsupportedOperationException("PlaceholderProperty cannot be read")
    }
    
}