package com.example.foodnow.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodnow.data.PageResponse
import com.example.foodnow.data.Repository
import com.example.foodnow.data.RestaurantResponse
import kotlinx.coroutines.launch

class HomeViewModel(private val repository: Repository) : ViewModel() {

    private val _restaurants = MutableLiveData<Result<List<RestaurantResponse>>>()
    val restaurants: LiveData<Result<List<RestaurantResponse>>> = _restaurants

    init {
        loadRestaurants()
    }

    fun loadRestaurants() {
        viewModelScope.launch {
            try {
                val response = repository.getRestaurants()
                if (response.isSuccessful && response.body() != null) {
                    _restaurants.value = Result.success(response.body()!!.content)
                } else {
                    _restaurants.value = Result.failure(Exception("Failed to load restaurants: ${response.code()}"))
                }
            } catch (e: Exception) {
                _restaurants.value = Result.failure(e)
            }
        }
    }
}
