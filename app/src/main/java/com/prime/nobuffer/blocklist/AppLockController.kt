package com.prime.nobuffer.blocklist

import android.app.Activity
import android.content.Context
import android.hardware.biometrics.BiometricPrompt as FrameworkBiometricPrompt
import android.os.CancellationSignal
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class AppLockController(
    private val appLockStore: AppLockStore,
    private val passwordStore: LockPasswordStore
) {
    @Volatile
    private var sessionUnlocked: Boolean = false

    val enabled: kotlinx.coroutines.flow.Flow<Boolean> = appLockStore.enabled

    val lastUnlockAt: kotlinx.coroutines.flow.Flow<Long> = appLockStore.lastUnlockAt

    val lockTimeoutMillis: kotlinx.coroutines.flow.Flow<Long> = appLockStore.lockTimeoutMillis

    fun shouldLock(): Boolean {
        if (sessionUnlocked) return false
        return runBlocking(Dispatchers.IO) {
            if (!appLockStore.isEnabledNow()) return@runBlocking false
            val timeout = appLockStore.lockTimeoutMillisNow()
            if (timeout <= 0L) return@runBlocking true
            val last = appLockStore.lastUnlockAtNow()
            System.currentTimeMillis() - last >= timeout
        }
    }

    suspend fun unlockWithPassword(pw: String): String? {
        val error = withContext(Dispatchers.Default) {
            when (val result = passwordStore.verify(pw)) {
                PasswordVerifyResult.Ok -> null
                PasswordVerifyResult.NoPassword -> "No lock password is set"
                is PasswordVerifyResult.Wrong -> SiteBlocker.wrongPasswordMessage(result)
                is PasswordVerifyResult.Locked -> LockPasswordStore.lockoutMessage(result.remainingMillis)
            }
        }
        if (error != null) return error
        markUnlocked()
        return null
    }

    suspend fun markUnlocked() {
        sessionUnlocked = true
        appLockStore.setLastUnlockAt(System.currentTimeMillis())
    }

    /** Call from Activity.onStop so timeout 0 re-locks when the app leaves the foreground. */
    fun onActivityStop() {
        sessionUnlocked = false
    }

    fun canUseBiometric(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val strong = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (strong == BiometricManager.BIOMETRIC_SUCCESS) return true
        val weak = manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)
        return weak == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Shows a biometric prompt. [onSuccess] is invoked on the main thread after a match.
     * Negative-button / cancel falls through to [onUsePassword].
     */
    fun authenticateBiometric(
        activity: Activity,
        onSuccess: () -> Unit,
        onUsePassword: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!canUseBiometric(activity)) {
            onUsePassword()
            return
        }
        if (activity is FragmentActivity) {
            authenticateAndroidX(activity, onSuccess, onUsePassword, onError)
        } else {
            authenticateFramework(activity, onSuccess, onUsePassword, onError)
        }
    }

    private fun authenticateAndroidX(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onUsePassword: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                        BiometricPrompt.ERROR_USER_CANCELED,
                        BiometricPrompt.ERROR_CANCELED -> onUsePassword()
                        else -> onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() = Unit
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock NoBuffer")
            .setSubtitle("Confirm it's you")
            .setNegativeButtonText("Use password")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()
        prompt.authenticate(info)
    }

    private fun authenticateFramework(
        activity: Activity,
        onSuccess: () -> Unit,
        onUsePassword: () -> Unit,
        onError: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = FrameworkBiometricPrompt.Builder(activity)
            .setTitle("Unlock NoBuffer")
            .setSubtitle("Confirm it's you")
            .setNegativeButton("Use password", executor) { _, _ -> onUsePassword() }
            .build()
        prompt.authenticate(
            CancellationSignal(),
            executor,
            object : FrameworkBiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: FrameworkBiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    when (errorCode) {
                        FrameworkBiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED,
                        FrameworkBiometricPrompt.BIOMETRIC_ERROR_CANCELED -> onUsePassword()
                        else -> onError(errString.toString())
                    }
                }

                override fun onAuthenticationFailed() = Unit
            }
        )
    }
}
