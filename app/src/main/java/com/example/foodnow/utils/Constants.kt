package com.example.foodnow.utils

object Constants {
    private const val IP_ADDRESS = "192.168.1.8"
    private const val PORT = "8080"
    
    const val BASE_URL = "http://$IP_ADDRESS:$PORT/"
    const val WS_URL = "ws://$IP_ADDRESS:$PORT/ws-foodnow/websocket"

    /**
     * Returns the full URL for an image path.
     * If the path is already a full URL, it returns it as is.
     */
    fun getFullImageUrl(path: String?): String? {
        if (path.isNullOrEmpty()) return null
        return if (path.startsWith("http")) path else "http://$IP_ADDRESS:$PORT$path"
    }
}
