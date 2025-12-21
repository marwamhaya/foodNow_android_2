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

    private val _restaurants = MutableLiveData<Result<List<com.example.foodnow.data.RestaurantResponse>>>()
    val restaurants: LiveData<Result<List<com.example.foodnow.data.RestaurantResponse>>> = _restaurants

    private val _popularItems = MutableLiveData<Result<List<com.example.foodnow.data.MenuItemResponse>>>()
    val popularItems: LiveData<Result<List<com.example.foodnow.data.MenuItemResponse>>> = _popularItems

    init {
        loadRestaurants()
    }

    fun loadRestaurants() {
        viewModelScope.launch {
            try {
                val response = repository.getRestaurants()
                if (response.isSuccessful && response.body() != null) {
                    val list = response.body()!!.content
                    _restaurants.value = Result.success(list)
                    
                    // Load popular items from the first restaurant for demo purposes
                    if (list.isNotEmpty()) {
                        loadPopularItems(list.first().id)
                    }
                } else {
                    _restaurants.value = Result.failure(Exception("Failed to load restaurants: ${response.code()}"))
                }
            } catch (e: Exception) {
                _restaurants.value = Result.failure(e)
            }
        }
    }

    private fun loadPopularItems(restaurantId: Long) {
        viewModelScope.launch {
            try {
                // Use real backend endpoint if available, but for now calling the new endpoint
                val response = repository.getPopularMenuItems()

                if (response.isSuccessful && response.body() != null) {
                    _popularItems.value = Result.success(response.body()!!)
                } else {
                    // Fallback to first restaurant items if backend endpoint fails or returns empty (simulating old behavior if needed)
                    // But if backend is updated, this should work.
                     _popularItems.value = Result.failure(Exception("Failed to load popular items"))
                }
            } catch (e: Exception) {
               // Ignore
            }
        }
    }
}
