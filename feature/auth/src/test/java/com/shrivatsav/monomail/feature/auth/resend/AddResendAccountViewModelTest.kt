package com.shrivatsav.monomail.feature.auth.resend

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.shrivatsav.monomail.core.data.auth.AccountManager
import com.shrivatsav.monomail.core.data.auth.UserProfile
import com.shrivatsav.monomail.core.data.repository.resend.ResendRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AddResendAccountViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: AddResendAccountViewModel
    private lateinit var mockResendRepository: ResendRepository
    private lateinit var mockAccountManager: AccountManager

    @Before
    fun setUp() {
        mockResendRepository = mock()
        mockAccountManager = mock()
        
        // Mock application
        val mockApplication = mock<android.app.Application>()
        
        viewModel = AddResendAccountViewModel(
            mockApplication,
            mockResendRepository,
            mockAccountManager
        )
    }

    @Test
    fun testUpdateFields() = runTest {
        // When
        viewModel.updateApiKey("re_test123")
        viewModel.updateInboxId("inbox_456")
        viewModel.updateEmail("test@example.com")
        viewModel.updateDisplayName("Test User")

        // Then
        assertEquals("re_test123", viewModel.apiKey.value)
        assertEquals("inbox_456", viewModel.inboxId.value)
        assertEquals("test@example.com", viewModel.email.value)
        assertEquals("Test User", viewModel.displayName.value)
    }

    @Test
    fun testValidateAndAddAccountSuccess() = runTest {
        // Given
        viewModel.updateApiKey("re_test123")
        viewModel.updateInboxId("inbox_456")
        viewModel.updateEmail("test@example.com")
        viewModel.updateDisplayName("Test User")
        
        // Mock successful validation
        whenever(mockResendRepository.validateCredentials("inbox_456", "re_test123"))
            .thenReturn(true)
        
        // Mock account manager
        whenever(mockAccountManager.addAccount(match {
            it.email == "test@example.com" &&
            it.displayName == "Test User" &&
            it.provider == "resend" &&
            it.accessToken == "re_test123" &&
            it.refreshToken == "inbox_456"
        })).thenReturn(Unit)
        
        // When
        viewModel.validateAndAddAccount()
        
        // Wait for coroutine
        Thread.sleep(100)
        
        // Then
        val state = viewModel.state.value
        assertTrue(state is ResendAccountState.Success)
        val successState = state as ResendAccountState.Success
        assertEquals("test@example.com", successState.profile.email)
        assertEquals("Test User", successState.profile.displayName)
        assertEquals("resend", successState.profile.provider)
    }

    @Test
    fun testValidateAndAddAccountInvalidApiKey() = runTest {
        // Given - API key doesn't start with "re_"
        viewModel.updateApiKey("invalid_key")
        viewModel.updateInboxId("inbox_456")
        viewModel.updateEmail("test@example.com")
        viewModel.updateDisplayName("Test User")
        
        // When
        viewModel.validateAndAddAccount()
        
        // Then
        val state = viewModel.state.value
        assertTrue(state is ResendAccountState.Error)
        val errorState = state as ResendAccountState.Error
        assertTrue(errorState.message.contains("API key"))
    }

    @Test
    fun testValidateAndAddAccountInvalidInboxId() = runTest {
        // Given - Inbox ID doesn't start with "inbox_"
        viewModel.updateApiKey("re_test123")
        viewModel.updateInboxId("invalid_inbox")
        viewModel.updateEmail("test@example.com")
        viewModel.updateDisplayName("Test User")
        
        // When
        viewModel.validateAndAddAccount()
        
        // Then
        val state = viewModel.state.value
        assertTrue(state is ResendAccountState.Error)
        val errorState = state as ResendAccountState.Error
        assertTrue(errorState.message.contains("Inbox ID"))
    }

    @Test
    fun testValidateAndAddAccountInvalidEmail() = runTest {
        // Given - Invalid email
        viewModel.updateApiKey("re_test123")
        viewModel.updateInboxId("inbox_456")
        viewModel.updateEmail("invalid-email")
        viewModel.updateDisplayName("Test User")
        
        // When
        viewModel.validateAndAddAccount()
        
        // Then
        val state = viewModel.state.value
        assertTrue(state is ResendAccountState.Error)
        val errorState = state as ResendAccountState.Error
        assertTrue(errorState.message.contains("email"))
    }

    @Test
    fun testValidateAndAddAccountEmptyDisplayName() = runTest {
        // Given - Empty display name
        viewModel.updateApiKey("re_test123")
        viewModel.updateInboxId("inbox_456")
        viewModel.updateEmail("test@example.com")
        viewModel.updateDisplayName("")
        
        // When
        viewModel.validateAndAddAccount()
        
        // Then
        val state = viewModel.state.value
        assertTrue(state is ResendAccountState.Error)
        val errorState = state as ResendAccountState.Error
        assertTrue(errorState.message.contains("display name"))
    }

    @Test
    fun testValidateAndAddAccountInvalidCredentials() = runTest {
        // Given
        viewModel.updateApiKey("re_test123")
        viewModel.updateInboxId("inbox_456")
        viewModel.updateEmail("test@example.com")
        viewModel.updateDisplayName("Test User")
        
        // Mock failed validation
        whenever(mockResendRepository.validateCredentials("inbox_456", "re_test123"))
            .thenReturn(false)
        
        // When
        viewModel.validateAndAddAccount()
        
        // Wait for coroutine
        Thread.sleep(100)
        
        // Then
        val state = viewModel.state.value
        assertTrue(state is ResendAccountState.Error)
        val errorState = state as ResendAccountState.Error
        assertTrue(errorState.message.contains("credentials"))
    }

    @Test
    fun testResetState() = runTest {
        // Given - Set to error state
        viewModel.updateApiKey("re_test123")
        viewModel.updateInboxId("invalid_inbox")
        viewModel.updateEmail("test@example.com")
        viewModel.updateDisplayName("Test User")
        
        viewModel.validateAndAddAccount()
        
        // When
        viewModel.resetState()
        
        // Then
        val state = viewModel.state.value
        assertTrue(state is ResendAccountState.Idle)
    }

    @Test
    fun testAccountManagerInteraction() = runTest {
        // Given
        viewModel.updateApiKey("re_test123")
        viewModel.updateInboxId("inbox_456")
        viewModel.updateEmail("test@example.com")
        viewModel.updateDisplayName("Test User")
        
        whenever(mockResendRepository.validateCredentials("inbox_456", "re_test123"))
            .thenReturn(true)
        
        var capturedProfile: UserProfile? = null
        whenever(mockAccountManager.addAccount(match {
            capturedProfile = it
            true
        })).thenReturn(Unit)
        
        // When
        viewModel.validateAndAddAccount()
        
        // Wait for coroutine
        Thread.sleep(100)
        
        // Then
        assertNotNull(capturedProfile)
        assertEquals("test@example.com", capturedProfile!!.email)
        assertEquals("Test User", capturedProfile!!.displayName)
        assertEquals("resend", capturedProfile!!.provider)
        assertEquals("re_test123", capturedProfile!!.accessToken)
        assertEquals("inbox_456", capturedProfile!!.refreshToken)
        
        // Verify account switching
        verify(mockAccountManager).switchAccount(capturedProfile!!.id)
    }
}