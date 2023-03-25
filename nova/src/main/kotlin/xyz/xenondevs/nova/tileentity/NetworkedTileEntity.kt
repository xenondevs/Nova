package xyz.xenondevs.nova.tileentity

import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.reflection.getRuntimeDelegate
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.tileentity.network.EndPointDataHolder
import xyz.xenondevs.nova.tileentity.network.Network
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.NetworkNode
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.DefaultNetworkTypes
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.swingHand
import xyz.xenondevs.nova.world.block.context.BlockInteractContext
import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

abstract class NetworkedTileEntity(blockState: NovaTileEntityState) : TileEntity(blockState), NetworkEndPoint {
    
    final override var isNetworkInitialized = false
    final override val networks: MutableMap<NetworkType, MutableMap<BlockFace, Network>> = HashMap()
    final override val connectedNodes: MutableMap<NetworkType, MutableMap<BlockFace, NetworkNode>> = HashMap()
    final override val holders: MutableMap<NetworkType, EndPointDataHolder> by lazy {
        val map = HashMap<NetworkType, EndPointDataHolder>()
        if (::energyHolder.getRuntimeDelegate() !is PlaceholderProperty) map[DefaultNetworkTypes.ENERGY] = energyHolder
        if (::itemHolder.getRuntimeDelegate() !is PlaceholderProperty) map[DefaultNetworkTypes.ITEMS] = itemHolder
        if (::fluidHolder.getRuntimeDelegate() !is PlaceholderProperty) map[DefaultNetworkTypes.FLUID] = fluidHolder
        return@lazy map
    }
    
    open val energyHolder: EnergyHolder by PlaceholderProperty
    open val itemHolder: ItemHolder by PlaceholderProperty
    open val fluidHolder: FluidHolder by PlaceholderProperty
    
    override fun handleInitialized(first: Boolean) {
        if (first) NetworkManager.queueAsync { it.addEndPoint(this, true) }
    }
    
    override fun reload() {
        super.reload()
        holders.forEach { (_, holder) -> holder.reload() }
    }
    
    override fun saveData() {
        super.saveData()
        holders.values.forEach(EndPointDataHolder::saveData)
        serializeNetworks()
        serializeConnectedNodes()
    }
    
    override fun retrieveSerializedNetworks(): Map<NetworkType, Map<BlockFace, UUID>>? {
        return retrieveDataOrNull<HashMap<NetworkType, EnumMap<BlockFace, UUID>>>("networks")
    }
    
    final override fun handleRightClick(ctx: BlockInteractContext): Boolean {
        val item = ctx.item
        val holder = holders[DefaultNetworkTypes.FLUID]
        if (holder is NovaFluidHolder && ctx.source is Player && ctx.hand != null) {
            val success = when (item?.type) {
                Material.BUCKET -> fillBucket(holder, ctx.source, ctx.hand)
                Material.WATER_BUCKET, Material.LAVA_BUCKET -> emptyBucket(holder, ctx.source, ctx.hand)
                else -> false
            }
            
            if (success) return true
        }
        
        return handleUnknownRightClick(ctx)
    }
    
    private fun emptyBucket(holder: NovaFluidHolder, player: Player, hand: EquipmentSlot): Boolean {
        val bucket = player.inventory.getItem(hand)
        val type = FluidType.values().first { bucket?.isSimilar(it.bucket) ?: false }
        
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
        val face = BlockFaceUtils.determineBlockFaceLookingAt(player.eyeLocation)
        
        val container = holder.containerConfig[face]
            ?.takeUnless { holder.connectionConfig[face] != NetworkConnectionType.NONE || it.amount < 1000 || !holder.allowedConnectionTypes[it]!!.extract }
            ?: holder.availableContainers.values.firstOrNull { it.amount >= 1000 && holder.allowedConnectionTypes[it]!!.extract }
        
        if (container != null) {
            if (player.gameMode != GameMode.CREATIVE) {
                val handItem = inventory.getItem(hand)!!
                val bucket = container.type!!.bucket!!
                if (handItem.amount == 1) {
                    inventory.setItem(hand, bucket)
                } else {
                    handItem.amount -= 1
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
    
    open fun handleUnknownRightClick(ctx: BlockInteractContext): Boolean {
        return super.handleRightClick(ctx)
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        
        if (!unload) {
            NetworkManager.queueAsync { it.removeEndPoint(this, true) }
            val itemHolder = holders[DefaultNetworkTypes.ITEMS]
            if (itemHolder is ItemHolder) {
                itemHolder.insertFilters.clear()
                itemHolder.extractFilters.clear()
            }
        }
    }
    
    override fun getDrops(includeSelf: Boolean): MutableList<ItemStack> {
        val drops = super.getDrops(includeSelf)
        val itemHolder = holders[DefaultNetworkTypes.ITEMS]
        if (itemHolder is ItemHolder)
            drops += (itemHolder.insertFilters.values.asSequence() + itemHolder.extractFilters.values.asSequence())
                .map(ItemFilter::createFilterItem)
        return drops
    }
    
}

private object PlaceholderProperty : ReadOnlyProperty<Any?, Nothing> {
    
    override fun getValue(thisRef: Any?, property: KProperty<*>): Nothing {
        throw UnsupportedOperationException("PlaceholderProperty cannot be read")
    }
    
}