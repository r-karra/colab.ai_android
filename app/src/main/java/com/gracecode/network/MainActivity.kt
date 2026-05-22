package com.gracecode.network

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppWebViewShell()
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
@Composable
fun AppWebViewShell() {
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

    // AndroidView holds the system web container
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            WebView(ctx).apply {
                webViewInstance = this
                webViewClient = WebViewClient() // Keeps navigation within WebView
                
                // WebView configuration parameters
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    loadWithOverviewMode = true
                    useWideViewPort = true
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }

                // Attach JavaScript Bridge Bridge
                addJavascriptInterface(
                    WebAppInterface(onLaunchSignIn = triggerNativeSignIn),
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
