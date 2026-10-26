package com.shrivatsav.monomail.feature.auth.resend

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shrivatsav.monomail.core.data.auth.AccountManager
import com.shrivatsav.monomail.core.data.auth.UserProfile
import com.shrivatsav.monomail.core.data.repository.resend.ResendRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ResendAccountState {
    object Idle : ResendAccountState()
    object Loading : ResendAccountState()
    data class Success(val profile: UserProfile) : ResendAccountState()
    data class Error(val message: String) : ResendAccountState()
}

@HiltViewModel
class AddResendAccountViewModel @Inject constructor(
    application: Application,
    private val resendRepository: ResendRepository,
    private val accountManager: AccountManager
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow<ResendAccountState>(ResendAccountState.Idle)
    val state: StateFlow<ResendAccountState> = _state.asStateFlow()

    private val _apiKey = MutableStateFlow("")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _inboxId = MutableStateFlow("")
    val inboxId: StateFlow<String> = _inboxId.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _displayName = MutableStateFlow("")
    val displayName: StateFlow<String> = _displayName.asStateFlow()

    fun updateApiKey(value: String) {
        _apiKey.value = value.trim()
    }

    fun updateInboxId(value: String) {
        _inboxId.value = value.trim()
    }

    fun updateEmail(value: String) {
        _email.value = value.trim()
    }

    fun updateDisplayName(value: String) {
        _displayName.value = value.trim()
    }

    fun validateAndAddAccount() {
        val apiKey = _apiKey.value
        val inboxId = _inboxId.value
        val email = _email.value
        val displayName = _displayName.value

        // Basic validation
        if (apiKey.isEmpty() || !apiKey.startsWith("re_")) {
            _state.value = ResendAccountState.Error("Please enter a valid Resend API key (starts with 're_')")
            return
        }

        if (inboxId.isEmpty() || !inboxId.startsWith("inbox_")) {
            _state.value = ResendAccountState.Error("Please enter a valid Inbox ID (starts with 'inbox_')")
            return
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _state.value = ResendAccountState.Error("Please enter a valid email address")
            return
        }

        if (displayName.isEmpty()) {
            _state.value = ResendAccountState.Error("Please enter your display name")
            return
        }

        viewModelScope.launch {
            _state.value = ResendAccountState.Loading

            try {
                // Test credentials
                val isValid = resendRepository.validateCredentials(inboxId, apiKey)
                if (!isValid) {
                    _state.value = ResendAccountState.Error("Invalid credentials. Please check your API key and inbox ID.")
                    return@launch
                }

                // Create user profile
                val profile = UserProfile(
                    id = "resend_${System.currentTimeMillis()}",
                    displayName = displayName,
                    email = email,
                    photoUrl = null,
                    accessToken = apiKey, // Store API key as access token
                    provider = "resend",
                    refreshToken = inboxId // Store inbox ID as refresh token
                )

                // Add account
                accountManager.addAccount(profile)
                accountManager.switchAccount(profile.id)

                _state.value = ResendAccountState.Success(profile)

            } catch (e: Exception) {
                _state.value = ResendAccountState.Error("Failed to add account: ${e.message}")
            }
        }
    }

    fun resetState() {
        _state.value = ResendAccountState.Idle
    }
}