package io.github.thecguy.cloudnet_rest_module




import eu.cloudnetservice.common.language.I18n
import eu.cloudnetservice.driver.document.Document
import eu.cloudnetservice.driver.document.DocumentFactory
import eu.cloudnetservice.driver.inject.InjectionLayer
import eu.cloudnetservice.driver.module.ModuleLifeCycle
import eu.cloudnetservice.driver.module.ModuleTask
import eu.cloudnetservice.driver.module.driver.DriverModule
import eu.cloudnetservice.node.ShutdownHandler
import eu.cloudnetservice.node.command.CommandProvider
import eu.cloudnetservice.node.service.CloudServiceManager
import eu.cloudnetservice.driver.provider.ServiceTaskProvider
import io.github.thecguy.cloudnet_rest_module.commands.rest
import io.github.thecguy.cloudnet_rest_module.config.Configuration
import io.github.thecguy.cloudnet_rest_module.coroutines.AuthChecker
import io.github.thecguy.cloudnet_rest_module.utli.DBManager
import io.github.thecguy.cloudnet_rest_module.utli.JsonUtils
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jakarta.inject.Named
import jakarta.inject.Singleton
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull
import java.util.*


@Singleton
class CloudNet_Rest_Module : DriverModule() {
     private val dbm = DBManager()
     private val jsonUtils = JsonUtils()
    private val authChecker = AuthChecker()
    @Volatile
     private var configuration: Configuration? = null


    @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
    fun convertConfig() {
        val config = this.readConfig(DocumentFactory.json())
        this.writeConfig(
            Document.newJsonDocument().appendTree(
                Configuration(
                    config.getString("username"),
                    config.getString("password"),
                    config.getString("database"),
                    config.getString("host"),
                    config.getInt("port"),
                    config.getInt("restapi_port")
                )
            )
        )
    }

    @ModuleTask(order = 125, lifecycle = ModuleLifeCycle.LOADED)
    fun load() {
         configuration = this.readConfig(
            Configuration::class.java,
            {
                Configuration(
                    "root",
                    "123456",
                    "cloudnet_rest",
                    "127.0.0.1",
                    3306,
                    8080
                )
            },
            DocumentFactory.json()
        )
        dbm.dbexecute("CREATE TABLE IF NOT EXISTS cloudnet_rest_users (id SERIAL PRIMARY KEY, user TEXT, password TEXT)")
        dbm.dbexecute("CREATE TABLE IF NOT EXISTS cloudnet_rest_permission (id SERIAL PRIMARY KEY, user TEXT, permission TEXT)")
        dbm.dbexecute("CREATE TABLE IF NOT EXISTS cloudnet_rest_auths (id SERIAL PRIMARY KEY, type TEXT, value TEXT, timestamp TEXT)")
        dbm.dbexecute("DELETE FROM cloudnet_rest_auths")
        authChecker.schedule()
    }
    @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STARTED)
    fun started(
        @NotNull cloudServiceManager: CloudServiceManager,
        @NotNull shutdownHandler: ShutdownHandler,
        @NotNull serviceTaskProvider: ServiceTaskProvider,
        @NotNull @Named("module") injectionLayer: InjectionLayer<*>
    ) {


        I18n.loadFromLangPath(CloudNet_Rest_Module::class.java)
        GlobalScope.launch {
            main(cloudServiceManager, shutdownHandler, serviceTaskProvider)
        }
        println("Rest API listening on port {configuration!!.restapi_port}!")
    }
    @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
    fun start(commandProvider: CommandProvider) {
        commandProvider.register(rest::class.java)
    }
    @ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
    fun stop() {
        println("Closing DB connection!")
        dbm.closedb()
        println("Closed DB connection!")
    }

    private fun main(
        @NotNull cloudServiceManager: CloudServiceManager,
        @NotNull shutdownHandler: ShutdownHandler,
        @NotNull serviceTaskProvider: ServiceTaskProvider
    ) {
        val port = configuration!!.restapi_port
        embeddedServer(Netty, port = port) {

            install(ShutDownUrl.ApplicationCallPlugin) {
                shutDownUrl = "/debug/shutdown"
                exitCodeSupplier = { 0 }
            }
            install(Authentication) {
                basic("auth-basic") {
                    realm = "Access to the '/' path"
                    validate { credentials ->
                        val users = dbm.cmd_rest_users()
                        if (users.contains(credentials.name)) {
                            val pw = String(Base64.getDecoder().decode(credentials.password))
                            val pww = dbm.getpw(credentials.name)
                            val pwwd = String(Base64.getDecoder().decode(pww))
                            if (pw == pwwd) {
                                println("USER AUTH SUCCESS!")
                                UserIdPrincipal(credentials.name)
                            } else {
                                println("error in auth")
                                null
                            }
                        } else {
                            println("error in auth")
                            null
                        }
                    }
                }
            }

            routing {

                swaggerUI(path = "swagger", swaggerFile = "openapi/swagger.yaml")
                authenticate("auth-basic") {
                    get("/auth") {
                        call.respond(jsonUtils.token().toString(4))
                    }
                }

                //services
                get("/services") {
                    val services = jsonUtils.services(cloudServiceManager)
                    val tokens = dbm.tokens()
                    val rToken = call.request.headers["Authorization"]
                    if (tokens.contains(rToken)) {
                        call.respondText(
                            services.toString(4)
                                .replace("[", "")
                                .replace("]", "")
                        )
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }

                get("/services/{service}") {
                    val services = cloudServiceManager.services().map { it.name() }.toList()
                    val serv = services.contains(call.parameters["service"])
                    val tokens = dbm.tokens()
                    val rToken = call.request.headers["Authorization"]
                    if (tokens.contains(rToken)) {
                        if (serv) {
                                call.respond(jsonUtils.service(cloudServiceManager, call.parameters["service"].toString()).toString(4))
                        } else {
                            call.response.status(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }

                //tasks
                get("/tasks") {
                    val tokens = dbm.tokens()
                    val rToken = call.request.headers["Authorization"]
                    if (tokens.contains(rToken)) {
                        call.respond(jsonUtils.tasks(serviceTaskProvider).toString(4))
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }

                get("/tasks/{task}") {
                    val tasks = serviceTaskProvider.serviceTasks().map { it.name() }.toList()
                    val tas = tasks.contains(call.parameters["task"])
                    val tokens = dbm.tokens()
                    val rToken = call.request.headers["Authorization"]
                    if (tokens.contains(rToken)) {
                        if (tas) {
                            call.respond(jsonUtils.task(serviceTaskProvider, call.parameters["task"].toString()))
                        } else {
                            call.response.status(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }





                get("/node/shutdown") {
                    call.respondText("NOTE: The Cloud is shutting down!")
                    shutdownHandler.shutdown()
                }


                get("/") {

                    call.respondText("Welcome to my Rest API!")
                }



            }
        }.start(wait = false)
    }
}