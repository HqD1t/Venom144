package com.venom.club.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.venom.club.data.Demo
import com.venom.club.data.ProfileRepo
import com.venom.club.data.normalizePhone
import com.venom.club.data.sha256
import com.venom.club.data.toE164
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

enum class AuthStep { PHONE, CODE, TERMS, PIN_CREATE, PIN_ENTER, DONE }

class AuthViewModel : ViewModel() {
    val step = MutableStateFlow(AuthStep.PHONE)
    val phone8 = MutableStateFlow("")
    val error = MutableStateFlow<String?>(null)
    val loading = MutableStateFlow(false)
    private var verificationId: String? = null

    init {
        // Уже залогинен? Тогда сразу к пин-коду.
        if (Firebase.auth.currentUser != null) step.value = AuthStep.PIN_ENTER
    }

    /** Тестовый вход без регистрации: всё приложение работает на локальных демо-данных. */
    fun enterDemoMode() {
        Demo.enabled = true
        step.value = AuthStep.DONE
    }

    fun sendCode(rawPhone: String, activity: Activity) {
        val p = normalizePhone(rawPhone)
        if (p == null) { error.value = "Введите номер через 8: 8XXXXXXXXXX"; return }
        phone8.value = p
        loading.value = true
        val options = PhoneAuthOptions.newBuilder(Firebase.auth)
            .setPhoneNumber(toE164(p))
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(cred: PhoneAuthCredential) = signIn(cred)
                override fun onVerificationFailed(e: FirebaseException) {
                    loading.value = false; error.value = e.localizedMessage
                }
                override fun onCodeSent(vId: String, t: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = vId; loading.value = false; step.value = AuthStep.CODE
                }
            }).build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun confirmCode(code: String) {
        val vId = verificationId ?: return
        signIn(PhoneAuthProvider.getCredential(vId, code))
    }

    private fun signIn(cred: PhoneAuthCredential) {
        loading.value = true
        viewModelScope.launch {
            try {
                Firebase.auth.signInWithCredential(cred).await()
                ProfileRepo.ensureCreated(phone8.value)
                val prof = ProfileRepo.get(ProfileRepo.uid!!)
                step.value = when {
                    prof?.acceptedTermsAt == null -> AuthStep.TERMS
                    prof.pinHash.isBlank() -> AuthStep.PIN_CREATE
                    else -> AuthStep.PIN_ENTER
                }
            } catch (e: Exception) {
                error.value = e.localizedMessage
            } finally { loading.value = false }
        }
    }

    fun acceptTerms() = viewModelScope.launch {
        ProfileRepo.acceptTerms()
        val prof = ProfileRepo.get(ProfileRepo.uid!!)
        step.value = if (prof?.pinHash.isNullOrBlank()) AuthStep.PIN_CREATE else AuthStep.PIN_ENTER
    }

    fun createPin(pin: String) = viewModelScope.launch {
        ProfileRepo.setPin(pin)
        step.value = AuthStep.DONE
    }

    fun checkPin(pin: String) = viewModelScope.launch {
        val prof = ProfileRepo.get(ProfileRepo.uid ?: return@launch)
        if (prof?.pinHash == sha256(pin)) step.value = AuthStep.DONE
        else error.value = "Неверный пин-код"
    }
}
