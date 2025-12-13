package com.example.foodnow.ui.register

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodnow.data.RegisterRequest
import com.example.foodnow.data.Repository
import kotlinx.coroutines.launch

class RegisterViewModel(private val repository: Repository) : ViewModel() {

    private val _registerResult = MutableLiveData<Result<Boolean>>()
    val registerResult: LiveData<Result<Boolean>> = _registerResult

    fun register(fullName: String, email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = repository.register(RegisterRequest(fullName, email, password))
                if (response.isSuccessful && response.body() != null) {
                    _registerResult.value = Result.success(true)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _registerResult.value = Result.failure(Exception("Registration failed: ${response.code()} $errorBody"))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _registerResult.value = Result.failure(Exception("Error: ${e.message}"))
            }
        }
    }
}
