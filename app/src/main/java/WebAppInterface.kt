package com.gracecode.network

import android.webkit.JavascriptInterface

/**
 * Handles communication from the Next.js web application inside WebView back to Native Android.
 */
class WebAppInterface(
    private val onLaunchGoogleSignIn: () -> Unit,
    private val onLaunchPhoneSignIn: () -> Unit
) {
    @JavascriptInterface
    fun launchGoogleSignIn() {
        onLaunchGoogleSignIn()
    }

    @JavascriptInterface
    fun launchPhoneSignIn() {
        onLaunchPhoneSignIn()
    }
}