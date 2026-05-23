package com.gracecode.network

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.PhoneAuthProvider
import com.gracecode.network.ui.theme.WeColabaiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeColabaiTheme {
                val currentUser by FirebaseManager.currentUserFlow.collectAsState()
                
                if (currentUser == null) {
                    AuthScreen {
                        // Auth state listener handles navigation
                    }
                } else {
                    MainAppShell(user = currentUser!!)
                }
            }
        }
    }
}

sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Home : Screen("home", "Feed", Icons.Default.Home)
    object Projects : Screen("projects", "Projects", Icons.Default.Work)
    object Network : Screen("network", "Network", Icons.Default.Public)
    object Alerts : Screen("alerts", "Alerts", Icons.Default.Notifications)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
}

@Composable
fun MainAppShell(user: User) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val screens = listOf(
        Screen.Home,
        Screen.Projects,
        Screen.Network,
        Screen.Alerts,
        Screen.Profile
    )

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                screens.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label, fontSize = 10.sp) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = Screen.Home.route) {
                composable(Screen.Home.route) { HomeScreen() }
                composable(Screen.Projects.route) { ProjectsScreen() }
                composable(Screen.Network.route) { PlaceholderScreen("Network") }
                composable(Screen.Alerts.route) { PlaceholderScreen("Notifications") }
                composable(Screen.Profile.route) { ProfileScreen(user = user) }
            }
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("$name Coming Soon", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
fun AuthScreen(onAuthSuccess: () -> Unit) {
    var isSignUp by remember { mutableStateOf(false) }
    var isPhoneAuth by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var verificationCode by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var isCodeSent by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var expertise by remember { mutableStateOf("") }
    var skillsInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as Activity

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = when {
                isPhoneAuth -> "Mobile Login"
                isSignUp -> "Register for WeColabai"
                else -> "Login to WeColabai"
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (!isPhoneAuth) {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            if (isSignUp) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = expertise,
                    onValueChange = { expertise = it },
                    label = { Text("Expertise (e.g., UI/UX, Android)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = skillsInput,
                    onValueChange = { skillsInput = it },
                    label = { Text("Skills (comma-separated)") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
            }
        } else {
            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                label = { Text("Phone Number (+CountryCode...)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            if (isCodeSent) {
                OutlinedTextField(
                    value = verificationCode,
                    onValueChange = { verificationCode = it },
                    label = { Text("Verification Code") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
            }
        }

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            Button(
                onClick = {
                    isLoading = true
                    if (isPhoneAuth) {
                        if (!isCodeSent) {
                            FirebaseManager.verifyPhoneNumber(
                                phoneNumber, activity,
                                onCodeSent = { id, _ ->
                                    verificationId = id
                                    isCodeSent = true
                                    isLoading = false
                                    Toast.makeText(context, "Code sent!", Toast.LENGTH_SHORT).show()
                                },
                                onVerificationCompleted = { credential ->
                                    FirebaseManager.signInWithPhoneCredential(credential,
                                        onSuccess = {
                                            isLoading = false
                                            onAuthSuccess()
                                        },
                                        onFailure = {
                                            isLoading = false
                                            Toast.makeText(context, "Verification failed: ${it.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                },
                                onVerificationFailed = {
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${it.message}", Toast.LENGTH_LONG).show()
                                }
                            )
                        } else {
                            val credential = PhoneAuthProvider.getCredential(verificationId, verificationCode)
                            FirebaseManager.signInWithPhoneCredential(credential,
                                onSuccess = {
                                    isLoading = false
                                    onAuthSuccess()
                                },
                                onFailure = {
                                    isLoading = false
                                    Toast.makeText(context, "Invalid code: ${it.message}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    } else if (isSignUp) {
                        val skillsList = skillsInput.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        FirebaseManager.signUp(
                            email, password, displayName, expertise, skillsList,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onFailure = {
                                isLoading = false
                                Toast.makeText(context, "Sign up failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        FirebaseManager.signIn(
                            email, password,
                            onSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onFailure = {
                                isLoading = false
                                Toast.makeText(context, "Sign in failed: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(
                    when {
                        isPhoneAuth && !isCodeSent -> "Send Code"
                        isPhoneAuth && isCodeSent -> "Verify & Log In"
                        isSignUp -> "Register"
                        else -> "Log In"
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = {
                    isPhoneAuth = !isPhoneAuth
                    isSignUp = false
                    isCodeSent = false
                }
            ) {
                Text(if (isPhoneAuth) "Back to Email Login" else "Sign in with Mobile Number")
            }

            if (!isPhoneAuth) {
                TextButton(
                    onClick = { isSignUp = !isSignUp },
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(if (isSignUp) "Already have an account? Sign In" else "New to the platform? Create Account")
                }
            }
        }
    }
}
