package com.searcher.zonenews.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import com.searcher.zonenews.net.AppHttpService
import com.searcher.zonenews.utils.ToastUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appHttpService: AppHttpService
) {

    private val _billingClient: BillingClient
    private val _productDetails = MutableStateFlow<List<ProductDetails>>(emptyList())
    val productDetails: StateFlow<List<ProductDetails>> = _productDetails.asStateFlow()

    private val _purchaseState = MutableStateFlow<PurchaseState>(PurchaseState.Idle)
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    companion object {
        const val PRODUCT_ID_PRO = "pro_subscription"
        const val BASE_PLAN_MONTHLY = "monthlysubscription"
        const val BASE_PLAN_YEARLY = "yearlysubscription"
        private const val TAG = "BillingManager"
    }

    sealed class PurchaseState {
        object Idle : PurchaseState()
        object Loading : PurchaseState()
        object PurchaseSuccess : PurchaseState()
        data class Error(val message: String) : PurchaseState()
    }

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                handlePurchase(purchase)
            }
        } else if (billingResult.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            _purchaseState.value = PurchaseState.Idle
        } else {
            _purchaseState.value = PurchaseState.Error(context.getString(com.searcher.zonenews.R.string.billing_error_purchase_failed, billingResult.debugMessage))
        }
    }

    init {
        _billingClient = BillingClient.newBuilder(context)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases()
            .build()
        
        startConnection()
    }

    fun startConnection() {
        _billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    querySubscriptionProductDetails()
                    queryPurchases() // Check for existing purchases
                } else {
                    Log.e(TAG, "Billing setup failed: ${billingResult.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request
                Log.e(TAG, "Billing service disconnected")
            }
        })
    }

    private fun querySubscriptionProductDetails() {
        val queryProductDetailsParams = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_PRO)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()

        _billingClient.queryProductDetailsAsync(queryProductDetailsParams) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                _productDetails.value = productDetailsList
            } else {
                Log.e(TAG, "Query product details failed: ${billingResult.debugMessage}")
            }
        }
    }

    fun launchPurchaseFlow(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        
        _purchaseState.value = PurchaseState.Loading
        _billingClient.launchBillingFlow(activity, billingFlowParams)
    }

    // Handle both new purchases and restored purchases
    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
            if (!purchase.isAcknowledged) {
                val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()

                _billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                    if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        verifyPurchaseWithBackend(purchase)
                    } else {
                         _purchaseState.value = PurchaseState.Error(context.getString(com.searcher.zonenews.R.string.billing_error_acknowledge_failed, billingResult.debugMessage))
                    }
                }
            } else {
                verifyPurchaseWithBackend(purchase)
            }
        }
    }
    
    // Call backend to verify and update user status
    private fun verifyPurchaseWithBackend(purchase: Purchase) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonObject = JSONObject()
                jsonObject.put("purchaseToken", purchase.purchaseToken)
                jsonObject.put("productId", purchase.products.firstOrNull() ?: "")
                jsonObject.put("packageName", purchase.packageName)
                
                val requestBody = jsonObject.toString()
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val response = appHttpService.verifyPurchase(requestBody)
                
                withContext(Dispatchers.Main) {
                    when (response) {
                        is com.searcher.zonenews.utils.network.exception.NetworkResponse.Success -> {
                            val body = response.body
                            if (body.code == 200) {
                                _purchaseState.value = PurchaseState.PurchaseSuccess
                            } else {
                                _purchaseState.value = PurchaseState.Error(body.msg ?: context.getString(com.searcher.zonenews.R.string.billing_error_verification_failed))
                            }
                        }
                        is com.searcher.zonenews.utils.network.exception.NetworkResponse.NetError -> {
                            _purchaseState.value = PurchaseState.Error(response.errorMsg ?: context.getString(com.searcher.zonenews.R.string.billing_error_network))
                        }
                        is com.searcher.zonenews.utils.network.exception.NetworkResponse.UnknownError -> {
                            _purchaseState.value = PurchaseState.Error(response.error?.message ?: context.getString(com.searcher.zonenews.R.string.billing_error_unknown))
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _purchaseState.value = PurchaseState.Error(context.getString(com.searcher.zonenews.R.string.billing_error_network))
                }
            }
        }
    }

    fun restorePurchases() {
        _purchaseState.value = PurchaseState.Loading
        _billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                if (purchases.isEmpty()) {
                    _purchaseState.value = PurchaseState.Error(context.getString(com.searcher.zonenews.R.string.billing_error_no_subscriptions))
                } else {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            } else {
                _purchaseState.value = PurchaseState.Error(context.getString(com.searcher.zonenews.R.string.billing_error_restore_failed, billingResult.debugMessage))
            }
        }
    }
    
    // Auto-check purchases on init to catch any pending transactions or existing states
    private fun queryPurchases() {
        _billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                for (purchase in purchases) {
                    if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
                        handlePurchase(purchase)
                    }
                }
            }
        }
    }
}
