package com.shrivatsav.monomail.feature.auth.resend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shrivatsav.monomail.ui.theme.cornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddResendAccountScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AddResendAccountViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val apiKey by viewModel.apiKey.collectAsStateWithLifecycle()
    val inboxId by viewModel.inboxId.collectAsStateWithLifecycle()
    val email by viewModel.email.collectAsStateWithLifecycle()
    val displayName by viewModel.displayName.collectAsStateWithLifecycle()

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Resend Account") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Description
            Text(
                text = "Connect your Resend Inbox to access email threads.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // API Key Field
            OutlinedTextField(
                value = apiKey,
                onValueChange = viewModel::updateApiKey,
                label = { Text("Resend API Key") },
                placeholder = { Text("re_...") },
                leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = cornerShape(12.dp),
                isError = apiKey.isNotEmpty() && !apiKey.startsWith("re_")
            )

            if (apiKey.isNotEmpty() && !apiKey.startsWith("re_")) {
                Text(
                    text = "API key should start with 're_'",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Inbox ID Field
            OutlinedTextField(
                value = inboxId,
                onValueChange = viewModel::updateInboxId,
                label = { Text("Inbox ID") },
                placeholder = { Text("inbox_...") },
                leadingIcon = { Icon(Icons.Rounded.Inbox, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = cornerShape(12.dp),
                isError = inboxId.isNotEmpty() && !inboxId.startsWith("inbox_")
            )

            if (inboxId.isNotEmpty() && !inboxId.startsWith("inbox_")) {
                Text(
                    text = "Inbox ID should start with 'inbox_'",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = viewModel::updateEmail,
                label = { Text("Your Email") },
                placeholder = { Text("you@example.com") },
                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = cornerShape(12.dp)
            )

            // Display Name Field
            OutlinedTextField(
                value = displayName,
                onValueChange = viewModel::updateDisplayName,
                label = { Text("Display Name") },
                placeholder = { Text("Your Name") },
                leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth(),
                shape = cornerShape(12.dp)
            )

            // Help text
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = cornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Where to find these credentials:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• API Key: Resend Dashboard → API Keys",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "• Inbox ID: Resend Inboxes → Select your inbox",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Add Account Button
            Button(
                onClick = viewModel::validateAndAddAccount,
                modifier = Modifier.fillMaxWidth(),
                shape = cornerShape(16.dp),
                enabled = state != ResendAccountState.Loading
            ) {
                when (state) {
                    ResendAccountState.Loading -> {
                        LoadingIndicator(
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Connecting...")
                    }
                    else -> {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Account")
                    }
                }
            }

            // Handle state changes
            LaunchedEffect(state) {
                when (state) {
                    is ResendAccountState.Success -> {
                        onSuccess()
                    }
                    is ResendAccountState.Error -> {
                        // Error is shown in the UI through the state
                    }
                    else -> {}
                }
            }

            // Show error message
            if (state is ResendAccountState.Error) {
                val errorState = state as ResendAccountState.Error
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = cornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
    }
}