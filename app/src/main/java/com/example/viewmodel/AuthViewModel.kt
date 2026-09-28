package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.User
import com.example.data.preferences.SessionManager
import com.example.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.regex.Pattern

data class AuthUiState(
    val isCheckingSession: Boolean = true,
    val isFirstRunAdminSetupNeeded: Boolean = false,
    val isLoggedIn: Boolean = false,
    val requiresPinUnlock: Boolean = false,
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,

    // Admin Setup Form (First Run)
    val adminName: String = "مشرف النظام",
    val adminEmail: String = "admin@duyuni.app",
    val adminPassword: String = "",
    val adminConfirmPassword: String = "",
    val adminPin: String = "1234",
    val adminCurrency: String = "ر.س",
    val adminPasswordVisible: Boolean = false,
    val adminConfirmPasswordVisible: Boolean = false,

    // Login Form
    val loginEmail: String = "",
    val loginPassword: String = "",
    val loginPasswordVisible: Boolean = false,

    // Register Form
    val regName: String = "",
    val regEmail: String = "",
    val regPassword: String = "",
    val regConfirmPassword: String = "",
    val regPin: String = "",
    val regCurrency: String = "ر.س",
    val regPasswordVisible: Boolean = false,

    // PIN Unlock Form
    val pinEntered: String = ""
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository: AuthRepository
    private val sessionManager: SessionManager

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val emailPattern = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
    )

    init {
        val db = AppDatabase.getDatabase(application)
        sessionManager = SessionManager(application)
        authRepository = AuthRepository(db.userDao(), sessionManager)

        checkInitialSession()
    }

    fun checkInitialSession() {
        viewModelScope.launch {
            val needsAdminSetup = authRepository.isFirstRunAdminSetupNeeded()
            if (needsAdminSetup) {
                _uiState.update {
                    it.copy(
                        isCheckingSession = false,
                        isFirstRunAdminSetupNeeded = true,
                        isLoggedIn = false,
                        requiresPinUnlock = false
                    )
                }
                return@launch
            }

            if (authRepository.isUserLoggedIn()) {
                val user = authRepository.getCurrentUser()
                if (user != null) {
                    val hasPin = !user.pinCode.isNullOrEmpty()
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            isFirstRunAdminSetupNeeded = false,
                            isLoggedIn = !hasPin,
                            requiresPinUnlock = hasPin,
                            currentUser = user
                        )
                    }
                } else {
                    authRepository.logout()
                    _uiState.update {
                        it.copy(
                            isCheckingSession = false,
                            isFirstRunAdminSetupNeeded = false,
                            isLoggedIn = false
                        )
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCheckingSession = false,
                        isFirstRunAdminSetupNeeded = false,
                        isLoggedIn = false
                    )
                }
            }
        }
    }

    // Input handlers for Admin Setup
    fun onAdminNameChanged(name: String) {
        _uiState.update { it.copy(adminName = name, errorMessage = null) }
    }

    fun onAdminEmailChanged(email: String) {
        _uiState.update { it.copy(adminEmail = email, errorMessage = null) }
    }

    fun onAdminPasswordChanged(password: String) {
        _uiState.update { it.copy(adminPassword = password, errorMessage = null) }
    }

    fun onAdminConfirmPasswordChanged(confirm: String) {
        _uiState.update { it.copy(adminConfirmPassword = confirm, errorMessage = null) }
    }

    fun onAdminPinChanged(pin: String) {
        if (pin.length <= 4 && pin.all { it.isDigit() }) {
            _uiState.update { it.copy(adminPin = pin, errorMessage = null) }
        }
    }

    fun onAdminCurrencyChanged(currency: String) {
        _uiState.update { it.copy(adminCurrency = currency) }
    }

    fun toggleAdminPasswordVisibility() {
        _uiState.update { it.copy(adminPasswordVisible = !it.adminPasswordVisible) }
    }

    fun toggleAdminConfirmPasswordVisibility() {
        _uiState.update { it.copy(adminConfirmPasswordVisible = !it.adminConfirmPasswordVisible) }
    }

    // Initial Admin Setup Action
    fun setupInitialAdmin(onSuccess: () -> Unit) {
        val name = _uiState.value.adminName.trim()
        val email = _uiState.value.adminEmail.trim()
        val password = _uiState.value.adminPassword
        val confirm = _uiState.value.adminConfirmPassword
        val pin = _uiState.value.adminPin.trim()
        val currency = _uiState.value.adminCurrency

        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "يرجى كتابة اسم المشرف") }
            return
        }
        if (email.isEmpty() || !emailPattern.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال بريد إلكتروني صالح للمشرف") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "كلمة مرور المشرف يجب أن لا تقل عن 6 خانات") }
            return
        }
        if (password != confirm) {
            _uiState.update { it.copy(errorMessage = "كلمتا المرور غير متطابقتين") }
            return
        }
        if (pin.isNotEmpty() && pin.length != 4) {
            _uiState.update { it.copy(errorMessage = "رمز PIN يجب أن يتكون من 4 أرقام") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.setupInitialAdmin(
                fullName = name,
                email = email,
                passwordPlain = password,
                pinCode = pin.ifEmpty { null },
                currency = currency
            )
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isFirstRunAdminSetupNeeded = false,
                            isLoggedIn = true,
                            requiresPinUnlock = false,
                            currentUser = user,
                            errorMessage = null,
                            successMessage = "تم إنشاء حساب المشرف وتهيئة النظام بنجاح!"
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "تعذر تهيئة حساب المشرف")
                    }
                }
            )
        }
    }

    // Input handlers for Login
    fun onLoginEmailChanged(email: String) {
        _uiState.update { it.copy(loginEmail = email, errorMessage = null) }
    }

    fun onLoginPasswordChanged(password: String) {
        _uiState.update { it.copy(loginPassword = password, errorMessage = null) }
    }

    fun toggleLoginPasswordVisibility() {
        _uiState.update { it.copy(loginPasswordVisible = !it.loginPasswordVisible) }
    }

    // Input handlers for Register
    fun onRegNameChanged(name: String) {
        _uiState.update { it.copy(regName = name, errorMessage = null) }
    }

    fun onRegEmailChanged(email: String) {
        _uiState.update { it.copy(regEmail = email, errorMessage = null) }
    }

    fun onRegPasswordChanged(password: String) {
        _uiState.update { it.copy(regPassword = password, errorMessage = null) }
    }

    fun onRegConfirmPasswordChanged(confirm: String) {
        _uiState.update { it.copy(regConfirmPassword = confirm, errorMessage = null) }
    }

    fun onRegPinChanged(pin: String) {
        if (pin.length <= 4 && pin.all { it.isDigit() }) {
            _uiState.update { it.copy(regPin = pin, errorMessage = null) }
        }
    }

    fun onRegCurrencyChanged(currency: String) {
        _uiState.update { it.copy(regCurrency = currency) }
    }

    fun toggleRegPasswordVisibility() {
        _uiState.update { it.copy(regPasswordVisible = !it.regPasswordVisible) }
    }

    // PIN Unlock
    fun onPinDigit(digit: Char) {
        if (!digit.isDigit()) return
        val current = _uiState.value.pinEntered
        if (current.length < 4) {
            val updated = current + digit
            _uiState.update { it.copy(pinEntered = updated, errorMessage = null) }
            if (updated.length == 4) {
                verifyPinAndUnlock(updated)
            }
        }
    }

    fun onPinBackspace() {
        val current = _uiState.value.pinEntered
        if (current.isNotEmpty()) {
            _uiState.update { it.copy(pinEntered = current.dropLast(1), errorMessage = null) }
        }
    }

    fun clearPin() {
        _uiState.update { it.copy(pinEntered = "", errorMessage = null) }
    }

    private fun verifyPinAndUnlock(pin: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = authRepository.verifyPin(pin)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            requiresPinUnlock = false,
                            currentUser = user,
                            pinEntered = "",
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            pinEntered = "",
                            errorMessage = error.message ?: "رمز PIN غير صحيح"
                        )
                    }
                }
            )
        }
    }

    // Login Action
    fun login(onSuccess: () -> Unit) {
        val email = _uiState.value.loginEmail.trim()
        val password = _uiState.value.loginPassword

        if (email.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال البريد الإلكتروني") }
            return
        }
        if (!emailPattern.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "صيغة البريد الإلكتروني غير صحيحة") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "كلمة المرور يجب أن لا تقل عن 6 خانات") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            requiresPinUnlock = false,
                            currentUser = user,
                            errorMessage = null,
                            successMessage = "تم تسجيل الدخول بنجاح! أهلاً بك"
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "فشل تسجيل الدخول")
                    }
                }
            )
        }
    }

    // Register Action
    fun register(onSuccess: () -> Unit) {
        val name = _uiState.value.regName.trim()
        val email = _uiState.value.regEmail.trim()
        val password = _uiState.value.regPassword
        val confirm = _uiState.value.regConfirmPassword
        val pin = _uiState.value.regPin.trim()
        val currency = _uiState.value.regCurrency

        if (name.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "يرجى كتابة الاسم بالكامل") }
            return
        }
        if (email.isEmpty() || !emailPattern.matcher(email).matches()) {
            _uiState.update { it.copy(errorMessage = "يرجى إدخال بريد إلكتروني صالح") }
            return
        }
        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "كلمة المرور يجب أن تكون 6 خانات أو أكثر") }
            return
        }
        if (password != confirm) {
            _uiState.update { it.copy(errorMessage = "كلمتا المرور غير متطابقتين") }
            return
        }
        if (pin.isNotEmpty() && pin.length != 4) {
            _uiState.update { it.copy(errorMessage = "رمز PIN يجب أن يتكون من 4 أرقام بالضبط") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.register(name, email, password, pin.ifEmpty { null }, currency)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            requiresPinUnlock = false,
                            currentUser = user,
                            errorMessage = null,
                            successMessage = "تم إنشاء الحساب بنجاح!"
                        )
                    }
                    onSuccess()
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "تعذر إنشاء الحساب")
                    }
                }
            )
        }
    }

    fun logout(onLoggedOut: () -> Unit) {
        authRepository.logout()
        _uiState.update {
            AuthUiState(
                isCheckingSession = false,
                isLoggedIn = false,
                requiresPinUnlock = false,
                currentUser = null
            )
        }
        onLoggedOut()
    }

    fun lockApp() {
        val user = _uiState.value.currentUser
        if (user != null && !user.pinCode.isNullOrEmpty()) {
            _uiState.update {
                it.copy(
                    isLoggedIn = false,
                    requiresPinUnlock = true,
                    pinEntered = ""
                )
            }
        } else {
            logout {}
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
