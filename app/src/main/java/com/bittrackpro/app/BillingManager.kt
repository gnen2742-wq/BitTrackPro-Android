package com.bittrackpro.app

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

class BillingManager(
    context: Context,
    private val onEntitlementChanged: (Boolean) -> Unit
) : PurchasesUpdatedListener {
    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    fun connect(onReady: () -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) onReady()
            }
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    fun launchSubscription(activity: Activity, productDetails: ProductDetails, offerToken: String) {
        val params = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(productDetails).setOfferToken(offerToken).build()
        billingClient.launchBillingFlow(activity, BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build())
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            onEntitlementChanged(purchases.any { it.purchaseState == Purchase.PurchaseState.PURCHASED })
        }
    }
}
