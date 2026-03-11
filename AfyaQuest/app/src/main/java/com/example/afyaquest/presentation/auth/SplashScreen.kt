package com.example.afyaquest.presentation.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.afyaquest.R
import com.example.afyaquest.presentation.navigation.Screen
import kotlinx.coroutines.delay

/**
 * Splash screen shown on app launch.
 * Checks authentication status and navigates accordingly.
 */
@Composable
fun SplashScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Navigate after 2 seconds
    LaunchedEffect(key1 = true) {
        delay(2000)
        if (isLoggedIn) {
            navController.navigate(Screen.Dashboard.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        } else {
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    // UI
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App logo
                Image(
                    painter = painterResource(id = R.drawable.afyaquest_logo),
                    contentDescription = stringResource(R.string.afyaquest_logo),
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // App name
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.empowering_cha),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Loading indicator
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
