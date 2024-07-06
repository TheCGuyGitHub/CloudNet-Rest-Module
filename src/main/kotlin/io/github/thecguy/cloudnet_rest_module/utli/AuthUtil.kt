package io.github.thecguy.cloudnet_rest_module.utli

import java.security.SecureRandom


class AuthUtil internal constructor() {


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