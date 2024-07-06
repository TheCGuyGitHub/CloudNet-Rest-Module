package io.github.thecguy.cloudnet_rest_module.utli

import ch.qos.logback.classic.Level
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.slf4j.LoggerFactory
import java.sql.ResultSet

class DBManager internal constructor() {
    private val ds: HikariDataSource

    //Original made by byPixelTV!


    init {
        val hikariLogger = LoggerFactory.getLogger("com.zaxxer.hikari") as ch.qos.logback.classic.Logger
        hikariLogger.level = Level.DEBUG

        val config = HikariConfig()

        val host = "127.0.0.1"
        val port = 3306
        val database = "cloudnet_rest"
        val username = "cloudnet"
        val password = "cloudnet"
        val CONNECT_URL_FORMAT: String = "jdbc:mariadb://%s:%d/%s?serverTimezone=UTC"

        config.jdbcUrl = String.format(
            CONNECT_URL_FORMAT,
            host, port, database
        )
        config.username = username
        config.password = password
        config.driverClassName = "org.mariadb.jdbc.Driver"
        config.maxLifetime = 9223372036854775807

        ds = HikariDataSource(config)
    }
    fun dbexecwithrs(sql: String): ResultSet {
        ds.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                val resultSet = statement.executeQuery()
                return resultSet
            }
        }
    }
    fun dbexecute(sql: String) {
        ds.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.executeUpdate()
            }
        }
    }
    fun closedb() {
        ds.close()
    }

    fun getpw(user: String): String {
        val users = cmd_rest_users()
        var pw: String = ""
        if (users.contains(user)) {
            ds.connection.use { connection ->
                connection.prepareStatement("SELECT password FROM cloudnet_rest_users WHERE user = '$user'").use { statement ->
                    statement.executeQuery().use { resultSet ->
                        if (resultSet.next()) {
                            pw = resultSet.getString("password")
                        }
                    }
                }
            }
        }
        return pw
    }

    fun tokens(): List<String> {
        val tokenList = mutableListOf<String>()
        ds.connection.use { connection ->
            connection.prepareStatement("SELECT value FROM cloudnet_rest_auths").use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val token = resultSet.getString("value")
                        tokenList.add(token)
                    }
                }
            }
        }
        return tokenList
    }
    fun tokensDate(): List<String> {
        val exprList = mutableListOf<String>()
        ds.connection.use { connection ->
            connection.prepareStatement("SELECT timestamp FROM cloudnet_rest_auths").use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val exprs = resultSet.getString("timestamp")
                        exprList.add(exprs)
                    }
                }
            }
        }
        return exprList
    }



    fun cmd_rest_users(): List<String> {
        val userList = mutableListOf<String>()
        ds.connection.use { connection ->
            connection.prepareStatement("SELECT user FROM cloudnet_rest_users").use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val user = resultSet.getString("user")
                        userList.add(user)
                    }
                }
            }
        }
        return userList
    }

    fun cmd_rest_perms(user: String): List<String> {
        val permslist = mutableListOf<String>()
        ds.connection.use { connection ->
            connection.prepareStatement("SELECT permission FROM cloudnet_rest_permission WHERE user = '$user'").use { statement ->
                statement.executeQuery().use { resultSet ->
                    while (resultSet.next()) {
                        val perms = resultSet.getString("permission")
                        permslist.add(perms)
                    }
                }
            }
        }
        return permslist
    }


    companion object {
        private var instance: DBManager? = null
        fun getInstance(): DBManager {
            if (instance == null) {
                instance = DBManager()
            }
            return instance!!
        }
    }
}