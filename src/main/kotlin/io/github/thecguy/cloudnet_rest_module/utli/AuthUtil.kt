package io.github.thecguy.cloudnet_rest_module.utli


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.security.SecureRandom


class AuthUtil internal constructor() {
    private val dbManager = DBManager()
    private val Logger: Logger = LoggerFactory.getLogger(AuthUtil::class.java)


    init {
        println("init success!")
    }


    fun generateToken(length: Int): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789/*-_!"
        val random = SecureRandom()
        val token = StringBuilder(length)

        for (i in 0 until length) {
            val index = random.nextInt(characters.length)
            token.append(characters[index])
        }

        return token.toString()
    }

    fun authUser(user: String, permission: String) {

    }

    fun authToken(token: String, permission: String): Boolean {
        Logger.info(token)
        return if (validToken(token)) {
            val user = dbManager.tokenToUser(token)
            val perms = dbManager.cmd_rest_perms(user)
            if (perms.contains(permission) || perms.contains("*")) {
                true
            } else {
                false
            }
        } else {
            false
        }
    }

    fun validToken(token: String): Boolean {
        val tokens = dbManager.tokens()
        Logger.info(tokens.toString())
        return tokens.contains(token)
    }

    companion object {
        private var instance: AuthUtil? = null
        fun getInstance(): AuthUtil {
            if (instance == null) {
                instance = AuthUtil()
            }
            return instance!!
        }

    }
}