package xyz.xenondevs.nova.world.block.hitbox

import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.bukkit.GameMode
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.inventory.EquipmentSlot
import org.joml.Intersectionf
import org.joml.Vector2f
import org.joml.Vector3f
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.serverbound.ServerboundInteractPacketEvent
import xyz.xenondevs.nova.util.bukkitEquipmentSlot
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.util.serverLevel
import xyz.xenondevs.nova.util.toLocation
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.util.toVec3
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.fakeentity.impl.FakeInteraction
import xyz.xenondevs.nova.world.player.WrappedPlayerInteractEvent
import xyz.xenondevs.nova.world.region.Region
import xyz.xenondevs.nova.world.region.VisualRegion
import java.util.*
import org.bukkit.event.block.Action as BlockAction
import xyz.xenondevs.nova.network.event.serverbound.ServerboundInteractPacketEvent.Action as EntityAction

internal object HitboxManager : Listener, PacketListener {
    
    private val physicalHitboxes = HashMap<PhysicalHitbox, FakeInteraction>()
    private val physicalHitboxesById = HashMap<Int, PhysicalHitbox>()
    
    private val virtualHitboxes = HashSet<VirtualHitbox>()
    private val virtualHitboxesByBlock = HashMap<BlockPos, ArrayList<VirtualHitbox>>()
    
    private val visualizers = Collections.newSetFromMap<Player>(WeakHashMap())
    
    init {
        registerPacketListener()
        registerEvents()
    }
    
    //<editor-fold desc="visualization", defaultstate="collapsed">
    // fixme: since there is no distance filtering, the server will crash if there are too many hitboxes
    fun toggleVisualizer(player: Player): Boolean {
        return if (player in visualizers) {
            visualizers -= player
            hideVirtualHitboxes(player)
            false
        } else {
            visualizers += player
            showVirtualHitboxes(player)
            true
        }
    }
    
    private fun showVirtualHitboxes(player: Player) =
        virtualHitboxes.forEach { showVirtualHitbox(it, player) }
    
    private fun hideVirtualHitboxes(player: Player) =
        virtualHitboxes.forEach { hideVirtualHitbox(it, player) }
    
    private fun showVirtualHitbox(hitbox: VirtualHitbox, player: Player) =
        VisualRegion.showRegion(player, hitbox.uuid, Region(hitbox.from.toLocation(hitbox.world), hitbox.to.toLocation(hitbox.world)))
    
    private fun hideVirtualHitbox(hitbox: VirtualHitbox, player: Player) =
        VisualRegion.hideRegion(player, hitbox.uuid)
    //</editor-fold>
    
    fun registerHitbox(hitbox: Hitbox<*, *>) {
        when (hitbox) {
            is PhysicalHitbox -> addPhysicalHitbox(hitbox)
            is VirtualHitbox -> addVirtualHitbox(hitbox)
        }
    }
    
    fun removeHitbox(hitbox: Hitbox<*, *>) {
        when (hitbox) {
            is PhysicalHitbox -> removePhysicalHitbox(hitbox)
            is VirtualHitbox -> removeVirtualHitbox(hitbox)
        }
    }
    
    private fun addPhysicalHitbox(hitbox: PhysicalHitbox) {
        val fakeInteraction = hitbox.createInteractionEntity()
        physicalHitboxes += hitbox to fakeInteraction
        physicalHitboxesById += fakeInteraction.entityId to hitbox
    }
    
    private fun removePhysicalHitbox(hitbox: PhysicalHitbox) {
        val fakeInteraction = physicalHitboxes.remove(hitbox) ?: return
        fakeInteraction.remove()
        physicalHitboxesById -= fakeInteraction.entityId
    }
    
    private fun addVirtualHitbox(hitbox: VirtualHitbox) {
        for (blockPos in hitbox.blocks) {
            virtualHitboxes += hitbox
            virtualHitboxesByBlock.getOrPut(blockPos) { ArrayList(1) } += hitbox
            visualizers.forEach { showVirtualHitbox(hitbox, it) }
        }
    }
    
    private fun removeVirtualHitbox(hitbox: VirtualHitbox) {
        virtualHitboxes -= hitbox
        visualizers.forEach { hideVirtualHitbox(hitbox, it) }
        for (blockPos in hitbox.blocks) {
            val hitboxes = virtualHitboxesByBlock[blockPos] ?: continue
            if (hitboxes.size > 1) {
                hitboxes -= hitbox
            } else {
                virtualHitboxesByBlock -= blockPos
            }
        }
    }
    
    @PacketHandler
    private fun handleEntityInteractPacket(event: ServerboundInteractPacketEvent) {
        val hitbox = physicalHitboxesById[event.entityId] ?: return
        event.isCancelled = true
        
        runTask {
            val player = event.player
            when (val action = event.action) {
                is EntityAction.Attack -> hitbox.leftClickHandlers.forEach { it.invoke(player) }
                is EntityAction.InteractAtLocation -> {
                    val hand = action.hand.bukkitEquipmentSlot
                    val location = action.location.toVector3f()
                    hitbox.rightClickHandlers.forEach { it.invoke(player, hand, location) }
                }
                
                else -> Unit // Action.Interact can be ignored as the client always sends both Interact and InteractAtLocation
            }
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST)
    private fun handleInteract(wrappedEvent: WrappedPlayerInteractEvent) {
        if (wrappedEvent.actionPerformed)
            return
        
        val event = wrappedEvent.event
        if (event.hand != EquipmentSlot.HAND)
            return
        
        val action = event.action
        if (action != BlockAction.PHYSICAL) {
            val player = event.player
            
            val origin = player.eyeLocation.toVec3()
            val originF = origin.toVector3f()
            val direction = player.eyeLocation.direction.toVec3().normalize()
            val directionF = direction.toVector3f()
            val distance = if (player.gameMode == GameMode.CREATIVE) 8.0 else 4.0
            val dest = Vec3(
                origin.x + direction.x * distance,
                origin.y + direction.y * distance,
                origin.z + direction.z * distance
            )
            
            val world = player.world
            val level = world.serverLevel
            val ctx = ClipContext(origin, dest, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, CollisionContext.empty())
            BlockGetter.traverseBlocks(origin, dest, ctx, { _, pos ->
                val novaPos = pos.toNovaPos(world)
                
                // check for collision with vanilla hitboxes
                val blockState: BlockState = level.getBlockState(pos)
                val blockShape: VoxelShape = ctx.getBlockShape(blockState, level, pos)
                val blockHitLoc: Vector3f? = level.clipWithInteractionOverride(origin, dest, pos, blockShape, blockState)?.let {
                    if (it.type != HitResult.Type.MISS) it.location.toVector3f() else null
                }
                
                var hitbox: VirtualHitbox? = null // the hitbox that was actually hit
                var done: Boolean = blockHitLoc != null // whether something was hit (virtual hitbox or block)
                var distance: Float = blockHitLoc?.distance(originF) ?: Float.MAX_VALUE // the distance to the hit location
                
                // check for collision with virtual hitboxes 
                virtualHitboxesByBlock[novaPos]?.forEach { candidate ->
                    if (candidate.qualifier?.invoke(event) == false)
                        return@forEach // skip hitbox as qualifier has failed
                    
                    val handlers = if (action.isLeftClick) candidate.leftClickHandlers else candidate.rightClickHandlers
                    if (handlers.isEmpty())
                        return@forEach // skip hitbox as it has no handlers for the current action
                    
                    val boxHitResult = Vector2f()
                    if (Intersectionf.intersectRayAab(originF, directionF, candidate.from, candidate.to, boxHitResult)) {
                        val t = boxHitResult.x
                        if (t < distance) {
                            hitbox = candidate
                            distance = t
                            done = true
                        }
                    }
                }
                
                // if a hitbox was hit, execute the handlers
                if (hitbox != null) {
                    val hitLoc = Vector3f(originF.x + directionF.x * distance, originF.y + directionF.y * distance, originF.z + directionF.z * distance)
                    
                    // cancel vanilla interactions
                    event.isCancelled = true
                    // mark performed custom action
                    wrappedEvent.actionPerformed = true
                    
                    // get hit location relative to hitbox.center
                    val center = hitbox.center
                    val relHitLoc = Vector3f(hitLoc.x - center.x, hitLoc.y - center.y, hitLoc.z - center.z)
                    
                    // check protection integrations & invoke handler
                    val handlers = if (action.isLeftClick) hitbox.leftClickHandlers else hitbox.rightClickHandlers
                    if (ProtectionManager.canUseBlock(player, event.item, novaPos)) {
                        handlers.forEach { it.invoke(player, event.hand!!, relHitLoc) }
                    }
                }
                
                // returning Unit stops traversal
                return@traverseBlocks if (done) Unit else null
            }, {})
        }
    }
    
}