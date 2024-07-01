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


        println("ich binrunned")
        val sql = CloudNet_Rest_Module()
        println("so far so good")
        sql.sqlwr("SELECT * FROM cloudnet_rest_users")
        println("how the fuk")

    }



}