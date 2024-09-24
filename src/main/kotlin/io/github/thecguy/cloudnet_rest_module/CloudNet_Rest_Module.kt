package io.github.thecguy.cloudnet_rest_module




import eu.cloudnetservice.common.language.I18n
import eu.cloudnetservice.driver.document.Document
import eu.cloudnetservice.driver.document.DocumentFactory
import eu.cloudnetservice.driver.event.EventListener
import eu.cloudnetservice.driver.event.events.service.CloudServiceLogEntryEvent
import eu.cloudnetservice.driver.inject.InjectionLayer
import eu.cloudnetservice.driver.module.ModuleLifeCycle
import eu.cloudnetservice.driver.module.ModuleTask
import eu.cloudnetservice.driver.module.driver.DriverModule
import eu.cloudnetservice.driver.network.http.websocket.WebSocketFrameType
import eu.cloudnetservice.driver.provider.ServiceTaskProvider
import eu.cloudnetservice.node.ShutdownHandler
import eu.cloudnetservice.node.cluster.NodeServerProvider
import eu.cloudnetservice.node.command.CommandProvider
import eu.cloudnetservice.node.service.CloudServiceManager
import eu.cloudnetservice.node.service.ServiceConsoleLineHandler
import eu.cloudnetservice.node.service.ServiceConsoleLogCache
import io.github.thecguy.cloudnet_rest_module.commands.rest
import io.github.thecguy.cloudnet_rest_module.config.Configuration
import io.github.thecguy.cloudnet_rest_module.coroutines.AuthChecker
import io.github.thecguy.cloudnet_rest_module.utli.AuthUtil
import io.github.thecguy.cloudnet_rest_module.utli.DBManager
import io.github.thecguy.cloudnet_rest_module.utli.JsonUtils
import io.github.thecguy.cloudnet_rest_module.utli.WebsocketManager
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.swagger.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import jakarta.inject.Named
import jakarta.inject.Singleton
import kong.unirest.core.json.JSONArray
import kong.unirest.core.json.JSONObject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import org.jetbrains.annotations.NotNull
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*


@Singleton
class CloudNet_Rest_Module : DriverModule() {
    private val Logger: Logger = LoggerFactory.getLogger(CloudNet_Rest_Module::class.java)
    private val dbm = DBManager()
    private val jsonUtils = JsonUtils()
    private val authChecker = AuthChecker()
    private val authUtil = AuthUtil()
    private val websocketManager = WebsocketManager()
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
        println("dsym74")
         configuration = this.readConfig(
            Configuration::class.java,
            {
                Configuration(
                    "root",
                    "123456",
                    "cloudnet_rest",
                    "127.0.0.1",
                    3306,
                    2412
                )
            },
            DocumentFactory.json()
        )

        dbm.dbexecute("CREATE TABLE IF NOT EXISTS cloudnet_rest_users (id SERIAL PRIMARY KEY, user TEXT, password TEXT)")
        dbm.dbexecute("CREATE TABLE IF NOT EXISTS cloudnet_rest_permission (id SERIAL PRIMARY KEY, user TEXT, permission TEXT)")
        dbm.dbexecute("CREATE TABLE IF NOT EXISTS cloudnet_rest_auths (id SERIAL PRIMARY KEY, type TEXT, value TEXT, timestamp TEXT, user TEXT)")
        dbm.dbexecute("DELETE FROM cloudnet_rest_auths")
        authChecker.schedule()
    }
    @OptIn(DelicateCoroutinesApi::class)
    @ModuleTask(order = 127, lifecycle = ModuleLifeCycle.STARTED)
    fun started(
        @NotNull cloudServiceManager: CloudServiceManager,
        @NotNull shutdownHandler: ShutdownHandler,
        @NotNull serviceTaskProvider: ServiceTaskProvider,
        @NotNull nodeServerProvider: NodeServerProvider,
        @NotNull @Named("module") injectionLayer: InjectionLayer<*>
    ) {


        I18n.loadFromLangPath(CloudNet_Rest_Module::class.java)
        GlobalScope.launch {
            main(cloudServiceManager, shutdownHandler, serviceTaskProvider, nodeServerProvider)
        }
        Logger.info("RestAPI listening on port ${configuration!!.restapi_port}")
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

    @OptIn(DelicateCoroutinesApi::class)
    @EventListener
    fun cloudServiceLogEntryEvent(event: CloudServiceLogEntryEvent) {
        // Extract the service name from the event
        val serviceName = event.serviceInfo().name()

        // Get the log message or any other relevant data from the event
        val message = event.line()

        // Broadcast the message to WebSocket sessions listening to this service
        GlobalScope.launch {
            println("Sending message: $message to service: $serviceName")
            WebsocketManager.WebSocketManager.broadcast(serviceName, message)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun main(
        @NotNull cloudServiceManager: CloudServiceManager,
        @NotNull shutdownHandler: ShutdownHandler,
        @NotNull serviceTaskProvider: ServiceTaskProvider,
        @NotNull nodeServerProvider: NodeServerProvider
    ) {
        val port = configuration!!.restapi_port
        embeddedServer(Netty, port = port, host = "0.0.0.0") {

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
            install(WebSockets) {
                pingPeriod = Duration.ofSeconds(15)
                timeout = Duration.ofSeconds(15)
                maxFrameSize = Long.MAX_VALUE
                masking = false
            }
            //install(CORS) {

            //    anyHost()

            //    allowMethod(HttpMethod.Get)
            //    allowMethod(HttpMethod.Put)
            //    allowMethod(HttpMethod.Post)
            //    allowMethod(HttpMethod.Options)
            //    allowMethod(HttpMethod.Patch)
            //    allowMethod(HttpMethod.Delete)
            //    allowMethod(HttpMethod.Head)

            //    allowHeader(HttpHeaders.Upgrade)

            //    allowHeader(HttpHeaders.SecWebSocketProtocol)
            //    allowHeader(HttpHeaders.Authorization)
            //    allowHeader(HttpHeaders.Accept)
            //    allowHeader(HttpHeaders.ContentType)

            //    allowCredentials = true
            //    allowSameOrigin = true

            //}

            routing {
                swaggerUI(path = "swagger", swaggerFile = "openapi/swagger.yaml")



                //      _     _   _  _____  _   _
                //     / \   | | | ||_   _|| | | |
                //    / _ \  | | | |  | |  | |_| |
                //   / ___ \ | |_| |  | |  |  _  |
                //  /_/   \_\ \___/   |_|  |_| |_|


                authenticate("auth-basic") {
                    post("api/v1/auth") {
                        call.respond(jsonUtils.token(call.principal<UserIdPrincipal>()?.name.toString()).toString(4))
                    }
                }
                get("api/v1/auth/verify") {
                    val token = call.request.headers["Authorization"]
                    if (authUtil.validToken(token.toString())) {
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }
                get("api/v1/auth/revoke") {
                    val token = call.request.headers["Authorization"]
                    if (authUtil.validToken(token.toString())) {
                        dbm.dbexecute("DELETE FROM cloudnet_rest_auths WHERE value = '$token'")
                        call.response.status(HttpStatusCode.OK)
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }

                // ____   _____  ____   _____
                // |  _ \ | ____|/ ___| |_   _|
                // | |_) ||  _|  \___ \   | |
                // |  _ < | |___  ___) |  | |
                // |_| \_\|_____||____/   |_|
                get("api/v1/users") {
                    val token = call.request.headers["Authorization"]
                    val usrs = dbm.cmd_rest_users()
                    if (authUtil.authToken(token.toString(), "cloudnet.rest.users")) {
                        val userS = JSONObject()
                        val users = JSONArray()
                        usrs.forEach { user ->
                            users.put(user)
                        }
                        userS.put("users", users)
                        call.respond(userS.toString(4))
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }

                //  _   _   ___   ____   _____
                //  | \ | | / _ \ |  _ \ | ____|
                //  |  \| || | | || | | ||  _|
                //  | |\  || |_| || |_| || |___
                //  |_| \_| \___/ |____/ |_____|

                get("api/v1/node") {
                    val token = call.request.headers["Authorization"]
                    if (authUtil.authToken(token.toString(), "cloudnet.rest.node")) {
                        call.respond(jsonUtils.nodeInfo(nodeServerProvider.localNode().nodeInfoSnapshot()).toString(4))
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }
                get("api/v1/node/ping") {
                    val token = call.request.headers["Authorization"]
                    if (authUtil.authToken(token.toString(), "cloudnet.rest.node.ping")) {
                        call.response.status(HttpStatusCode.NoContent)
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }

                //  ____  _      _   _  ____   _____  _____  ____
                //  / ___|| |    | | | |/ ___| |_   _|| ____||  _ \
                //  | |    | |    | | | |\___ \   | |  |  _|  | |_) |
                //  | |___ | |___ | |_| | ___) |  | |  | |___ |  _ <
                //  \____||_____| \___/ |____/   |_|  |_____||_| \_\

                









                //val token = call.request.headers["Authorization"]
                //if (authUtil.authToken(token.toString(), "cloudnet.rest.services")) {
                //} else {
                //    call.response.status(HttpStatusCode.Unauthorized)
                //}

                //services
                get("api/v1/services") {
                    val services = jsonUtils.services(cloudServiceManager)
                    val token = call.request.headers["Authorization"]
                    if (authUtil.authToken(token.toString(), "cloudnet.rest.services")) {
                        call.respondText(
                            services.toString(4)
                                .replace("[", "")
                                .replace("]", "")
                        )
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }

                }

                get("api/v1/services/{service}") {
                    val services = cloudServiceManager.services().map { it.name() }.toList()
                    val serv = services.contains(call.parameters["service"])

                    val token = call.request.headers["Authorization"]
                    if (authUtil.authToken(token.toString(), "cloudnet.rest.service")) {
                        if (serv) {
                            call.respond(jsonUtils.service(cloudServiceManager, call.parameters["service"].toString()).toString(4))
                        } else {
                            call.response.status(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }

                webSocket("api/v1/services/{service}/liveLog") {
                    val services = cloudServiceManager.services().map { it.name() }.toList()
                    val serv = services.contains(call.parameters["service"])
                    val token = call.parameters["token"]

                    Logger.info(token.toString())

                    if (authUtil.authToken(token.toString(), "cloudnet.rest.service.liveLog")) {
                        Logger.warn("WebSocket - Token valid!")
                    } else {
                        Logger.error("WebSocket - Token invalid!")
                    }

                    if (serv) {
                        Logger.warn("WebSocket - Service valid!")
                    } else {
                        Logger.error("WebSocket - Service invalid!")
                    }

                    if (authUtil.authToken(token.toString(), "cloudnet.rest.service.liveLog")) {

                        if (serv) {


                            val service = call.parameters["service"]

                            val handler: ServiceConsoleLineHandler =
                                ServiceConsoleLineHandler { _: ServiceConsoleLogCache?, line: String?, _: Boolean ->
                                    GlobalScope.launch {
                                        WebsocketManager.WebSocketManager.broadcast(
                                            service!!,
                                            line.toString()
                                        )
                                    }
                                }

                            cloudServiceManager.localCloudService(
                                cloudServiceManager.serviceByName(service!!)!!.serviceId().uniqueId()
                            )!!.serviceConsoleLogCache().addHandler(handler)


                            // Add the session to the WebSocketManager for this specific service
                            WebsocketManager.WebSocketManager.addSession(service, this)

                            try {
                                // Listen for incoming WebSocket frames (if needed)
                                incoming.consumeEach { frame ->
                                    if (frame is Frame.Text) {
                                        val receivedText = frame.readText()
                                        println("Received message from client: $receivedText")
                                    }
                                }
                            } finally {
                                println("Disconnected!")

                                cloudServiceManager.localCloudService(
                                    cloudServiceManager.serviceByName(service)!!.serviceId().uniqueId()
                                )!!.serviceConsoleLogCache().removeHandler(handler)
                                WebsocketManager.WebSocketManager.removeSession(service, this)
                            }
                        } else {
                            println("ForceClose")
                            close(CloseReason(CloseReason.Codes.CANNOT_ACCEPT, "Missing service parameter"))
                        }
                    }
                }








                //tasks
                get("api/v1/tasks") {
                    val token = call.request.headers["Authorization"]
                    if (authUtil.authToken(token.toString(), "cloudnet.rest.tasks")) {
                        call.respond(jsonUtils.tasks(serviceTaskProvider).toString(4))
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }

                get("api/v1/tasks/{task}") {
                    val tasks = serviceTaskProvider.serviceTasks().map { it.name() }.toList()
                    val tas = tasks.contains(call.parameters["task"])

                    val token = call.request.headers["Authorization"]
                    if (authUtil.authToken(token.toString(), "cloudnet.rest.task")) {
                        if (tas) {
                            call.respond(jsonUtils.task(serviceTaskProvider, call.parameters["task"].toString()))
                        } else {
                            call.response.status(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }
                delete("api/v1/tasks/{task}") {
                    val tasks = serviceTaskProvider.serviceTasks().map { it.name() }.toList()
                    val tas = tasks.contains(call.parameters["task"])

                    val token = call.request.headers["Authorization"]
                    if (authUtil.authToken(token.toString(), "cloudnet.rest.task.delete")) {
                        if (tas) {
                            serviceTaskProvider.removeServiceTask(serviceTaskProvider.serviceTask(call.parameters["task"].toString())!!)
                            call.response.status(HttpStatusCode.OK)
                        } else {
                            call.response.status(HttpStatusCode.NotFound)
                        }
                    } else {
                        call.response.status(HttpStatusCode.Unauthorized)
                    }
                }



                get("api/v1/node/shutdown") {
                    call.respondText("NOTE: The Cloud is shutting down!")
                    shutdownHandler.shutdown()
                }


                get("api/v1/") {

                    call.respondText("Welcome to my Rest API!")
                }



            }
        }.start(wait = false)
    }
}