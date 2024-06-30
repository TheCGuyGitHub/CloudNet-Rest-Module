package io.github.thecguy.cloudnet_rest_module



import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import eu.cloudnetservice.common.log.LogManager
import eu.cloudnetservice.common.log.Logger
import eu.cloudnetservice.driver.document.Document
import eu.cloudnetservice.driver.document.DocumentFactory
import eu.cloudnetservice.driver.inject.InjectionLayer
import eu.cloudnetservice.driver.module.ModuleLifeCycle
import eu.cloudnetservice.driver.module.ModuleTask
import eu.cloudnetservice.driver.module.driver.DriverModule
import eu.cloudnetservice.node.ShutdownHandler
import eu.cloudnetservice.node.command.CommandProvider
import eu.cloudnetservice.node.service.CloudServiceManager
import io.github.thecguy.cloudnet_rest_module.commands.Test
import io.github.thecguy.cloudnet_rest_module.config.Configuration
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import jakarta.inject.Named
import jakarta.inject.Singleton
import kong.unirest.core.json.JSONArray
import kong.unirest.core.json.JSONObject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull
import kotlin.concurrent.Volatile


@Singleton
class CloudNet_Rest_Module : DriverModule() {

    @Volatile
    private var configuration: Configuration? = null
    private val logger: Logger = LogManager.logger(CloudNet_Rest_Module::class.java)






    @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
    fun started(
        @NotNull cloudServiceManager: CloudServiceManager,
        @NotNull shutdownHandler: ShutdownHandler,
        @NotNull @Named("module") injectionLayer: InjectionLayer<*>
    ) {
        logger.info("Listening on port 8080!")
        GlobalScope.launch {
            main(cloudServiceManager, shutdownHandler)
        }
    }

    @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.LOADED)
    fun load() {
        val config = this.readConfig(DocumentFactory.json())
        this.writeConfig(
            Document.newJsonDocument().appendTree(
                Configuration(
                    config.getString("username"),
                    config.getString("password"),
                    config.getString("database"),
                    config.getString("host"),
                    config.getInt("port")
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
                    3306
                )
            },
            DocumentFactory.json()
        )

        val config = HikariConfig()

        config.jdbcUrl = "jdbc:mysql://${configuration!!.host}:${configuration!!.port}/${configuration!!.database}"
        config.username = configuration!!.username
        config.password = configuration!!.password
        config.driverClassName = "com.mysql.cj.jdbc.Driver"
        config.addDataSourceProperty("cachePrepStmts", "true")
        config.addDataSourceProperty("prepStmtCacheSize", "250")
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

        val ds = HikariDataSource(config)

        ds.connection.use { connection ->
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS cloudnet_rest_users (id SERIAL PRIMARY KEY, user TEXT, password TEXT)").use { statement ->
                statement.executeUpdate()
            }
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS cloudnet_rest_permission (id SERIAL PRIMARY KEY, user TEXT, permission TEXT)").use { statement ->
                statement.executeUpdate()
            }
            connection.prepareStatement("CREATE TABLE IF NOT EXISTS cloudnet_rest_auths (id SERIAL PRIMARY KEY, type TEXT, value TEXT, timestamp TEXT)").use { statement ->
                statement.executeUpdate()
            }
        }
        ds.close()
    }

    @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
    fun start(commandProvider: CommandProvider) {
        commandProvider.register(Test::class.java)
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
        embeddedServer(Netty, port = 8080) {

            install(ShutDownUrl.ApplicationCallPlugin) {
                shutDownUrl = "/debug/shutdown"
                exitCodeSupplier = { 0 }
            }



            routing {

                //swaggerUI(path = "swagger", swaggerFile = "openapi/swagger.yaml")

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