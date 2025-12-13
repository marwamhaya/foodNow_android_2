package com.example.foodnow.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodnow.data.LoginRequest
import com.example.foodnow.data.Repository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: Repository) : ViewModel() {

    private val _loginResult = MutableLiveData<Result<Boolean>>()
    val loginResult: LiveData<Result<Boolean>> = _loginResult

    fun login(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = repository.login(LoginRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val authResponse = response.body()!!
                    repository.saveAuth(authResponse.token, authResponse.role)
                    _loginResult.value = Result.success(true)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    _loginResult.value = Result.failure(Exception("Login failed: ${response.code()} $errorBody"))
                }
            } catch (e: Exception) {
                // Log the exception stack trace to Logcat? Or just pass message
                e.printStackTrace()
                _loginResult.value = Result.failure(Exception("Error: ${e.message}"))
            }
        }
    }
}
