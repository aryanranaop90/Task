package com.example.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UnityAdsManager {
    private const val TAG = "UnityAdsManager"
    const val GAME_ID = "800005794"
    const val REWARDED_PLACEMENT = "Rewarded_Android"
    const val BANNER_PLACEMENT = "Banner_Android"
    const val TEST_MODE = true

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isAdLoaded = MutableStateFlow(false)
    val isAdLoaded: StateFlow<Boolean> = _isAdLoaded.asStateFlow()

    private val _adStatus = MutableStateFlow("Uninitialized")
    val adStatus: StateFlow<String> = _adStatus.asStateFlow()

    fun initialize(context: Context) {
        if (_isInitialized.value) return

        _adStatus.value = "Initializing Unity..."
        Log.d(TAG, "Initializing Unity Ads with Game ID: $GAME_ID")

        try {
            UnityAds.initialize(context.applicationContext, GAME_ID, TEST_MODE, object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.d(TAG, "Unity Ads Initialization Complete")
                    _isInitialized.value = true
                    _adStatus.value = "SDK Initialized"
                    loadRewardedAd()
                }

                override fun onInitializationFailed(error: UnityAds.UnityAdsInitializationError?, message: String?) {
                    Log.e(TAG, "Unity Ads Initialization Failed: $message")
                    _isInitialized.value = false
                    _adStatus.value = "Init Failed: ${message ?: "Unknown"}"
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception initializing Unity Ads", e)
            _adStatus.value = "Init Exception: ${e.message}"
        }
    }

    fun loadRewardedAd() {
        if (!_isInitialized.value) {
            Log.w(TAG, "Cannot load ad before initialization")
            return
        }

        _isAdLoaded.value = false
        _adStatus.value = "Loading Rewarded Ad..."
        Log.d(TAG, "Loading rewarded ad for placement: $REWARDED_PLACEMENT")

        try {
            UnityAds.load(REWARDED_PLACEMENT, object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String?) {
                    Log.d(TAG, "Ad Loaded successfully: $placementId")
                    if (placementId == REWARDED_PLACEMENT) {
                        _isAdLoaded.value = true
                        _adStatus.value = "Rewarded Ad Ready!"
                    }
                }

                override fun onUnityAdsFailedToLoad(placementId: String?, error: UnityAds.UnityAdsLoadError?, message: String?) {
                    Log.e(TAG, "Ad Failed to Load ($placementId): $message")
                    _isAdLoaded.value = false
                    _adStatus.value = "Load Failed: $message"
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception loading Unity Ads", e)
            _adStatus.value = "Load Exception: ${e.message}"
        }
    }

    fun showRewardedAd(activity: Activity, onRewardEarned: () -> Unit) {
        if (!_isAdLoaded.value) {
            Log.w(TAG, "Ad is not loaded yet")
            _adStatus.value = "Ad Not Ready"
            loadRewardedAd()
            return
        }

        _adStatus.value = "Displaying Ad..."
        Log.d(TAG, "Displaying ad for placement: $REWARDED_PLACEMENT")

        try {
            UnityAds.show(activity, REWARDED_PLACEMENT, object : IUnityAdsShowListener {
                override fun onUnityAdsShowFailure(placementId: String?, error: UnityAds.UnityAdsShowError?, message: String?) {
                    Log.e(TAG, "Ad Show Failed ($placementId): $message")
                    _isAdLoaded.value = false
                    _adStatus.value = "Show Failed: $message"
                    loadRewardedAd()
                }

                override fun onUnityAdsShowStart(placementId: String?) {
                    Log.d(TAG, "Ad Started Show: $placementId")
                    _adStatus.value = "Ad Playing..."
                }

                override fun onUnityAdsShowClick(placementId: String?) {
                    Log.d(TAG, "Ad Clicked: $placementId")
                }

                override fun onUnityAdsShowComplete(placementId: String?, state: UnityAds.UnityAdsShowCompletionState?) {
                    Log.d(TAG, "Ad Completed with state: $state")
                    _isAdLoaded.value = false
                    if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                        _adStatus.value = "Reward Claimed!"
                        onRewardEarned()
                    } else {
                        _adStatus.value = "Ad Closed/Skipped"
                    }
                    loadRewardedAd()
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "Exception showing Unity Ads", e)
            _adStatus.value = "Show Exception: ${e.message}"
            loadRewardedAd()
        }
    }
}
