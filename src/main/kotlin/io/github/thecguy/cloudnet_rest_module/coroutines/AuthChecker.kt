package io.github.thecguy.cloudnet_rest_module.coroutines

import io.github.thecguy.cloudnet_rest_module.utli.AuthUtil
import io.github.thecguy.cloudnet_rest_module.utli.DBManager
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext

class AuthChecker : CoroutineScope {
    private var job: Job = Job()
    private val dbManager = DBManager()
    private val authUtil = AuthUtil()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default + job

    fun cancel() {
        job.cancel()
    }

    fun schedule() = launch {
        while (true) {
            val tokens = dbManager.tokensDate()
            val dateFormat = SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ENGLISH)
            tokens.forEach { dateString ->
                val date = dateFormat.parse(dateString)
                if (date != null && date.before(Date(System.currentTimeMillis()))) {
                    dbManager.dbexecute("DELETE FROM cloudnet_rest_auths WHERE timestamp = '$dateString'")
                }
            }
            delay(60000)
        }
    }
}
