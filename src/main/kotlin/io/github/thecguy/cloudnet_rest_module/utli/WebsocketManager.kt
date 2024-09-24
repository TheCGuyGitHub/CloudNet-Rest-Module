package io.github.thecguy.cloudnet_rest_module.utli

import io.ktor.websocket.*
import java.util.concurrent.ConcurrentHashMap

class WebsocketManager {

    object WebSocketManager {
        // Store sessions by service name
        private val serviceSessions: MutableMap<String, MutableSet<DefaultWebSocketSession>> = ConcurrentHashMap()

        // Add a WebSocket session for a specific service
        fun addSession(service: String, session: DefaultWebSocketSession) {
            serviceSessions.computeIfAbsent(service) { ConcurrentHashMap.newKeySet() }.add(session)
        }

        // Remove a WebSocket session for a specific service
        fun removeSession(service: String, session: DefaultWebSocketSession) {
            serviceSessions[service]?.remove(session)
            if (serviceSessions[service]?.isEmpty() == true) {
                serviceSessions.remove(service)  // Clean up if no sessions are left
            }
        }

        // Send message to all open WebSocket sessions for a specific service
        suspend fun broadcast(service: String, message: String) {
            serviceSessions[service]?.forEach { session ->
                try {
                    session.send(message)
                } catch (e: Exception) {
                    e.printStackTrace() // Handle errors during message sending
                }
            }
        }
    }
}