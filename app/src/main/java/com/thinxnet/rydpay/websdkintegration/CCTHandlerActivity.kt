package com.thinxnet.rydpay.websdkintegration

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.thinxnet.rydpay.websdkintegration.model.PaymentDataDto
import com.thinxnet.rydpay.websdkintegration.model.PaymentDataDtoMapper
import com.thinxnet.rydpay.websdkintegration.model.RydPayWebSdkCallbackResult
import kotlinx.serialization.json.Json

class CCTHandlerActivity : AppCompatActivity() {

    private var uri: Uri? = null
    private val json: Json by lazy {
        Json {
            isLenient = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = intent.getParcelableExtra(INTENT_EXTRA_URI)

        if (savedInstanceState == null) {
            val currentUri = uri
            if (currentUri != null) {
                val builder = CustomTabsIntent.Builder()
                val customTabsIntent = builder.build()
                customTabsIntent.launchUrl(this, currentUri)
            } else {
                finishWithCancel()
            }
        } else {
            processIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent(intent!!)
    }

    private fun processIntent(intent: Intent) {
        if (intent == null) {
            finishWithCancel()
            return
        }

        val callbackUri = intent.data
        if (callbackUri == null) {
            finishWithCancel()
            return
        }

        val isPaymentSuccessful = callbackUri.host == "finish"
        val paymentDataString = callbackUri.getQueryParameter("paymentdata")
        if (isPaymentSuccessful && paymentDataString != null) {
            try {
                val paymentData = json.decodeFromString(PaymentDataDto.serializer(), paymentDataString)
                finishWithResult(
                    RydPayWebSdkCallbackResult.Success(PaymentDataDtoMapper.transform(paymentData))
                )
            } catch (ex: Exception) {
                finishWithResult(
                    RydPayWebSdkCallbackResult.Failure("Failed to parse result from:\n${paymentDataString}")
                )
            }

        } else {
            finishWithResult(
                RydPayWebSdkCallbackResult.Failure("Unexpected callbackUri path or params:\n${callbackUri}")
            )
        }
    }

    private fun finishWithCancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun finishWithResult(rydPayWebSdkCallbackResult: RydPayWebSdkCallbackResult) {
        setResult(RESULT_OK, Intent().putExtra(INTENT_EXTRA_CALLBACK_RESULT, rydPayWebSdkCallbackResult))
        finish()
    }

    companion object {

        private const val INTENT_EXTRA_URI = "INTENT_EXTRA_URI"

        private const val INTENT_EXTRA_CALLBACK_RESULT = "INTENT_EXTRA_CALLBACK_RESULT"

        fun createLaunchIntent(context: Context, uri: Uri): Intent =
            Intent(context, CCTHandlerActivity::class.java).apply {
                putExtra(INTENT_EXTRA_URI, uri)
            }

        fun parseCallbackResultIntent(intent: Intent): RydPayWebSdkCallbackResult? =
            intent.getParcelableExtra(INTENT_EXTRA_CALLBACK_RESULT)
    }
}