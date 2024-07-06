package io.github.thecguy.cloudnet_rest_module.utli

import eu.cloudnetservice.node.service.CloudServiceManager
import kong.unirest.core.json.JSONArray
import kong.unirest.core.json.JSONObject
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