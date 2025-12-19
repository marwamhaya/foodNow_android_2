package com.example.foodnow.ui.orders

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodnow.data.Order
import com.example.foodnow.data.Repository
import kotlinx.coroutines.launch

class OrdersViewModel(private val repository: Repository) : ViewModel() {

    private val _orders = MutableLiveData<Result<List<Order>>>()
    val orders: LiveData<Result<List<Order>>> = _orders

    private val _ratingStatus = MutableLiveData<Result<Boolean>>()
    val ratingStatus: LiveData<Result<Boolean>> = _ratingStatus

    fun fetchOrders() {
        viewModelScope.launch {
            try {
                val response = repository.getMyOrders()
                if (response.isSuccessful && response.body() != null) {
                    _orders.value = Result.success(response.body()!!)
                } else {
                    _orders.value = Result.failure(Exception("Failed to fetch orders"))
                }
            } catch (e: Exception) {
                _orders.value = Result.failure(e)
            }
        }
    }

    fun submitRating(orderId: Long, rating: Int, comment: String) {
        viewModelScope.launch {
            try {
                val response = repository.submitRating(orderId, rating, comment)
                if (response.isSuccessful) {
                    _ratingStatus.value = Result.success(true)
                } else {
                    _ratingStatus.value = Result.failure(Exception("Failed to submit rating: ${response.code()}"))
                }
            } catch (e: Exception) {
                _ratingStatus.value = Result.failure(e)
            }
        }
    }
}
