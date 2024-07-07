package io.github.thecguy.cloudnet_rest_module.utli

import eu.cloudnetservice.driver.provider.ServiceTaskProvider
import eu.cloudnetservice.node.service.CloudServiceManager
import kong.unirest.core.json.JSONArray
import kong.unirest.core.json.JSONObject
import org.jetbrains.annotations.NotNull
import java.util.*


class JsonUtils internal constructor() {
    private val authUtil = AuthUtil()
    private val dbManager = DBManager()

    init {

    }

     fun services(cloudServiceManager: CloudServiceManager): JSONObject {
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

    fun service(cloudServiceManager: CloudServiceManager, @NotNull service: String): JSONObject {
        val ser = cloudServiceManager.serviceByName(service)
        val serv = JSONObject()
        if (ser != null) {
            serv.put("name", ser.name())
            println("1")
            val addressObject = JSONObject()
            addressObject.put("host", ser.address().host)
            addressObject.put("port", ser.address().port)
            serv.put("address", addressObject)
            println("2")
            serv.put("connected", ser.connected())
            serv.put("connectedTime", ser.connectedTime())
            serv.put("lifeCycle", ser.lifeCycle())
            serv.put("creationTime", ser.creationTime())
            println("3")
            val configuration = JSONObject()
            configuration.put("staticService", ser.configuration().staticService())
            configuration.put("autoDeleteOnStop", ser.configuration().autoDeleteOnStop())
            configuration.put("groups", ser.configuration().groups())
            configuration.put("runtime", ser.configuration().runtime())
            serv.put("configuration", configuration)
            println("4")
            val processConfig = JSONObject()
            processConfig.put("jvmOptions", ser.configuration().processConfig().jvmOptions)
            processConfig.put("environment", ser.configuration().processConfig().environment)
            processConfig.put("maxHeapMemorySize", ser.configuration().processConfig().maxHeapMemorySize)
            processConfig.put("processParameters", ser.configuration().processConfig().processParameters)
            processConfig.put("environmentVariables", ser.configuration().processConfig().environmentVariables)
            serv.put("processConfig", processConfig)
            println("5")
            val processSnapshot = JSONObject()
            processSnapshot.put("pid", ser.processSnapshot().pid)
            processSnapshot.put("threads", ser.processSnapshot().threads)
            processSnapshot.put("cpuUsage", ser.processSnapshot().cpuUsage)
            processSnapshot.put("maxHeapMemory", ser.processSnapshot().maxHeapMemory)
            processSnapshot.put("currentLoadedClassCount", ser.processSnapshot().currentLoadedClassCount)
            processSnapshot.put("heapUsageMemory", ser.processSnapshot().heapUsageMemory)
            processSnapshot.put("noHeapUsageMemory", ser.processSnapshot().noHeapUsageMemory)
            processSnapshot.put("systemCpuUsage", ser.processSnapshot().systemCpuUsage)
            processSnapshot.put("unloadedClassCount", ser.processSnapshot().unloadedClassCount)
            serv.put("processSnapshot", processSnapshot)
            println("6")
        }
        println("7")
        println(serv.toString(4))

        return serv
    }

    fun tasks(serviceTaskProvider: ServiceTaskProvider):JSONObject {
        val taskT = serviceTaskProvider.serviceTasks()
        val tasksArray = JSONArray()
        taskT.forEach { task ->
            val jTask = JSONObject()
            jTask.put("name", task.name())
            jTask.put("groups", task.groups())
            jTask.put("runtime", task.runtime())
            jTask.put("autoDeleteOnStop", task.autoDeleteOnStop())
            jTask.put("jvmOptions", task.jvmOptions())
            jTask.put("associatedNodes", task.associatedNodes())
            jTask.put("hostAddress", task.hostAddress())
            jTask.put("javaCommand", task.javaCommand())
            jTask.put("maintenance", task.maintenance())
            jTask.put("minServiceCount", task.minServiceCount())
            jTask.put("nameSplitter", task.nameSplitter())
            jTask.put("startPort", task.startPort())
            jTask.put("staticServices", task.staticServices())

            val processConfiguration = JSONObject()
            processConfiguration.put("environment", task.processConfiguration().environment)
            processConfiguration.put("jvmOptions", task.processConfiguration().jvmOptions)
            processConfiguration.put("processParameters", task.processConfiguration().processParameters)
            processConfiguration.put("maxHeapMemorySize", task.processConfiguration().maxHeapMemorySize)
            processConfiguration.put("environmentVariables", task.processConfiguration().environmentVariables)
            jTask.put("processConfiguration", processConfiguration)


            tasksArray.put(jTask)
        }
        val result = JSONObject()
        result.put("tasks", tasksArray)
        return result
    }



    fun token():JSONObject {
        val token = JSONObject()
        val ttoken = authUtil.generateToken(256)
        val date = Date(System.currentTimeMillis() + 600000)
        val tokens = dbManager.tokens()
        if (tokens.contains(ttoken)) {
            token.put("Error", "Please try again!")
        } else {
            token.put("token", ttoken)
            token.put("expire_date", date)
            dbManager.dbexecute("INSERT INTO cloudnet_rest_auths (type, value, timestamp) VALUES ('QUERY', '$ttoken', '$date')")
        }
        println(dbManager.tokensDate())
        return token
    }

    companion object {
        private var instance: JsonUtils? = null
        fun getInstance(): JsonUtils {
            if (instance == null) {
                instance = JsonUtils()
            }
            return instance!!
        }

    }
}