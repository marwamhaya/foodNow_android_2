package com.example.foodnow.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodnow.data.LivreurRequest
import com.example.foodnow.data.Repository
import com.example.foodnow.data.RestaurantRequest
import com.example.foodnow.data.RestaurantResponse
import com.example.foodnow.data.User
import kotlinx.coroutines.launch

class AdminViewModel(private val repository: Repository) : ViewModel() {

    private val _users = MutableLiveData<Result<List<User>>>()
    val users: LiveData<Result<List<User>>> = _users

    private val _restaurants = MutableLiveData<Result<List<RestaurantResponse>>>()
    val restaurants: LiveData<Result<List<RestaurantResponse>>> = _restaurants

    // Add orders LiveData
    private val _restaurantOrders = MutableLiveData<Result<List<com.example.foodnow.data.Order>>>()
    val restaurantOrders: LiveData<Result<List<com.example.foodnow.data.Order>>> = _restaurantOrders

    fun getRestaurantOrders(restaurantId: Long) {
        viewModelScope.launch {
            try {
                val response = repository.getRestaurantOrders(restaurantId)
                if (response.isSuccessful && response.body() != null) {
                    _restaurantOrders.value = Result.success(response.body()!!.content)
                } else {
                    _restaurantOrders.value = Result.failure(Exception("Error loading orders: ${response.code()}"))
                }
            } catch (e: Exception) {
                 _restaurantOrders.value = Result.failure(e)
            }
        }
    }

    fun getAllUsers() {
        viewModelScope.launch {
            try {
                val response = repository.getAllUsers()
                if (response.isSuccessful && response.body() != null) {
                    _users.value = Result.success(response.body()!!)
                } else {
                    _users.value = Result.failure(Exception("Error loading users: ${response.code()}"))
                }
            } catch (e: Exception) {
                _users.value = Result.failure(e)
            }
        }
    }

    fun getAllRestaurants() {
        viewModelScope.launch {
             try {
                 val response = repository.getAllRestaurantsAdmin()
                 if (response.isSuccessful && response.body() != null) {
                     _restaurants.value = Result.success(response.body()!!)
                 }
             } catch (e: Exception) {
                 _restaurants.value = Result.failure(e)
             }
        }
    }

    fun toggleUserStatus(id: Long) {
        viewModelScope.launch {
            repository.toggleUserStatus(id)
            getAllUsers()
        }
    }

    fun toggleRestaurantStatus(id: Long) {
        viewModelScope.launch {
            repository.toggleRestaurantStatus(id)
            getAllRestaurants()
        }
    }

    fun createRestaurant(request: RestaurantRequest, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.createRestaurant(request)
                if (response.isSuccessful) {
                    onSuccess()
                    getAllRestaurants()
                } else {
                    onError("Failed to create restaurant: ${response.code()}")
                }
            } catch (e: Exception) {
                onError("Error: ${e.message}")
            }
        }
    }

    fun createLivreur(request: LivreurRequest, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = repository.createLivreur(request)
                if (response.isSuccessful) {
                    onSuccess()
                    // Refresh users? Livreur is a user + livreur entry.
                    getAllUsers()
                } else {
                    onError("Failed to create livreur: ${response.code()}")
                }
            } catch (e: Exception) {
                 onError("Error: ${e.message}")
            }
        }
    }
}
