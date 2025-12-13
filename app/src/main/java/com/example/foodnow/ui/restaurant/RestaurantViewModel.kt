package com.example.foodnow.ui.restaurant

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodnow.data.MenuItemRequest
import com.example.foodnow.data.Order
import com.example.foodnow.data.Repository
import com.example.foodnow.data.RestaurantResponse
import kotlinx.coroutines.launch

class RestaurantViewModel(private val repository: Repository) : ViewModel() {

    private val _restaurant = MutableLiveData<Result<RestaurantResponse>>()
    val restaurant: LiveData<Result<RestaurantResponse>> = _restaurant

    private val _orders = MutableLiveData<Result<List<Order>>>()
    val orders: LiveData<Result<List<Order>>> = _orders

    private val _menuItems = MutableLiveData<Result<List<com.example.foodnow.data.MenuItemResponse>>>()
    val menuItems: LiveData<Result<List<com.example.foodnow.data.MenuItemResponse>>> = _menuItems

    var currentRestaurantId: Long? = null

    fun getMyRestaurant() {
        viewModelScope.launch {
            try {
                val response = repository.getMyRestaurant()
                if (response.isSuccessful && response.body() != null) {
                    val rest = response.body()!!
                    currentRestaurantId = rest.id
                    _restaurant.value = Result.success(rest)
                    // Auto load menu
                    getMenuItems()
                } else {
                    _restaurant.value = Result.failure(Exception("Error fetching restaurant: ${response.code()}"))
                }
            } catch (e: Exception) {
                _restaurant.value = Result.failure(e)
            }
        }
    }

    fun getMenuItems() {
        if (currentRestaurantId == null) return
        viewModelScope.launch {
            try {
                val response = repository.getMenuItems(currentRestaurantId!!)
                if (response.isSuccessful && response.body() != null) {
                    _menuItems.value = Result.success(response.body()!!)
                }
            } catch (e: Exception) {
                 _menuItems.value = Result.failure(e)
            }
        }
    }

    fun createMenuItem(request: MenuItemRequest) {
        viewModelScope.launch {
            try {
                val response = repository.createMenuItem(request)
                if (response.isSuccessful) {
                    getMenuItems() // Refresh
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun deleteMenuItem(id: Long) {
        viewModelScope.launch {
             repository.deleteMenuItem(id)
             getMenuItems()
        }
    }

    fun getOrders() {
        viewModelScope.launch {
            try {
                // Currently fetching all orders, backend supports filtering by status if needed
                val response = repository.getMyRestaurantOrders()
                if (response.isSuccessful && response.body() != null) {
                    _orders.value = Result.success(response.body()!!.content)
                } else {
                    _orders.value = Result.failure(Exception("Error fetching orders: ${response.code()}"))
                }
            } catch (e: Exception) {
                _orders.value = Result.failure(e)
            }
        }
    }

    fun acceptOrder(orderId: Long) {
        viewModelScope.launch {
            repository.acceptOrder(orderId)
            getOrders() // Refresh
        }
    }

    fun prepareOrder(orderId: Long) {
        viewModelScope.launch {
            repository.prepareOrder(orderId)
            getOrders()
        }
    }

    fun readyOrder(orderId: Long) {
        viewModelScope.launch {
            repository.readyOrder(orderId)
            getOrders()
        }
    }

    fun rejectOrder(orderId: Long, reason: String) {
        viewModelScope.launch {
            repository.rejectOrder(orderId, reason)
            getOrders()
        }
    }
}
