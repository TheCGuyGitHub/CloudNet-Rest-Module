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
import io.github.thecguy.cloudnet_rest_module.commands.rest
import io.github.thecguy.cloudnet_rest_module.config.Configuration
import io.github.thecguy.cloudnet_rest_module.utli.DBManager
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jakarta.inject.Named
import jakarta.inject.Singleton
import kong.unirest.core.json.JSONArray
import kong.unirest.core.json.JSONObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull
import java.util.Base64


@Singleton
class CloudNet_Rest_Module : DriverModule() {

     var configuration: Configuration? = null
     var host: String? = null
     var port: Int? = null
     var database: String? = null
     var username: String? = null
     var password: String? = null

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
    fun lload() {
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
        val dbm = DBManager()
        dbm.dbexecute("CREATE TABLE IF NOT EXISTS cloudnet_rest_users (id SERIAL PRIMARY KEY, user TEXT, password TEXT)")
        dbm.dbexecute("CREATE TABLE IF NOT EXISTS cloudnet_rest_permission (id SERIAL PRIMARY KEY, user TEXT, permission TEXT)")
        dbm.dbexecute("CREATE TABLE IF NOT EXISTS cloudnet_rest_auths (id SERIAL PRIMARY KEY, type TEXT, value TEXT, timestamp TEXT)")
    }
    @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STARTED)
    fun started(
        @NotNull cloudServiceManager: CloudServiceManager,
        @NotNull shutdownHandler: ShutdownHandler,
        @NotNull @Named("module") injectionLayer: InjectionLayer<*>
    ) {
        println("Decoded: ${Base64.getDecoder().decode("amV0YnJhaW5zOmZvb2Jhcg")}")


        I18n.loadFromLangPath(CloudNet_Rest_Module::class.java)
        GlobalScope.launch {
            main(cloudServiceManager, shutdownHandler)
        }
        println("Rest API listening on port ${configuration!!.restapi_port.toString()}!")
    }
    @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
    fun start(commandProvider: CommandProvider) {
        commandProvider.register(rest::class.java)
    }
    @ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
    fun stop() {
        val dbm = DBManager()
        println("Closing DB connection!")
        dbm.closedb()
        println("Closed DB connection!")
    }


    private fun services(cloudServiceManager: CloudServiceManager): JSONObject {
        val ser = cloudServiceManager.services()
        val servicesArray = JSONArray()
        ser.forEach { service ->
            val serviceObject = JSONObject()
            serviceObject.put("Name", service.name())
            val addressObject = JSONObject()
            addressObject.put("host", service.address().host)
            addressObject.put("port", service.address().port)
            serviceObject.put("Address", addressObject)
            serviceObject.put("Connected", service.connected())
            serviceObject.put("LifeCycle", service.lifeCycle())
            serviceObject.put("CreationTime", service.creationTime())
            serviceObject.put("ConnectedTime", service.connectedTime())
            servicesArray.put(serviceObject)
        }
        val result = JSONObject()
        result.put("services", servicesArray)
        return result
    }

    private fun main(
        @NotNull cloudServiceManager: CloudServiceManager,
        @NotNull shutdownHandler: ShutdownHandler
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
                        if (credentials.name == "jetbrains" && credentials.password == "foobar") {
                            UserIdPrincipal(credentials.name)
                        } else {
                            null
                        }
                    }


                }
            }


            routing {

                //swaggerUI(path = "swagger", swaggerFile = "openapi/swagger.yaml")


                authenticate("auth-basic") {
                    get("/auth") {
                        call.respondText("You have reached the auth site!")
                    }
                }



                get("/") {

                    call.respondText("Welcome to my Rest API!")
                }
                get("/services") {
                    val services = services(cloudServiceManager)
                    call.respondText(
                        services.toString(4)
                            .replace("[", "")
                            .replace("]", "")
                    )
                }

                get("/services/{service}") {
                    val services = cloudServiceManager.services().map { it.name() }.toList()
                    val serv = services.contains(call.parameters["service"])
                    if (serv) {
                        val servout = cloudServiceManager.serviceByName(call.parameters["service"].toString())
                        if (servout != null) {
                            call.respondText("Service: ${servout.name()} \n ServiceID: ${servout.serviceId()} \n Address: ${servout.address()} \n Connected: ${servout.connected()} \n ConnectedTime: ${servout.connectedTime()} \n CreationTime: ${servout.creationTime()} \n LifeCycle: ${servout.lifeCycle()} \n Provider: ${servout.provider()}")
                        }
                    } else {
                        call.respondText("Du Mensch, es gibt diesen Service NICHT! Wasn vollidiot!")
                    }
                }

                get("/node/shutdown") {
                    call.respondText("NOTE: The Cloud is shutting down!")
                    shutdownHandler.shutdown()
                }


            }
        }.start(wait = false)
    }
}