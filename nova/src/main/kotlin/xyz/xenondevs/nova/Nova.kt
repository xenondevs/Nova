@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION", "UnstableApiUsage")

package xyz.xenondevs.nova

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.gson.*
import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import xyz.xenondevs.invui.InvUI
import xyz.xenondevs.invui.i18n.Languages
import xyz.xenondevs.nova.api.ApiBlockManager
import xyz.xenondevs.nova.api.ApiBlockRegistry
import xyz.xenondevs.nova.api.ApiItemRegistry
import xyz.xenondevs.nova.api.ApiTileEntityManager
import xyz.xenondevs.nova.api.NovaMaterialRegistry
import xyz.xenondevs.nova.api.protection.ProtectionIntegration
import xyz.xenondevs.nova.initialize.Initializer
import xyz.xenondevs.nova.integration.protection.ProtectionManager
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.util.ServerUtils
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.api.Nova as INova
import xyz.xenondevs.nova.api.block.BlockManager as IBlockManager
import xyz.xenondevs.nova.api.block.NovaBlockRegistry as INovaBlockRegistry
import xyz.xenondevs.nova.api.item.NovaItemRegistry as INovaItemRegistry
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry
import xyz.xenondevs.nova.api.player.WailaManager as IWailaManager
import xyz.xenondevs.nova.api.tileentity.TileEntityManager as ITileEntityManager

internal val HTTP_CLIENT = HttpClient(CIO) {
    install(ContentNegotiation) { gson() }
    install(HttpTimeout) {
        connectTimeoutMillis = 10_000
        requestTimeoutMillis = Long.MAX_VALUE
    }
    expectSuccess = false
}

internal var PLUGIN_READY = false
    private set

internal object Nova : JavaPlugin(), INova {
    
    override fun onEnable() {
        if (BOOTSTRAPPER.remainingAddons > 0)
            throw IllegalStateException("${BOOTSTRAPPER.remainingAddons} addons did not load.")
        
        PLUGIN_READY = true
        LIFECYCLE_MANAGER = lifecycleManager
        
        InvUI.getInstance().setPlugin(this)
        Languages.getInstance().enableServerSideTranslations(false)
        Initializer.registerEvents()
    }
    
    override fun onDisable() {
        Initializer.disable()
        
        if (ServerUtils.isReload()) {
            LOGGER.error("====================================================")
            LOGGER.error("RELOADING IS NOT SUPPORTED, SHUTTING DOWN THE SERVER")
            LOGGER.error("====================================================")
            Bukkit.shutdown()
        }
    }
    
    //<editor-fold desc="nova-api", defaultstate="collapsed">
    override fun getBlockManager(): IBlockManager = ApiBlockManager
    override fun getTileEntityManager(): ITileEntityManager = ApiTileEntityManager
    override fun getMaterialRegistry(): INovaMaterialRegistry = NovaMaterialRegistry
    override fun getBlockRegistry(): INovaBlockRegistry = ApiBlockRegistry
    override fun getItemRegistry(): INovaItemRegistry = ApiItemRegistry
    override fun getWailaManager(): IWailaManager = WailaManager
    
    override fun registerProtectionIntegration(integration: ProtectionIntegration) {
        ProtectionManager.integrations.add(integration)
    }
    //</editor-fold>
    
}