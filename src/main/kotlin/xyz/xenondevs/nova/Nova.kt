package xyz.xenondevs.nova

import de.studiocode.invui.resourcepack.ForceResourcePack
import net.md_5.bungee.api.chat.ComponentBuilder
import org.bstats.bukkit.Metrics
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.nova.command.CommandManager
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.database.DatabaseManager
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.recipe.RecipeRegistry
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.item.ItemManager
import xyz.xenondevs.nova.network.PacketListener
import xyz.xenondevs.nova.player.ability.AbilityManager
import xyz.xenondevs.nova.player.advancement.AdvancementManager
import xyz.xenondevs.nova.player.attachment.AttachmentManager
import xyz.xenondevs.nova.player.equipment.ArmorEquipListener
import xyz.xenondevs.nova.tileentity.ChunkLoadManager
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.ui.setGlobalIngredients
import xyz.xenondevs.nova.util.data.Version
import xyz.xenondevs.nova.world.LootGeneration
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager
import java.util.logging.Logger

lateinit var NOVA: Nova
lateinit var LOGGER: Logger
var IS_VERSION_CHANGE: Boolean = false

class Nova : JavaPlugin() {
    
    val version = Version(description.version.removeSuffix("-SNAPSHOT"))
    val devBuild = description.version.contains("SNAPSHOT")
    val disableHandlers = ArrayList<() -> Unit>()
    val pluginFile
        get() = file
    var isUninstalled = false
    
    override fun onEnable() {
        NOVA = this
        LOGGER = logger
        
        IS_VERSION_CHANGE = PermanentStorage.retrieve("last_version") { "0.1" } != description.version
        PermanentStorage.store("last_version", description.version)
        UpdateReminder.init()
        
        setGlobalIngredients()
        NovaConfig.init()
        DatabaseManager.connect()
        LocaleManager.init()
        FakeArmorStandManager.init()
        AdvancementManager.loadAdvancements()
        RecipeManager.registerRecipes()
        RecipeRegistry.init()
        ChunkLoadManager.init()
        VanillaTileEntityManager.init()
        TileEntityManager.init()
        NetworkManager.init()
        ItemManager.init()
        AttachmentManager.init()
        CommandManager.init()
        ArmorEquipListener.init()
        AbilityManager.init()
        PacketListener.init()
        LootGeneration.init()
        forceResourcePack()
        
        Metrics(this, 11927)
        
        LOGGER.info("Done loading")
    }
    
    override fun onDisable() {
        disableHandlers.forEach {
            runCatching(it).onFailure(Throwable::printStackTrace)
        }
        DatabaseManager.disconnect()
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