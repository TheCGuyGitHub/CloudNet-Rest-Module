package io.github.thecguy.cloudnet_rest_module.commands

import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.parsers.Parser
import cloud.commandframework.annotations.suggestions.Suggestions
import cloud.commandframework.context.CommandContext
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.cloudnetservice.driver.inject.InjectionLayer
import eu.cloudnetservice.driver.provider.ServiceTaskProvider
import eu.cloudnetservice.driver.service.ServiceTask
import eu.cloudnetservice.driver.database.DatabaseProvider
import eu.cloudnetservice.driver.document.Document
import eu.cloudnetservice.node.command.annotation.Description
import eu.cloudnetservice.node.command.source.CommandSource

import io.github.thecguy.cloudnet_rest_module.CloudNet_Rest_Module
import io.github.thecguy.cloudnet_rest_module.config.Configuration

import jakarta.inject.Singleton
import org.checkerframework.checker.nullness.qual.NonNull
import org.jetbrains.annotations.NotNull
import java.util.*


@Singleton
@CommandPermission("thecguy.test")
@Description("test")
class rest {


    public class test(
        @NotNull config: Configuration
    ) {
        public val config = config
    }

    private val taskProvider: ServiceTaskProvider = InjectionLayer.ext().instance(ServiceTaskProvider::class.java)
    private val databaseProvider: DatabaseProvider = InjectionLayer.ext().instance(DatabaseProvider::class.java)


    @Suggestions("rest")
    fun suggestion(context: CommandContext<*>, input: String?): List<ServiceTask> {
        return taskProvider.serviceTasks().toList()
    }

    @Parser(name = "rest", suggestions = "rest")
    fun parser(context: CommandContext<*>, input: Queue<String?>): List<ServiceTask> {
        return taskProvider.serviceTasks().toList()
    }

    @CommandMethod("rest users")
    fun users(
        source: CommandSource,
    ) {
        val host = "127.0.0.1"
        val port = 3306
        val database = "cloudnet_rest"
        val username = "cloudnet"
        val password = "cloudnet"
        val CONNECT_URL_FORMAT: String = "jdbc:mysql://%s:%d/%s?serverTimezone=UTC"
        val config = HikariConfig()
        config.jdbcUrl = String.format(
            CONNECT_URL_FORMAT,
            host, port, database
        )
        config.username = username
        config.password = password
        config.driverClassName = "com.mysql.cj.jdbc.Driver"
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
        val ds = HikariDataSource(config)
        ds.connection.use { connection ->
            connection.prepareStatement("SELECT user FROM cloudnet_rest_users").use { statement ->
                statement.executeQuery().use { resultSet ->
                    source.sendMessage("Current registered RestAPI users:")
                    while (resultSet.next()) {
                        val user = resultSet.getString("user")
                        source.sendMessage(user)
                    }
                }
            }
        }
        ds.close()
    }



}