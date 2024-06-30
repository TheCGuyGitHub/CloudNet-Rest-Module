package io.github.thecguy.cloudnet_rest_module.commands

import cloud.commandframework.annotations.CommandMethod
import cloud.commandframework.annotations.CommandPermission
import cloud.commandframework.annotations.parsers.Parser
import cloud.commandframework.annotations.suggestions.Suggestions
import cloud.commandframework.context.CommandContext
import eu.cloudnetservice.driver.inject.InjectionLayer
import eu.cloudnetservice.driver.provider.ServiceTaskProvider
import eu.cloudnetservice.driver.service.ServiceTask
import eu.cloudnetservice.driver.database.DatabaseProvider
import eu.cloudnetservice.node.command.annotation.Description
import eu.cloudnetservice.node.command.source.CommandSource

import jakarta.inject.Singleton
import java.util.*


@Singleton
@CommandPermission("thecguy.test")
@Description("test")
class Test {

    private val taskProvider: ServiceTaskProvider = InjectionLayer.ext().instance(ServiceTaskProvider::class.java)
    private val databaseProvider: DatabaseProvider = InjectionLayer.ext().instance(DatabaseProvider::class.java)




    @Suggestions("test")
    fun suggestion(context: CommandContext<*>, input: String?): List<ServiceTask> {
        return taskProvider.serviceTasks().toList()
    }

    @Parser(name = "test", suggestions = "test")
    fun parser(context: CommandContext<*>, input: Queue<String?>): List<ServiceTask> {
        return taskProvider.serviceTasks().toList()
    }

    @CommandMethod("test dbs")
    fun enable(
        source: CommandSource,
    ) {

        source.sendMessage(databaseProvider.databaseNames())
    }

    @CommandMethod("test create")
    fun test(
        source: CommandSource,
    ) {
        databaseProvider.database("test")
    }

    @CommandMethod("test insert")
    fun insert(
        source: CommandSource,
    ) {
        println("test")
    }


}