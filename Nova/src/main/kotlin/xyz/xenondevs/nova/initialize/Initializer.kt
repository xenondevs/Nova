package xyz.xenondevs.nova.initialize

import de.studiocode.invui.resourcepack.ForceResourcePack
import net.md_5.bungee.api.chat.ComponentBuilder
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.UpdateReminder
import xyz.xenondevs.nova.api.event.NovaLoadDataEvent
import xyz.xenondevs.nova.command.CommandManager
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.database.DatabaseManager
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.item.ItemManager
import xyz.xenondevs.nova.material.PacketItems
import xyz.xenondevs.nova.network.PacketListener
import xyz.xenondevs.nova.player.ability.AbilityManager
import xyz.xenondevs.nova.player.advancement.AdvancementManager
import xyz.xenondevs.nova.player.attachment.AttachmentManager
import xyz.xenondevs.nova.player.equipment.ArmorEquipListener
import xyz.xenondevs.nova.tileentity.ChunkLoadManager
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.world.ChunkReloadWatcher
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager
import xyz.xenondevs.nova.world.loot.LootGeneration
import java.util.concurrent.CountDownLatch

object Initializer {
    
    private val toInit = listOf(
        Resources, UpdateReminder, DatabaseManager, CustomItemServiceManager,
        PacketItems,
        LocaleManager, ChunkReloadWatcher, FakeArmorStandManager, AdvancementManager,
        RecipeManager, RecipeRegistry, ChunkLoadManager, VanillaTileEntityManager,
        TileEntityManager, NetworkManager, ItemManager, AttachmentManager,
        CommandManager, ArmorEquipListener, AbilityManager, PacketListener,
        LootGeneration
    ).sorted()
    
    private val latch = CountDownLatch(toInit.size)
    
    
    fun init() {
        runAsyncTask {
            toInit.forEach {
                runAsyncTask {
                    it.dependsOn?.latch?.await()
                    it.initialize(latch)
                }
            }
            latch.await()
            forceResourcePack()
            callEvent(NovaLoadDataEvent())
            
            LOGGER.info("Done loading")
        }
    }
    
    private fun forceResourcePack() {
        if (DEFAULT_CONFIG.getBoolean("resource_pack.enabled")) {
            ForceResourcePack.getInstance().setResourcePack(
                DEFAULT_CONFIG.getString("resource_pack.url"),
                ComponentBuilder("Nova Resource Pack").create()
            )
        }
    }
    
}