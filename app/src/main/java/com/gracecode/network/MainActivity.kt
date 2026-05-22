package com.gracecode.network

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeColabaiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val currentUser by FirebaseManager.currentUserFlow.collectAsState()
                    val context = LocalContext.current
                    var showWebView by remember { mutableStateOf(true) }

                    if (currentUser == null) {
                        AuthScreen(
                            onAuthSuccess = {
                                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                            }
                        )
                    } else {
                        if (showWebView) {
                            AppWebViewShell(
                                onSwitchToNative = { showWebView = false }
                            )
                        } else {
                            MainDashboard(user = currentUser!!, onSwitchToWeb = { showWebView = true })
                        }
                    }
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun AppWebViewShell(onSwitchToNative: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Web application URL - you can swap this configuration easily
    val appUrl = "https://ais-pre-smcbipak7hxdd2q72oeo6r-47851105980.asia-east1.run.app"
    val webClientId = "631538881426-grfl39vr97v9bsnnrmc7l74fkr06oq6q.apps.googleusercontent.com"

    val credentialManager = remember { CredentialManager.create(context) }

    // Keeps reference to evaluate JavaScript later
    var webViewInstance: WebView? = null

    // Google Sign-In orchestration
    val triggerNativeSignIn: () -> Unit = {
        coroutineScope.launch {
            try {
                // Configure Google ID Option (Identity Credential Manager)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(webClientId)
                    .setAutoSelectEnabled(true)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                Log.d("AndroidBridge", "Launching Native Credential Manager...")
                val result = credentialManager.getCredential(
                    context = context,
                    request = request
                )

                // Process acquired sign in credentials
                when (val credential = result.credential) {
                    is GoogleIdTokenCredential -> {
                        val idToken = credential.idToken
                        Log.d("AndroidBridge", "Token received! Injecting to webapp...")
                        
                        // Inject into Next.js Web App
                        webViewInstance?.post {
                            webViewInstance?.evaluateJavascript(
                                "window.signInWithAndroidToken(\"$idToken\")"
                            ) { reply ->
                                Log.d("AndroidBridge", "Web app evaluated token: $reply")
                            }
                        }
                    }
                    else -> {
                        Log.w("AndroidBridge", "Unknown credential type received.")
                    }
                }
            } catch (e: GetCredentialException) {
                Log.e("AndroidBridge", "Credential retrieval failed", e)
                Toast.makeText(context, "Sign-in cancelled or failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("AndroidBridge", "Unexpected exception during Sign-In", e)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WeColab.ai (Web)", fontSize = 18.sp) },
                actions = {
                    IconButton(onClick = onSwitchToNative) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Native Dashboard")
                    }
                    IconButton(onClick = { FirebaseManager.signOut() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            )
        }
    ) { padding ->
        // AndroidView holds the system web container
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { ctx ->
                WebView(ctx).apply {
                    webViewInstance = this
                    webViewClient = object : WebViewClient() {
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            Log.e("WebViewError", "Error $errorCode: $description at $failingUrl")
                            if (failingUrl?.startsWith(appUrl) == true) {
                                Toast.makeText(context, "Web App Error. Try Native Mode.", Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            Log.d("WebView", "Finished loading: $url")
                        }
                    }
                    
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        databaseEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        userAgentString = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
                        allowFileAccess = true
                        allowContentAccess = true
                    }

                    addJavascriptInterface(
                        WebAppInterface(
                            onLaunchGoogleSignIn = triggerNativeSignIn,
                            onLaunchPhoneSignIn = {
                                Toast.makeText(context, "Switching to Native Mobile Login...", Toast.LENGTH_SHORT).show()
                                FirebaseManager.signOut() 
                            }
                        ),
                        "AndroidBridge"
                    )

                    loadUrl(appUrl)
                }
            },
            update = { webView ->
                webViewInstance = webView
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboard(user: User, onSwitchToWeb: () -> Unit) {
    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        FirebaseManager.getProjects(
            onSuccess = { projects = it },
            onFailure = { Log.e("Dashboard", "Failed to load projects", it) }
        )
        FirebaseManager.getPosts(
            onSuccess = { 
                posts = it
                isLoading = false
            },
            onFailure = { 
                Log.e("Dashboard", "Failed to load posts", it)
                isLoading = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("WeColab Dashboard") },
                actions = {
                    IconButton(onClick = onSwitchToWeb) {
                        Icon(Icons.Default.Web, contentDescription = "Switch to Web")
                    }
                    IconButton(onClick = { FirebaseManager.signOut() }) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text("Welcome, ${user.displayName.ifEmpty { "Contributor" }}!", style = MaterialTheme.typography.headlineSmall)
                    Text(user.email.ifEmpty { user.phoneNumber }, color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Latest Projects", style = MaterialTheme.typography.titleMedium)
                }

                if (projects.isEmpty()) {
                    item { Text("No projects found. Firestore might be offline.", color = MaterialTheme.colorScheme.error) }
                } else {
                    items(projects) { project ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(project.name, style = MaterialTheme.typography.titleSmall)
                                Text(project.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Community Posts", style = MaterialTheme.typography.titleMedium)
                }

                if (posts.isEmpty()) {
                    item { Text("No posts yet.") }
                } else {
                    items(posts) { post ->
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text(post.authorName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(post.content)
                            HorizontalDivider(Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }
    }
}
