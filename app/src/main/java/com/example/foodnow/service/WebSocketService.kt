package com.example.foodnow.service

import android.annotation.SuppressLint
import android.util.Log
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import ua.naiksoftware.stomp.Stomp
import ua.naiksoftware.stomp.StompClient
import ua.naiksoftware.stomp.dto.LifecycleEvent

object WebSocketService {

    private const val TAG = "WebSocketService"
    // Use Constants for the WebSocket URL
    private val WS_URL = com.example.foodnow.utils.Constants.WS_URL

    private var stompClient: StompClient? = null
    private val compositeDisposable = CompositeDisposable()

    fun connect(context: android.content.Context) {
        if (stompClient != null && stompClient!!.isConnected) return

        // Get auth token
        val tokenManager = com.example.foodnow.data.TokenManager(context)
        val token = tokenManager.getToken()

        if (token.isNullOrEmpty()) {
            Log.e(TAG, "No auth token available for WebSocket connection")
            return
        }

        // Interceptor : Ajoute automatiquement le header d'authentification à chaque requête WebSocket
        val client = okhttp3.OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(request)
            }
            .build()
        //Crée le client STOMP avec OkHttp comme transport
        stompClient = Stomp.over(Stomp.ConnectionProvider.OKHTTP, WS_URL, null, client)
        //subscribeOn(Schedulers.io()) : Exécute sur thread background
        //observeOn(AndroidSchedulers.mainThread()) : Reçoit les résultats sur le thread UI
        //subscribe : Abonne à l'événement de la connexion
        stompClient?.lifecycle()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({ lifecycleEvent ->
                when (lifecycleEvent.type) {
                    LifecycleEvent.Type.OPENED -> Log.d(TAG, "Stomp connection opened")
                    LifecycleEvent.Type.ERROR -> Log.e(TAG, "Stomp connection error", lifecycleEvent.exception)
                    LifecycleEvent.Type.CLOSED -> Log.d(TAG, "Stomp connection closed")
                    else -> Log.d(TAG, "Stomp event: ${lifecycleEvent.message}")
                }
            }, { error ->
                Log.e(TAG, "Lifecycle subscription error", error)
            })?.let { compositeDisposable.add(it) }
            //Lance la connexion WebSocket
        stompClient?.connect()
    }
    //Ferme la connexion WebSocket
    fun disconnect() {
        try {
            //Ferme la connexion WebSocket
            stompClient?.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "Error during disconnect", e)
        }
        //Supprime les abonnements
        compositeDisposable.clear()
        stompClient = null
    }

    @SuppressLint("CheckResult")
    fun sendLocation(orderId: Long, latitude: Double, longitude: Double) {
        if (stompClient == null || !stompClient!!.isConnected) return
        //Crée un JSON avec les coordonnées
        val json = "{\"latitude\": $latitude, \"longitude\": $longitude}"
        //Envoie la localisation
        //Exécute sur thread background
        //Reçoit les résultats sur le thread UI
        stompClient?.send("/app/delivery/$orderId/location", json)
            ?.compose { upstream -> upstream.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()) }
            ?.subscribe(
                { Log.d(TAG, "Location sent for order $orderId") },
                { t -> Log.e(TAG, "Error sending location", t) }
            )
    }

    fun subscribeToDeliveryLocation(context: android.content.Context, orderId: Long, onLocationReceived: (Double, Double) -> Unit) {
        if (stompClient == null || !stompClient!!.isConnected) connect(context)

        stompClient?.topic("/topic/delivery/$orderId/location")
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({ topicMessage ->
                try {
                    // Simple parsing, assuming valid JSON
                    // Use Gson if available or manual string parsing
                    val payload = topicMessage.payload
                    // format: {"latitude": 12.34, "longitude": 56.78}
                    val lat = payload.substringAfter("\"latitude\":").substringBefore(",").trim().toDouble()
                    val lon = payload.substringAfter("\"longitude\":").substringBefore("}").trim().toDouble()
                    onLocationReceived(lat, lon)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing location message", e)
                }
            }, { t ->
                Log.e(TAG, "Error on subscription", t)
            })?.let { compositeDisposable.add(it) }
    }
    
    fun subscribeToDeliveryStatus(context: android.content.Context, orderId: Long, onStatusReceived: (String) -> Unit) {
         if (stompClient == null || !stompClient!!.isConnected) connect(context)

        stompClient?.topic("/topic/delivery/$orderId/status")
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({ topicMessage ->
                try {
                    // Extract status from JSON response
                    val payload = topicMessage.payload
                    // DeliveryResponse JSON
                    // "status": "ON_THE_WAY"
                    val status = payload.substringAfter("\"status\":\"").substringBefore("\"")
                    onStatusReceived(status)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing status message", e)
                }
            }, { t ->
                Log.e(TAG, "Error on status subscription", t)
            })?.let { compositeDisposable.add(it) }
    }
}
