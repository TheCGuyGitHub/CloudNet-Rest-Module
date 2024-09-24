package io.github.thecguy.cloudnet_rest_module.utli

import eu.cloudnetservice.driver.cluster.NodeInfoSnapshot
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

            val addressObject = JSONObject()
            addressObject.put("host", ser.address().host)
            addressObject.put("port", ser.address().port)
            serv.put("address", addressObject)

            serv.put("connected", ser.connected())
            serv.put("connectedTime", ser.connectedTime())
            serv.put("lifeCycle", ser.lifeCycle())
            serv.put("creationTime", ser.creationTime())

            val configuration = JSONObject()
            configuration.put("staticService", ser.configuration().staticService())
            configuration.put("autoDeleteOnStop", ser.configuration().autoDeleteOnStop())
            configuration.put("groups", ser.configuration().groups())
            configuration.put("runtime", ser.configuration().runtime())
            serv.put("configuration", configuration)

            val processConfig = JSONObject()
            processConfig.put("jvmOptions", ser.configuration().processConfig().jvmOptions)
            processConfig.put("environment", ser.configuration().processConfig().environment)
            processConfig.put("maxHeapMemorySize", ser.configuration().processConfig().maxHeapMemorySize)
            processConfig.put("processParameters", ser.configuration().processConfig().processParameters)
            processConfig.put("environmentVariables", ser.configuration().processConfig().environmentVariables)
            serv.put("processConfig", processConfig)

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

        }

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

    fun task(taskProvider: ServiceTaskProvider, vTask: String): JSONObject {
        val task = taskProvider.serviceTask(vTask)
        val jTask = JSONObject()
        if (task != null) {
            jTask.put("name", task.name())
            jTask.put("groups", task.groups())
            jTask.put("runtime", task.runtime())
            jTask.put("startPort", task.startPort())
            jTask.put("associatedNodes", task.associatedNodes())
            jTask.put("autoDeleteOnStop", task.autoDeleteOnStop())
            jTask.put("hostAddress", task.hostAddress())
            jTask.put("javaCommand", task.javaCommand())
            jTask.put("maintenance", task.maintenance())
            jTask.put("minServiceCount", task.minServiceCount())
            jTask.put("nameSplitter", task.nameSplitter())
            jTask.put("staticServices", task.staticServices())

            val processConfiguration = JSONObject()
            processConfiguration.put("environment", task.processConfiguration().environment)
            processConfiguration.put("jvmOptions", task.processConfiguration().jvmOptions)
            processConfiguration.put("processParameters", task.processConfiguration().processParameters)
            processConfiguration.put("maxHeapMemorySize", task.processConfiguration().maxHeapMemorySize)
            processConfiguration.put("environmentVariables", task.processConfiguration().environmentVariables)
            jTask.put("processConfiguration", processConfiguration)
        }

        return jTask
    }

    fun nodeInfo(nodeInfoSnapshot: NodeInfoSnapshot): JSONObject {
        val nodeInfo = JSONObject()

        val nNode = JSONObject()
        nNode.put("listeners", nodeInfoSnapshot.node().listeners())
        nNode.put("uniqueId", nodeInfoSnapshot.node().uniqueId())
        nodeInfo.put("node", nNode)

        nodeInfo.put("modules", nodeInfoSnapshot.modules())
        nodeInfo.put("creationTime", nodeInfoSnapshot.creationTime())
        nodeInfo.put("version", nodeInfoSnapshot.version())
        nodeInfo.put("currentServicesCount", nodeInfoSnapshot.currentServicesCount())
        nodeInfo.put("draining", nodeInfoSnapshot.draining())
        nodeInfo.put("maxMemory", nodeInfoSnapshot.maxMemory())
        nodeInfo.put("maxProcessorUsageToStartServices", nodeInfoSnapshot.maxProcessorUsageToStartServices())
        nodeInfo.put("reservedMemory", nodeInfoSnapshot.reservedMemory())
        nodeInfo.put("startupMillis", nodeInfoSnapshot.startupMillis())
        nodeInfo.put("usedMemory", nodeInfoSnapshot.usedMemory())
        val processSnapshot = JSONObject()
        processSnapshot.put("pid", nodeInfoSnapshot.processSnapshot().pid)
        processSnapshot.put("threads", nodeInfoSnapshot.processSnapshot().threads)
        processSnapshot.put("cpuUsage", nodeInfoSnapshot.processSnapshot().cpuUsage)
        processSnapshot.put("maxHeapMemory", nodeInfoSnapshot.processSnapshot().maxHeapMemory)
        processSnapshot.put("systemCpuUsage", nodeInfoSnapshot.processSnapshot().systemCpuUsage)
        processSnapshot.put("heapUsageMemory", nodeInfoSnapshot.processSnapshot().heapUsageMemory)
        processSnapshot.put("currentLoadedClassCount", nodeInfoSnapshot.processSnapshot().currentLoadedClassCount)
        processSnapshot.put("totalLoadedClassCount", nodeInfoSnapshot.processSnapshot().totalLoadedClassCount)
        processSnapshot.put("unloadedClassCount", nodeInfoSnapshot.processSnapshot().unloadedClassCount)
        nodeInfo.put("processSnapshot", processSnapshot)

        return nodeInfo
    }


    fun token(user: String):JSONObject {
        val token = JSONObject()
        val ttoken = authUtil.generateToken(256)
        val date = Date(System.currentTimeMillis() + 600000)
        val tokens = dbManager.tokens()
        if (tokens.contains(ttoken)) {
            token.put("Error", "Please try again!")
        } else {
            token.put("token", ttoken)
            token.put("expire_date", date)
            dbManager.dbexecute("INSERT INTO cloudnet_rest_auths (type, value, timestamp, user) VALUES ('QUERY', '$ttoken', '$date', '$user')")
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