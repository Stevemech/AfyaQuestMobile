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
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            item { SectionHeader(text = "General") }

            item {
                SettingsItem(
                    icon = Icons.Default.Language,
                    title = "Language",
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
                SectionHeader(text = "Account")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Profile Information",
                    subtitle = userProfile?.name ?: "View your profile details",
                    onClick = { showProfileDialog = true }
                )
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Change Password",
                    subtitle = "Update your password",
                    onClick = { showPasswordDialog = true }
                )
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(text = "About")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About AfyaQuest",
                    subtitle = "Version 1.0.0",
                    onClick = { }
                )
            }

            // Logout
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SectionHeader(text = "Danger Zone")
            }

            item {
                SettingsItem(
                    icon = Icons.Default.Logout,
                    title = "Logout",
                    subtitle = "Sign out of your account",
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
            title = { Text("Profile Information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileInfoRow("Name", userProfile?.name ?: "—")
                    ProfileInfoRow("Email", userProfile?.email ?: "—")
                    ProfileInfoRow("Phone", userProfile?.phone ?: "—")
                    ProfileInfoRow("Organization", userProfile?.organization ?: "—")
                    ProfileInfoRow("Role", userProfile?.role ?: "—")
                    ProfileInfoRow("Level", "${userProfile?.level ?: 0}")
                    ProfileInfoRow("Total XP", "${userProfile?.totalPoints ?: 0}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close")
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

        AlertDialog(
            onDismissRequest = { if (!isLoading) showPasswordDialog = false },
            title = { Text(if (success) "Password Changed" else if (step == 1) "Reset Password" else "Enter Code") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    when {
                        success -> {
                            Text("Your password has been changed successfully!",
                                color = MaterialTheme.colorScheme.primary)
                        }
                        step == 1 -> {
                            Text("We'll send a verification code to your email:",
                                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(userEmail, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                        step == 2 -> {
                            Text("Enter the verification code sent to $userEmail and your new password.",
                                fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedTextField(
                                value = verificationCode,
                                onValueChange = { verificationCode = it; errorMessage = null },
                                label = { Text("Verification Code") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it; errorMessage = null },
                                label = { Text("New Password") },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it; errorMessage = null },
                                label = { Text("Confirm New Password") },
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
                        TextButton(onClick = { showPasswordDialog = false }) { Text("Done") }
                    }
                    step == 1 -> {
                        TextButton(
                            onClick = {
                                if (userEmail.isBlank()) {
                                    errorMessage = "No email found for your account"
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
                            else Text("Send Code")
                        }
                    }
                    step == 2 -> {
                        TextButton(
                            onClick = {
                                when {
                                    verificationCode.isBlank() -> errorMessage = "Enter the verification code"
                                    newPassword.length < 8 -> errorMessage = "Password must be at least 8 characters"
                                    newPassword != confirmPassword -> errorMessage = "Passwords don't match"
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
                            else Text("Reset Password")
                        }
                    }
                }
            },
            dismissButton = {
                if (!success) {
                    TextButton(onClick = { showPasswordDialog = false }, enabled = !isLoading) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
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
        title = { Text("Select Language") },
        text = {
            Column {
                LanguageOption(LanguageManager.LANGUAGE_ENGLISH, "English", currentLanguage == LanguageManager.LANGUAGE_ENGLISH) { onLanguageSelected(LanguageManager.LANGUAGE_ENGLISH) }
                Spacer(modifier = Modifier.height(8.dp))
                LanguageOption(LanguageManager.LANGUAGE_SPANISH, "Español", currentLanguage == LanguageManager.LANGUAGE_SPANISH) { onLanguageSelected(LanguageManager.LANGUAGE_SPANISH) }
                Spacer(modifier = Modifier.height(8.dp))
                LanguageOption(LanguageManager.LANGUAGE_KAQCHIKEL, "Kaqchikel", currentLanguage == LanguageManager.LANGUAGE_KAQCHIKEL) { onLanguageSelected(LanguageManager.LANGUAGE_KAQCHIKEL) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
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
