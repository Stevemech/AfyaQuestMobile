package com.example.afyaquest.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.example.afyaquest.R
import com.example.afyaquest.presentation.auth.AuthViewModel
import com.example.afyaquest.presentation.navigation.Screen
import com.example.afyaquest.presentation.profile.ProfileViewModel
import com.example.afyaquest.util.LanguageManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val currentLanguage by viewModel.currentLanguage.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // General Section
            item { SectionHeader(text = stringResource(R.string.settings_general)) }

            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = stringResource(R.string.language),
                    subtitle = when (currentLanguage) {
                        LanguageManager.LANGUAGE_SPANISH -> "Español"
                        LanguageManager.LANGUAGE_KAQCHIKEL -> "Kaqchikel"
                        else -> "English"
                    },
                    onClick = { showLanguageDialog = true }
                )
            }

            // Account Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(text = stringResource(R.string.account))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = stringResource(R.string.settings_profile_info),
                    subtitle = userProfile?.name ?: stringResource(R.string.settings_view_profile),
                    onClick = { showProfileDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = stringResource(R.string.settings_change_password),
                    subtitle = stringResource(R.string.settings_update_password),
                    onClick = { showPasswordDialog = true }
                )
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(text = stringResource(R.string.about))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = stringResource(R.string.settings_about_app),
                    subtitle = stringResource(R.string.settings_version),
                    onClick = { }
                )
            }

            // Logout
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(text = stringResource(R.string.settings_danger_zone))
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = stringResource(R.string.logout),
                    subtitle = stringResource(R.string.settings_sign_out_desc),
                    onClick = { showLogoutDialog = true },
                    isDestructive = true
                )
            }
        }
    }

    // Language dialog
    if (showLanguageDialog) {
        LanguageDialog(
            currentLanguage = currentLanguage,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { languageCode ->
                scope.launch {
                    viewModel.changeLanguage(languageCode)
                    showLanguageDialog = false
                }
            }
        )
    }

    // Profile info dialog
    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text(stringResource(R.string.settings_profile_info)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileInfoRow(stringResource(R.string.settings_name), userProfile?.name ?: "—")
                    ProfileInfoRow(stringResource(R.string.email), userProfile?.email ?: "—")
                    ProfileInfoRow(stringResource(R.string.settings_phone), userProfile?.phone ?: "—")
                    ProfileInfoRow(stringResource(R.string.settings_organization), userProfile?.organization ?: "—")
                    ProfileInfoRow(stringResource(R.string.settings_role), userProfile?.role ?: "—")
                    ProfileInfoRow(stringResource(R.string.level), "${userProfile?.level ?: 0}")
                    ProfileInfoRow(stringResource(R.string.total_xp), "${userProfile?.totalPoints ?: 0}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }

    // Change password dialog — uses email verification code flow
    if (showPasswordDialog) {
        var step by remember { mutableStateOf(1) } // 1=send code, 2=enter code+password
        var verificationCode by remember { mutableStateOf("") }
        var newPassword by remember { mutableStateOf("") }
        var confirmPassword by remember { mutableStateOf("") }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var isLoading by remember { mutableStateOf(false) }
        var success by remember { mutableStateOf(false) }
        val userEmail = userProfile?.email ?: ""
        val noEmailFoundMsg = stringResource(R.string.no_email_found)
        val enterVerificationCodeMsg = stringResource(R.string.enter_verification_code_error)
        val passwordMinLengthMsg = stringResource(R.string.password_min_length)
        val passwordsDontMatchMsg = stringResource(R.string.passwords_dont_match)

        AlertDialog(
            onDismissRequest = { if (!isLoading) showPasswordDialog = false },
            title = { Text(if (success) stringResource(R.string.password_changed) else if (step == 1) stringResource(R.string.reset_password) else stringResource(R.string.enter_code)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        success -> {
                            Text(stringResource(R.string.password_changed_success),
                                color = MaterialTheme.colorScheme.primary)
                        }
                        step == 1 -> {
                            Text(stringResource(R.string.send_code_email_desc),
                                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(userEmail, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        step == 2 -> {
                            Text(stringResource(R.string.enter_code_instructions, userEmail),
                                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = verificationCode,
                                onValueChange = { verificationCode = it; errorMessage = null },
                                label = { Text(stringResource(R.string.verification_code)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
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
                                label = { Text(stringResource(R.string.confirm_new_password)) },
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
                        TextButton(onClick = { showPasswordDialog = false }) { Text(stringResource(R.string.done)) }
                    }
                    step == 1 -> {
                        TextButton(
                            onClick = {
                                if (userEmail.isBlank()) {
                                    errorMessage = noEmailFoundMsg
                                } else {
                                    isLoading = true
                                    scope.launch {
                                        val result = authViewModel.forgotPassword(userEmail)
                                        isLoading = false
                                        if (result == null) step = 2
                                        else errorMessage = result
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
                                    verificationCode.isBlank() -> errorMessage = enterVerificationCodeMsg
                                    newPassword.length < 8 -> errorMessage = passwordMinLengthMsg
                                    newPassword != confirmPassword -> errorMessage = passwordsDontMatchMsg
                                    else -> {
                                        isLoading = true
                                        scope.launch {
                                            val result = authViewModel.confirmForgotPassword(userEmail, verificationCode, newPassword)
                                            isLoading = false
                                            if (result == null) success = true
                                            else errorMessage = result
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
                    TextButton(onClick = { showPasswordDialog = false }, enabled = !isLoading) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text(stringResource(R.string.logout_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text(stringResource(R.string.logout), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LanguageDialog(
    currentLanguage: String,
    onDismiss: () -> Unit,
    onLanguageSelected: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_select_language)) },
        text = {
            Column {
                LanguageOption(LanguageManager.LANGUAGE_ENGLISH, "English", currentLanguage == LanguageManager.LANGUAGE_ENGLISH) { onLanguageSelected(LanguageManager.LANGUAGE_ENGLISH) }
                Spacer(modifier = Modifier.height(8.dp))
                LanguageOption(LanguageManager.LANGUAGE_SPANISH, "Español", currentLanguage == LanguageManager.LANGUAGE_SPANISH) { onLanguageSelected(LanguageManager.LANGUAGE_SPANISH) }
                Spacer(modifier = Modifier.height(8.dp))
                LanguageOption(LanguageManager.LANGUAGE_KAQCHIKEL, "Kaqchikel", currentLanguage == LanguageManager.LANGUAGE_KAQCHIKEL) { onLanguageSelected(LanguageManager.LANGUAGE_KAQCHIKEL) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
fun LanguageOption(language: String, displayName: String, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = displayName, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
            if (isSelected) {
                Icon(imageVector = Icons.Filled.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
