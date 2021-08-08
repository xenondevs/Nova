package xyz.xenondevs.nova.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.transaction
import xyz.xenondevs.nova.config.NovaConfig
import xyz.xenondevs.nova.database.table.TileEntitiesTable
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
    
    fun connect() {
        if (NovaConfig.getBoolean("mysql.enabled")) {
            val address = NovaConfig.getString("mysql.address")!!
            val port = NovaConfig.getInt("mysql.port")!!
            val username = NovaConfig.getString("mysql.username")!!
            val password = NovaConfig.getString("mysql.password")!!
            val databaseName = NovaConfig.getString("mysql.database")!!
            
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
        
        transaction { SchemaUtils.create(TileEntitiesTable) }
    }
    
}
