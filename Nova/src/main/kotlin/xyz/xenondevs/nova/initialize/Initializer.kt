package xyz.xenondevs.nova.initialize

import org.bstats.bukkit.Metrics
import org.bstats.charts.DrilldownPie
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.UpdateReminder
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.addon.AddonsInitializer
import xyz.xenondevs.nova.addon.AddonsLoader
import xyz.xenondevs.nova.api.event.NovaLoadDataEvent
import xyz.xenondevs.nova.command.CommandManager
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.item.ItemManager
import xyz.xenondevs.nova.material.ItemCategories
import xyz.xenondevs.nova.material.PacketItems
import xyz.xenondevs.nova.network.PacketManager
import xyz.xenondevs.nova.player.ability.AbilityManager
import xyz.xenondevs.nova.player.attachment.AttachmentManager
import xyz.xenondevs.nova.player.equipment.ArmorEquipListener
import xyz.xenondevs.nova.tileentity.ChunkLoadManager
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.ui.setGlobalIngredients
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.runTask
import xyz.xenondevs.nova.world.ChunkReloadWatcher
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.loot.LootConfigHandler
import xyz.xenondevs.nova.world.loot.LootGeneration
import java.util.concurrent.CountDownLatch
import java.util.logging.Level

object Initializer {
    
    private val toInit = listOf(
        UpdateReminder, AddonsInitializer, NovaConfig, AutoUploadManager, Resources,
        CustomItemServiceManager, PacketItems, LocaleManager, ChunkReloadWatcher, FakeArmorStandManager,
        RecipeManager, RecipeRegistry, ChunkLoadManager, VanillaTileEntityManager,
        NetworkManager, ItemManager, AttachmentManager, CommandManager, ArmorEquipListener,
        AbilityManager, PacketManager, LootConfigHandler, LootGeneration, AddonsLoader, ItemCategories,
        BlockManager, WorldDataManager, TileEntityManager
    ).sorted()
    
    private val latch = CountDownLatch(toInit.size)
    
    fun init() {
        runAsyncTask {
            toInit.forEach {
                runAsyncTask {
                    try {
                        it.dependsOn.forEach { it.latch.await() }
                        it.initialize(latch)
                    } catch (e: Exception) {
                        LOGGER.log(Level.SEVERE, "An exception occurred trying to initialize $it", e)
                        latch.countDown()
                    }
                }
            }
            latch.await()
            callEvent(NovaLoadDataEvent())
            
            runTask {
                setGlobalIngredients()
                AddonManager.enableAddons()
                setupMetrics()
                LOGGER.info("Done loading")
            }
        }
    }
    
    fun disable() {
        toInit.reversed().forEach {
            try {
                it.disable()
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "An exception occurred trying to disable $it", e)
            }
        }
    }
    
    private fun setupMetrics() {
        val metrics = Metrics(NOVA, 11927)
        metrics.addCustomChart(DrilldownPie("addons") {
            val map = HashMap<String, Map<String, Int>>()
            
            AddonManager.addons.values.forEach {
                map[it.description.name] = mapOf(it.description.version to 1)
            }
            
            return@DrilldownPie map
        })
    }
    
}