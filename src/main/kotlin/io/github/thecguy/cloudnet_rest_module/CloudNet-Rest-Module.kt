package io.github.thecguy.cloudnet_rest_module

import eu.cloudnetservice.driver.inject.InjectionLayer
import eu.cloudnetservice.driver.module.ModuleLifeCycle
import eu.cloudnetservice.driver.module.ModuleTask
import eu.cloudnetservice.driver.module.driver.DriverModule
import eu.cloudnetservice.node.service.CloudServiceManager
import eu.cloudnetservice.node.ShutdownHandler
import eu.cloudnetservice.node.command.CommandProvider
import io.github.thecguy.cloudnet_rest_module.commands.Test
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


@Singleton
class `CloudNet-Rest-Module` : DriverModule() {

    @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
    fun started(
        @NotNull cloudServiceManager: CloudServiceManager,
        @NotNull shutdownHandler: ShutdownHandler,
        @NotNull @Named("module") injectionLayer: InjectionLayer<*>
    ) {

        GlobalScope.launch {
            main(cloudServiceManager,shutdownHandler, injectionLayer)
        }


    }

    @ModuleTask(lifecycle = ModuleLifeCycle.STARTED)
    fun start(commandProvider: CommandProvider) {
        commandProvider.register(Test::class.java)
    }

    @ModuleTask(lifecycle = ModuleLifeCycle.STOPPED)
    fun stop(gracePeriodMillis: Long, timeoutMillis: Long) {
        println("SHUTTING DOWN KTOR!")
    }





    private fun json(cloudServiceManager: CloudServiceManager): JSONObject {
        val ser = cloudServiceManager.services()
        val thing = JSONObject()
        thing.put("", 2)


        return thing
    }

    private fun main(@NotNull cloudServiceManager: CloudServiceManager,
                     @NotNull shutdownHandler: ShutdownHandler,
                     @NotNull @Named("module") injectionLayer: InjectionLayer<*>) {
        embeddedServer(Netty, port = 8080) {

            install(ShutDownUrl.ApplicationCallPlugin) {
                shutDownUrl = "/debug/shutdown"
                exitCodeSupplier = { 0 }
            }



            routing {

                swaggerUI(path = "swagger", swaggerFile = "openapi/swagger.yaml")

                get("/") {

                    call.respondText("Welcome to my Rest API!")
                }
                get("/services") {
                    val services = cloudServiceManager.services()

                    val jsonObject = serviceInfoSnapshot.toJson()
                    println(jsonObject.toString(4))



                    //val json = json(cloudServiceManager)
                    //println(json.toString())
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