package io.github.thecguy.cloudnet_rest_module.utli

import java.security.SecureRandom


class AuthUtil internal constructor() {
    val dbManager = DBManager()

    init {

    }


    fun generateToken(length: Int): String {
        val characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789/*-+-_!"
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

    private fun validToken(token: String): Boolean {
        val tokens = dbManager.tokens()
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