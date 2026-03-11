package com.example.afyaquest.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.R
import com.example.afyaquest.presentation.navigation.Screen
import com.example.afyaquest.util.LanguageManager
import com.example.afyaquest.util.Resource
import kotlinx.coroutines.launch

/**
 * Login screen for user authentication.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPassword by remember { mutableStateOf(false) }

    val loginState by viewModel.loginState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val scope = rememberCoroutineScope()

    val loginFailedText = stringResource(R.string.login_failed)

    // Handle login state changes
    LaunchedEffect(loginState) {
        when (loginState) {
            is Resource.Success -> {
                navController.navigate(Screen.Dashboard.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
                viewModel.resetLoginState()
            }
            is Resource.Error -> {
                snackbarHostState.showSnackbar(
                    message = (loginState as Resource.Error).message ?: loginFailedText,
                    duration = SnackbarDuration.Short
                )
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp)
        ) {
            // Language toggle at the top
            LanguageToggle(
                currentLanguage = currentLanguage,
                onLanguageSelected = { lang ->
                    scope.launch {
                        viewModel.changeLanguage(lang)
                    }
                },
                modifier = Modifier.align(Alignment.TopEnd)
            )

            // Centered login form
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = stringResource(R.string.welcome_back),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.sign_in_subtitle),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Email field
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(stringResource(R.string.email)) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password field
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) {
                                    stringResource(R.string.hide_password)
                                } else {
                                    stringResource(R.string.show_password)
                                }
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            if (email.isNotBlank() && password.isNotBlank()) {
                                viewModel.login(email, password)
                            }
                        }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Login button
                Button(
                    onClick = {
                        if (email.isNotBlank() && password.isNotBlank()) {
                            viewModel.login(email, password)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = loginState !is Resource.Loading
                ) {
                    if (loginState is Resource.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text(stringResource(R.string.sign_in), fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Forgot password link
                TextButton(onClick = { showForgotPassword = true }) {
                    Text(stringResource(R.string.forgot_password), fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Register link
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.no_account) + " ",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    TextButton(
                        onClick = {
                            navController.navigate(Screen.Register.route)
                        }
                    ) {
                        Text(stringResource(R.string.sign_up))
                    }
                }
            }
        }
    }

    // Forgot Password flow
    if (showForgotPassword) {
        ForgotPasswordDialog(
            initialEmail = email,
            viewModel = viewModel,
            onDismiss = { showForgotPassword = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ForgotPasswordDialog(
    initialEmail: String,
    viewModel: AuthViewModel,
    onDismiss: () -> Unit
) {
    var step by remember { mutableStateOf(1) } // 1=enter email, 2=enter code+password
    var forgotEmail by remember { mutableStateOf(initialEmail) }
    var verificationCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var success by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val enterYourEmailText = stringResource(R.string.enter_your_email)
    val enterVerificationCodeText = stringResource(R.string.enter_verification_code_error)
    val passwordMinLengthText = stringResource(R.string.password_min_length)
    val passwordsDontMatchText = stringResource(R.string.passwords_dont_match)

    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = {
            Text(
                if (success) stringResource(R.string.password_reset_title)
                else if (step == 1) stringResource(R.string.forgot_password_title)
                else stringResource(R.string.reset_password)
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when {
                    success -> {
                        Text(stringResource(R.string.password_reset_success),
                            color = MaterialTheme.colorScheme.primary)
                    }
                    step == 1 -> {
                        Text(stringResource(R.string.enter_email_for_code),
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = forgotEmail,
                            onValueChange = { forgotEmail = it; errorMessage = null },
                            label = { Text(stringResource(R.string.email)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                    }
                    step == 2 -> {
                        Text(stringResource(R.string.enter_code_and_password, forgotEmail),
                            fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = verificationCode,
                            onValueChange = { verificationCode = it; errorMessage = null },
                            label = { Text(stringResource(R.string.verification_code)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it; errorMessage = null },
                            label = { Text(stringResource(R.string.new_password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = { confirmPassword = it; errorMessage = null },
                            label = { Text(stringResource(R.string.confirm_password)) },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            when {
                success -> {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.done)) }
                }
                step == 1 -> {
                    TextButton(
                        onClick = {
                            if (forgotEmail.isBlank()) {
                                errorMessage = enterYourEmailText
                            } else {
                                isLoading = true
                                scope.launch {
                                    val result = viewModel.forgotPassword(forgotEmail)
                                    isLoading = false
                                    if (result == null) {
                                        step = 2
                                    } else {
                                        errorMessage = result
                                    }
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(stringResource(R.string.send_code))
                    }
                }
                step == 2 -> {
                    TextButton(
                        onClick = {
                            when {
                                verificationCode.isBlank() -> errorMessage = enterVerificationCodeText
                                newPassword.length < 8 -> errorMessage = passwordMinLengthText
                                newPassword != confirmPassword -> errorMessage = passwordsDontMatchText
                                else -> {
                                    isLoading = true
                                    scope.launch {
                                        val result = viewModel.confirmForgotPassword(forgotEmail, verificationCode, newPassword)
                                        isLoading = false
                                        if (result == null) {
                                            success = true
                                        } else {
                                            errorMessage = result
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        else Text(stringResource(R.string.reset_password))
                    }
                }
            }
        },
        dismissButton = {
            if (!success) {
                TextButton(onClick = onDismiss, enabled = !isLoading) { Text(stringResource(R.string.cancel)) }
            }
        }
    )
}

@Composable
private fun LanguageToggle(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(4.dp))

            LanguageChip(
                label = "EN",
                isSelected = currentLanguage == LanguageManager.LANGUAGE_ENGLISH,
                onClick = { onLanguageSelected(LanguageManager.LANGUAGE_ENGLISH) }
            )

            Spacer(modifier = Modifier.width(4.dp))

            LanguageChip(
                label = "ES",
                isSelected = currentLanguage == LanguageManager.LANGUAGE_SPANISH,
                onClick = { onLanguageSelected(LanguageManager.LANGUAGE_SPANISH) }
            )

            Spacer(modifier = Modifier.width(4.dp))

            LanguageChip(
                label = "CAK",
                isSelected = currentLanguage == LanguageManager.LANGUAGE_KAQCHIKEL,
                onClick = { onLanguageSelected(LanguageManager.LANGUAGE_KAQCHIKEL) }
            )
        }
    }
}

@Composable
private fun LanguageChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .then(
                if (isSelected) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}
