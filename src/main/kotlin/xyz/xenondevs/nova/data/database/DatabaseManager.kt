package xyz.xenondevs.nova.data.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.database.table.TileEntitiesTable
import xyz.xenondevs.nova.data.database.table.TileInventoriesTable
import xyz.xenondevs.nova.util.runAsyncTask
import java.io.File

fun asyncTransaction(statement: Transaction.() -> Unit) {
    runAsyncTask {
        transaction(statement = statement)
    }
}

object DatabaseManager {
    
    private lateinit var database: Database
    private lateinit var dataSource: HikariDataSource
    
    val MYSQL = DEFAULT_CONFIG.getBoolean("mysql.enabled")
    
    fun connect() {
        LOGGER.info("Connecting to database")
        if (MYSQL) {
            val address = DEFAULT_CONFIG.getString("mysql.address")!!
            val port = DEFAULT_CONFIG.getInt("mysql.port")!!
            val username = DEFAULT_CONFIG.getString("mysql.username")!!
            val password = DEFAULT_CONFIG.getString("mysql.password")!!
            val databaseName = DEFAULT_CONFIG.getString("mysql.database")!!
            
            connectMySql(address, port, username, password, databaseName)
        } else connectSqlite()
    }
    
    fun disconnect() {
        dataSource.close()
    }
    
    private fun connectMySql(address: String, port: Int, username: String, password: String, databaseName: String) {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:mysql://$address:$port/$databaseName?&rewriteBatchedStatements=true"
        config.username = username
        config.password = password
        config.driverClassName = "com.mysql.jdbc.Driver"
        
        connect(config)
    }
    
    private fun connectSqlite() {
        val config = HikariConfig()
        config.jdbcUrl = "jdbc:sqlite:" + File("plugins/Nova/database.db").absolutePath
        config.driverClassName = "org.sqlite.JDBC"
        
        connect(config)
    }
    
    private fun connect(config: HikariConfig) {
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        
        dataSource = HikariDataSource(config)
        database = Database.connect(dataSource)
        
        transaction {
            SchemaUtils.create(TileEntitiesTable, TileInventoriesTable)
        }
    }
    
}
