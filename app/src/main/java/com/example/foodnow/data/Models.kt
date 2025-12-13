package com.example.foodnow.data

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

// Auth
data class LoginRequest(
    val email: String,
    val password: String
)

data class RegisterRequest(
    val fullName: String,
    val email: String,
    val password: String,
    val role: String = "CLIENT" 
)

data class AuthResponse(
    val token: String,
    val id: Long,
    val email: String,
    val fullName: String,
    val role: String
)

// Restaurant
data class RestaurantResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val address: String?,
    val phone: String?,
    val imageUrl: String?,
    val isActive: Boolean
)

// Menu
data class MenuItemResponse(
    val id: Long,
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val imageUrl: String?,
    val category: String?,
    val isAvailable: Boolean
)

data class MenuItemRequest(
    val name: String,
    val description: String?,
    val price: BigDecimal,
    val imageUrl: String?,
    val category: String?,
    val isAvailable: Boolean
)



data class RestaurantRequest(
    val name: String,
    val address: String,
    val description: String?,
    val phone: String,
    val imageUrl: String?,
    // Admin fields
    val ownerEmail: String,
    val ownerPassword: String,
    val ownerFullName: String,
    val ownerPhoneNumber: String?
)

data class LivreurRequest(
    val userEmail: String,
    val userPassword: String,
    val userFullName: String,
    val userPhoneNumber: String?,
    val vehicleType: String
)

data class User(
    val id: Long,
    val fullName: String,
    val email: String,
    val role: String,
    val isActive: Boolean
)

// Pagination
data class PageResponse<T>(
    val content: List<T>,
    val pageNo: Int,
    val pageSize: Int,
    val totalElements: Long,
    val totalPages: Int,
    val last: Boolean
)
data class Order(
    val id: Long,
    val restaurantName: String, // Assuming backend projection or fetch
    val status: String,
    val totalAmount: BigDecimal,
    val createdAt: String,
    @SerializedName("orderItems")
    val items: List<OrderItem> = emptyList()
)

data class DeliveryResponse(
    val id: Long,
    val orderId: Long,
    val restaurantName: String,
    val restaurantAddress: String,
    val clientName: String,
    val clientAddress: String,
    val clientPhone: String,
    val status: String,
    val pickupTime: String?,
    val deliveryTime: String?,
    val createdAt: String
)

data class LivreurResponse(
    val id: Long,
    val userId: Long,
    val fullName: String,
    val phone: String?,
    val vehicleType: String?,
    val isAvailable: Boolean,
    val isActive: Boolean,
    val latitude: Double?,
    val longitude: Double?
)

data class LocationUpdateDto(
    val latitude: Double,
    val longitude: Double
)

data class OrderItem(
    val id: Long,
    val menuItemName: String,
    val quantity: Int,
    val price: BigDecimal
)
