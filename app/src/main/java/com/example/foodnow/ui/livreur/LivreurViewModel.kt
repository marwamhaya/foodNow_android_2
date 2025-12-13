package com.example.foodnow.ui.livreur

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodnow.data.DeliveryResponse
import com.example.foodnow.data.LivreurResponse
import com.example.foodnow.data.Repository
import kotlinx.coroutines.launch

class LivreurViewModel(private val repository: Repository) : ViewModel() {

    private val _deliveries = MutableLiveData<Result<List<DeliveryResponse>>>()
    val deliveries: LiveData<Result<List<DeliveryResponse>>> = _deliveries

    private val _profile = MutableLiveData<Result<LivreurResponse>>()
    val profile: LiveData<Result<LivreurResponse>> = _profile

    fun getAssignedDeliveries() {
        viewModelScope.launch {
            try {
                val response = repository.getAssignedDeliveries()
                if (response.isSuccessful && response.body() != null) {
                    _deliveries.value = Result.success(response.body()!!)
                } else {
                    _deliveries.value = Result.failure(Exception("Error loading deliveries: ${response.code()}"))
                }
            } catch (e: Exception) {
                _deliveries.value = Result.failure(e)
            }
        }
    }

    fun updateStatus(deliveryId: Long, status: String) {
        viewModelScope.launch {
            try {
                // Status options: "PREPARING" (no), "IN_DELIVERY", "DELIVERED"
                // Backend endpoint expects status string
                val response = repository.updateDeliveryStatus(deliveryId, status)
                if (response.isSuccessful) {
                    getAssignedDeliveries() // Refresh
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun getProfile() {
        viewModelScope.launch {
            try {
                 val response = repository.getLivreurProfile()
                 if (response.isSuccessful) {
                     _profile.value = Result.success(response.body()!!)
                 }
            } catch (e: Exception) {
                _profile.value = Result.failure(e)
            }
        }
    }
    
    fun toggleAvailability() {
         viewModelScope.launch {
             repository.toggleAvailability()
             getProfile()
         }
    }

    fun getDeliveryHistory() {
        viewModelScope.launch {
            try {
                val response = repository.getDeliveryHistory()
                if (response.isSuccessful && response.body() != null) {
                    _deliveries.value = Result.success(response.body()!!)
                }
            } catch (e: Exception) {
                 _deliveries.value = Result.failure(e)
            }
        }
    }
}
