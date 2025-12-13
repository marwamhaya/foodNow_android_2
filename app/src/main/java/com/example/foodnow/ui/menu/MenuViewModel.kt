package com.example.foodnow.ui.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodnow.data.MenuItemResponse
import com.example.foodnow.data.Repository
import kotlinx.coroutines.launch

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
}
