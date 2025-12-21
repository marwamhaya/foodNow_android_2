package com.example.foodnow.ui.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodnow.data.MenuItemResponse
import com.example.foodnow.data.Repository
import com.example.foodnow.utils.CartManager
import kotlinx.coroutines.launch
import com.example.foodnow.data.Order
import com.example.foodnow.data.OrderItemRequest
import com.example.foodnow.data.OrderRequest

class MenuViewModel(private val repository: Repository) : ViewModel() {

    private val _menuItems = MutableLiveData<Result<List<MenuItemResponse>>>()
    val menuItems: LiveData<Result<List<MenuItemResponse>>> = _menuItems

    fun loadMenu(restaurantId: Long) {
        viewModelScope.launch {
            try {
                val response = repository.getMenuItems(restaurantId)
                if (response.isSuccessful && response.body() != null) {
                    _menuItems.value = Result.success(response.body()!!)
                } else {
                    _menuItems.value = Result.failure(Exception("Failed to load menu: ${response.code()}"))
                }
            } catch (e: Exception) {
                _menuItems.value = Result.failure(e)
            }
        }
    }

    // Cart logic delegated to CartManager

    private val _restaurantDetails = MutableLiveData<Result<com.example.foodnow.data.RestaurantResponse>>()
    val restaurantDetails: LiveData<Result<com.example.foodnow.data.RestaurantResponse>> = _restaurantDetails

    fun loadRestaurantDetails(restaurantId: Long) {
        viewModelScope.launch {
            try {
                val response = repository.getRestaurantById(restaurantId)
                if (response.isSuccessful && response.body() != null) {
                    _restaurantDetails.value = Result.success(response.body()!!)
                } else {
                    _restaurantDetails.value = Result.failure(Exception("Failed to load restaurant details: ${response.code()}"))
                }
            } catch (e: Exception) {
                _restaurantDetails.value = Result.failure(e)
            }
        }
    }

    private val _restaurantReviews = MutableLiveData<Result<List<com.example.foodnow.data.RestaurantRatingResponse>>>()
    val restaurantReviews: LiveData<Result<List<com.example.foodnow.data.RestaurantRatingResponse>>> = _restaurantReviews

    fun loadRestaurantReviews(restaurantId: Long) {
        viewModelScope.launch {
            _restaurantReviews.value = repository.getRestaurantReviews(restaurantId)
        }
    }

    private val _orderResult = MutableLiveData<Result<Order>>()
    val orderResult: LiveData<Result<Order>> = _orderResult

    fun placeOrder(restaurantId: Long, latitude: Double, longitude: Double) {
        val items = CartManager.getOrderRequests()
        if (items.isEmpty()) return

        viewModelScope.launch {
            try {
                // Pass location in the order request itself (in case backend expects it there)
                val request = OrderRequest(restaurantId, items, "Default Delivery Address", latitude, longitude)
                val response = repository.createOrder(request)
                if (response.isSuccessful && response.body() != null) {
                    val order = response.body()!!
                    
                    // Save client GPS location for this order
                    try {
                        val locationDto = com.example.foodnow.data.LocationUpdateDto(latitude, longitude)
                        val locResponse = repository.saveOrderLocation(order.id, locationDto)
                        if (!locResponse.isSuccessful) {
                             android.util.Log.e("MenuViewModel", "Failed to save location. Code: ${locResponse.code()}, Error: ${locResponse.errorBody()?.string()}")
                             // Ideally we could surface this warning to the user, but for now we Log it.
                        } else {
                             android.util.Log.d("MenuViewModel", "Location saved successfully for Order ${order.id}")
                        }
                    } catch (e: Exception) {
                        // Log but don't fail the order if location save fails
                        android.util.Log.e("MenuViewModel", "Failed to save order location", e)
                    }
                    
                    _orderResult.value = Result.success(order)
                    CartManager.clearCart() // Clear global cart
                } else {
                    _orderResult.value = Result.failure(Exception("Failed to place order: ${response.code()}"))
                }
            } catch (e: Exception) {
                _orderResult.value = Result.failure(e)
            }
        }
    }

    private val _paymentResult = MutableLiveData<Result<com.example.foodnow.data.PaymentResponse>>()
    val paymentResult: LiveData<Result<com.example.foodnow.data.PaymentResponse>> = _paymentResult

    fun processPayment(amount: java.math.BigDecimal, method: String) {
        viewModelScope.launch {
            try {
                val request = com.example.foodnow.data.PaymentRequest(amount, method)
                val response = repository.simulatePayment(request)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.status == "SUCCESS") {
                        _paymentResult.value = Result.success(body)
                    } else {
                        _paymentResult.value = Result.failure(Exception(body.message))
                    }
                } else {
                    _paymentResult.value = Result.failure(Exception("Payment failed: ${response.code()}"))
                }
            } catch (e: Exception) {
                _paymentResult.value = Result.failure(e)
            }
        }
    }
    private val _userProfile = MutableLiveData<Result<com.example.foodnow.data.AuthResponse>>()
    val userProfile: LiveData<Result<com.example.foodnow.data.AuthResponse>> = _userProfile

    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val response = repository.getUserProfile()
                if (response.isSuccessful && response.body() != null) {
                    _userProfile.value = Result.success(response.body()!!)
                } else {
                    _userProfile.value = Result.failure(Exception("Failed to load profile: ${response.code()}"))
                }
            } catch (e: Exception) {
                _userProfile.value = Result.failure(e)
            }
        }
    }
}
