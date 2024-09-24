package io.github.thecguy.cloudnet_rest_module.commands

import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.parser.Parser
import org.incendo.cloud.annotations.suggestion.Suggestions
import org.incendo.cloud.context.CommandContext
import org.incendo.cloud.annotations.Permission

import eu.cloudnetservice.common.language.I18n
import eu.cloudnetservice.driver.inject.InjectionLayer
import eu.cloudnetservice.driver.provider.ServiceTaskProvider
import eu.cloudnetservice.driver.service.ServiceTask
import eu.cloudnetservice.node.command.annotation.Description
import eu.cloudnetservice.node.command.source.CommandSource

import io.github.thecguy.cloudnet_rest_module.utli.DBManager

import jakarta.inject.Singleton
import org.incendo.cloud.annotations.Command

import org.jetbrains.annotations.NotNull

import java.util.*


@Singleton
@Permission("thecguy.rest")
@Description("test")
class rest {
    private val dbManager = DBManager()
    private val taskProvider: ServiceTaskProvider = InjectionLayer.ext().instance(ServiceTaskProvider::class.java)


    @Suggestions("rest")
    fun suggestion(context: CommandContext<*>, input: String?): List<ServiceTask> {
        return taskProvider.serviceTasks().toList()
    }

    @Parser(name = "rest", suggestions = "rest")
    fun parser(context: CommandContext<*>, input: Queue<String?>): List<ServiceTask> {
        return taskProvider.serviceTasks().toList()
    }

    @Command("rest users")
    fun users(
        source: CommandSource,
    ) {
        source.sendMessage(I18n.trans("module-rest-command-users"))
        source.sendMessage(dbManager.cmd_rest_users())
    }
    @Command("rest user create <username> <password>")
    fun createUser(
        source: CommandSource,
        @NotNull @Argument("username") username: String,
        @NotNull @Argument("password") password: String
    ) {
        val users = dbManager.cmd_rest_users()
        if (users.contains(username)) {
            source.sendMessage(I18n.trans("module-rest-command-usersexist"))
        } else {
            val encodedpw: String = Base64.getEncoder().encodeToString(password.toByteArray())
            dbManager.dbexecute("INSERT INTO cloudnet_rest_users (user, password) VALUES ('$username', '$encodedpw')")
            source.sendMessage(I18n.trans("module-rest-command-createduser"))
        }
    }
    @Command("rest user delete <username>")
    fun deleteUser(
        source: CommandSource,
        @NotNull @Argument("username") username: String
    ) {
        val users = dbManager.cmd_rest_users()
        if (users.contains(username)) {
            dbManager.dbexecute("DELETE FROM cloudnet_rest_users WHERE user = '$username'")
            source.sendMessage(I18n.trans("module-rest-command-deluser"))
        } else {
            source.sendMessage(I18n.trans("module-rest-command-usernotexist"))
        }
    }
    @Command("rest user user <username> add permission <permission>")
    fun addPermsToUser(
        source: CommandSource,
        @NotNull @Argument("username") username: String,
        @NotNull @Argument("permission") permissison: String
    ) {
        val users = dbManager.cmd_rest_users()
        if (users.contains(username)) {
            val perms = dbManager.cmd_rest_perms(username)
            if (perms.contains(permissison)) {
                source.sendMessage(I18n.trans("module-rest-command-permsalredy"))
            } else {
                dbManager.dbexecute("INSERT INTO cloudnet_rest_permission (user, permission) VALUE ('$username', '$permissison')")
                source.sendMessage(I18n.trans("module-rest-command-permsadded"))
            }
        } else {
            source.sendMessage(I18n.trans("module-rest-command-usernotexist"))
        }
    }
    @Command("rest user user <username> remove permission <permission>")
    fun remPerms(
        source: CommandSource,
        @NotNull @Argument("username") username: String,
        @NotNull @Argument("permission") permisison: String
    ) {
        val users = dbManager.cmd_rest_users()
        if (users.contains(username)) {
            val perms = dbManager.cmd_rest_perms(username)
            if (perms.contains(permisison)) {
                dbManager.dbexecute("DELETE FROM cloudnet_rest_permission WHERE permission = '$permisison'")
                source.sendMessage(I18n.trans("module-rest-command-permsrem"))
            } else {
                source.sendMessage(I18n.trans("module-rest-command-notperms"))
            }
        } else {
            source.sendMessage(I18n.trans("module-rest-command-usernotexist"))
        }
    }

}