package com.swiftui.io

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.*
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.browser.customtabs.CustomTabsServiceConnection
import androidx.browser.customtabs.CustomTabsSession
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.swiftui.io.databinding.ActivityHomeBinding
import java.util.*


const val GAME_LENGTH_MILLISECONDS = 3000L
const val AD_UNIT_ID = "ca-app-pub-2795821427125244/1040880006"

internal var lastSelectedImage =""
internal var loadUrl = "file:///android_asset/error.html"
internal var checkIsHomeOrNot = "https://swiftuiio.blogspot.com/" // to close whwn u r in home page
internal var booleanCheck = true

@Suppress("DEPRECATION")
class HomeActivity : Activity() {



    private var mCustomTabsServiceConnection: CustomTabsServiceConnection? = null
    private var mClient: CustomTabsClient? = null
    private var mCustomTabsSession: CustomTabsSession? = null



    //.......................................................................
    private lateinit var appUpdateManager: AppUpdateManager
    private val updateAvailable = MutableLiveData<Boolean>().apply { value = false }
    private var updateInfo: AppUpdateInfo? = null
    private var updateListener = InstallStateUpdatedListener { state: InstallState ->
        commonLog("update01:$state")
        if (state.installStatus() == InstallStatus.DOWNLOADED) {
            showUpdateSnackbar()
        }
    }
    private fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener {
            if (it.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                it.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {
                updateInfo = it
                updateAvailable.value = true
                commonLog("update01:Version code available ${it.availableVersionCode()}")
                startForInAppUpdate(updateInfo)
            } else {
                updateAvailable.value = false
                commonLog("update01:Update not available")
            }
        }
    }
    private fun startForInAppUpdate(it: AppUpdateInfo?) {
        appUpdateManager.startUpdateFlowForResult(it!!, AppUpdateType.IMMEDIATE, this, 1101)
    }
    private fun showUpdateSnackbar() {
        try{
            val snackbar = Snackbar.make(binding.parent, "An update has just been downloaded.", Snackbar.LENGTH_INDEFINITE)
                .setAction("RESTART") { appUpdateManager.completeUpdate() }
                .setBackgroundTint(ContextCompat.getColor(applicationContext, R.color.backgroundTint))
                .setActionTextColor(ContextCompat.getColor(applicationContext, R.color.actionTextColor))
            snackbar.show()
        }catch (e:java.lang.Exception){
        }
    }
    //.......................................................................

















    private var mInterstitialAd: InterstitialAd? = null
    private var mCountDownTimer: CountDownTimer? = null
    private var retryButton = false
    private var mGameIsInProgress = false
    private var mAdIsLoading: Boolean = false
    private var mTimerMilliseconds = 0L
    private var TAG = "MainActivity"
    private var booleanIsValue = 1


    private val wifiStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("test001","wifiStateReceiver : init " +isOnline(applicationContext))
            try {
                if (isOnline(applicationContext)) {
                    if(booleanCheck){
                        booleanCheck = false //wifiStateReceiver true
                        binding.webView.apply{
                            loadUrl ="https://swiftuiio.blogspot.com/"
                            loadUrl(loadUrl)
                            removeElement(binding.webView)

                        }
                    }
                    else{
                        Log.d("test001","wifiStateReceiver else")
                    }
                }
                else {
                    booleanCheck = true //wifiStateReceiver false
                }
            } catch ( e:NullPointerException) {
                e.printStackTrace()
            }

        }
    }


    fun isOnline(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // For 29 api or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->    true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->   true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->   true
                else ->     false
            }
        }
        // For below 29 api
        else {
            @Suppress("DEPRECATION")
            if (connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnectedOrConnecting) {
                return true
            }
        }
        return false
    }
    override fun onStop() {
        super.onStop()
        unregisterReceiver(wifiStateReceiver)
    }
    override fun onStart() {
        super.onStart()
        Log.d("test001","onnstart")

        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(wifiStateReceiver, intentFilter)
    }

    private lateinit var binding: ActivityHomeBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)


        mCustomTabsServiceConnection = object : CustomTabsServiceConnection() {
            override fun onCustomTabsServiceConnected(componentName: ComponentName, customTabsClient: CustomTabsClient) {
                //Pre-warming
                mClient = customTabsClient
                mClient?.warmup(0L)
                mCustomTabsSession = mClient?.newSession(null)
            }
            override fun onServiceDisconnected(name: ComponentName) {
                mClient = null
            }
        }
        CustomTabsClient.bindCustomTabsService(applicationContext, "com.android.chrome", mCustomTabsServiceConnection!!)



        binding.progressBar.visibility=View.VISIBLE
        binding.progressBar.startAnimation(AnimationUtils.loadAnimation(applicationContext, R.anim.loading))

        Log.d("test001","onCreate")

        var id = "ABCDEF012345"
        try {
            // Initialize the Mobile Ads SDK.
            MobileAds.initialize(this) {}

            try {
                id = UUID.randomUUID().toString()
            }catch (e:Exception){}
            // Set your test devices. Check your logcat output for the hashed device ID to
            // get test ads on a physical device. e.g.
            // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("ABCDEF012345"))
            // to get test ads on this device."
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(listOf(id))
                    .build()
            )

            // Create the "retry" button, which triggers an interstitial between game plays.
            // binding.retryButton.visibility = View.INVISIBLE
            retryButton = false

            // binding.retryButton.setOnClickListener { showInterstitial() }

            // Kick off the first play of the "game."
            startGame()
        }catch (e:Exception){}


        binding.noInternet.setOnClickListener {
            return@setOnClickListener
        }

        binding.retry.setOnClickListener {
            booleanCheck = true //retry btn
            val intent = Intent(applicationContext, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        binding.webView.apply{
         //   loadUrl("file:///android_asset/index.html")

            if (isOnline(applicationContext)) {
                binding.noInternet.visibility=View.GONE
                loadUrl("https://swiftuiio.blogspot.com/")
                Log.d("error", "yes")
            }else {
                binding.noInternet.visibility=View.VISIBLE
                //   Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
                //loadUrl("file:///android_asset/www/index.html")
                Log.d("error", "nio")
            }









            // to play video on a web view
            settings.javaScriptEnabled = true
            // to verify that the client requesting your web page is actually your Android app.
            settings.userAgentString = System.getProperty("http.agent") //Dalvik/2.1.0 (Linux; U; Android 11; M2012K11I Build/RKQ1.201112.002)
            settings.useWideViewPort = true
            // Enable zooming in web view
            settings.setSupportZoom(false)
            settings.builtInZoomControls = false
            settings.displayZoomControls = false
            // Zoom web view text
            settings.textZoom = 100
            //settings.pluginState = WebSettings.PluginState.ON
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.mediaPlaybackRequiresUserGesture = false
            // More optional settings, you can enable it by yourself
            settings.domStorageEnabled = true
            settings.setSupportMultipleWindows(true)
            settings.loadWithOverviewMode = true
            settings.allowContentAccess = true
            settings.setGeolocationEnabled(true)
            settings.allowUniversalAccessFromFileURLs = true
            settings.allowFileAccess = true
            // WebView settings
            fitsSystemWindows = true
            setLayerType(View.LAYER_TYPE_HARDWARE, null)

            addJavascriptInterface(WebAppInterface(binding,context), "Android")

            removeElement(binding.webView)


            settings.pluginState = WebSettings.PluginState.ON
            webViewClient = MyWebClient()

  /*          setInitialScale(1)
            settings.loadWithOverviewMode = true
            settings.useWideViewPort = true
            settings.saveFormData = true
            settings.setEnableSmoothTransition(true)*/

            // feature 1 : dark mode (auto system setup)
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_AUTO)
            }
        }


        try{
            //.......................................................................
            appUpdateManager = AppUpdateManagerFactory.create(this)
            appUpdateManager.registerListener(updateListener)
            checkForUpdate()
        }catch (e:Exception){
            commonLog("update01:Update e1 ${e.message}")
        }



    }

    /** Instantiate the interface and set the context  */
    class WebAppInterface(
        var binding: ActivityHomeBinding,
        private val mContext: Context,
   ) {
        /** Show a toast from the web page  */
        @JavascriptInterface
        fun showToast(toast: String) {
            // toast.replace("s320","s8000")

            Log.d("f11",""+toast)

            if(toast.contains("https://blogger.googleusercontent.com/img/a/AVvXsEj85AkMGbjW7-72sVvmCdB-C298bXb91ek_SCKLQXf9Ug4q68Y5gVFHAVv47p_P9lQlcyxh8VOmWcsMs6XbiFLFc_boNtzCd3O34Ya-ebfJ3qYjo76yFAovxt8rO3BWj0xKtaYnKU5_HIKCIZAiPanJXh7nGbJPo4CGJu0yXXanIQ7LPq_PCpXf6LA_=s800")){
                return
            }

            if(toast=="InternetX1"){
                booleanCheck = true // InternetX1
           /*     val intent = Intent(mContext, CodeLabsActivity::class.java)
                mContext.startActivity(intent)*/
            }
            else{

                if (isOnline(mContext)) {
                    lastSelectedImage =toast.replace("s320","s8000")
                    val intent = Intent(mContext, DetailsActivity::class.java)
                    mContext.startActivity(intent)
                }
                else{

                    //need to change gradient custom snackbar
                    Snackbar.make(binding.parent, "Please check your internet connection or try again later.", Snackbar.LENGTH_LONG)
                        .setBackgroundTint(ContextCompat.getColor(mContext, R.color.backgroundTint))
                        .setActionTextColor(ContextCompat.getColor(mContext, R.color.actionTextColor))
                        .show()
                }



            }
        }

        private fun isOnline(mContext: Context): Boolean {
            val connectivityManager = mContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            // For 29 api or above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
                return when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->    true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->   true
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->   true
                    else ->     false
                }
            }
            // For below 29 api
            else {
                @Suppress("DEPRECATION")
                if (connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnectedOrConnecting) {
                    return true
                }
            }
            return false
        }
    }

    internal inner class MyWebClient() : WebViewClient() {

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            Log.d("test001", "onReceivedError")
            binding.progressBar.visibility=View.GONE
            binding.noInternet.visibility=View.VISIBLE
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            Log.d("test001", "onPageStarted" )
            removeElement(binding.webView)
            binding.noInternet.visibility=View.GONE

            binding.progressBar.visibility=View.VISIBLE
            binding.progressBar.startAnimation(AnimationUtils.loadAnimation(applicationContext, R.anim.loading))

        }
        override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            Log.d("test001", "onPageCommitVisible")
            binding.progressBar.visibility=View.GONE

        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d("test001", "onPageFinished")
            removeElement(binding.webView)

            if (url != null) {
                checkIsHomeOrNot = url
                Log.d("f1","url: $checkIsHomeOrNot")//url.contains("https") &&
            }
            else{
                Log.d("test001", "onPageFinished else")
            }
            booleanIsValue++
            try{
                Log.d(TAG,"onPageFinished booleanIs: $booleanIsValue")
                if(booleanIsValue>25){
                    booleanIsValue = 1
                    showInterstitial()
                }
            }catch (e:Exception){}
        }

        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Log.d("test001","shouldOverrideUrlLoading: $url")//url.contains("https") &&


            if (!url.contains("swiftuiio.com")) { //  && url.contains("signin")
                chromeTab(url)
                return true
            }



           /* if (url.contains("accounts.google.com")) { //  && url.contains("signin")
                loadPage("https://www.instagram.com/boltuix/")
                return true
            }
            else if (!url.contains("codelabs")) { // Could be cleverer and use a regex
                loadPage("https://www.boltuix.com/")
                return true
            }
            else if (url.contains("facebook.com")) { // Could be cleverer and use a regex
                loadPage("https://www.facebook.com/groups/561488101984069")
                return true
            }
            else if (url.contains("youtube.com")) { // Could be cleverer and use a regex
                loadPage("https://www.youtube.com/channel/UCr6xjVwoyVkx7Q5AMEoUzhg/playlists")
                return true
            }
            else if (url.contains("twitter.com")) { // Could be cleverer and use a regex
                loadPage("https://twitter.com/BoltUix")
                return true
            }
            else if (url.contains("medium.com")) { // Could be cleverer and use a regex
                loadPage("https://www.boltuix.com/")
                return true
            }
            else if (url.contains("blogspot.com")) { // Could be cleverer and use a regex
                loadPage("https://www.boltuix.com/")
                return true
            }

            else if (url != null && url.startsWith("mailto")) {


                try {
                    var i2 =  Intent("android.intent.action.SEND")
                    i2.type = "text/plain"
                    i2.putExtra("android.intent.extra.SUBJECT", "Codelabs : Learn to build anything")
                    i2.putExtra("android.intent.extra.TEXT", "\nWe can also play game when offline \n\nhttps://play.google.com/store/apps/details?id=com.swiftui.io \n\n")
                    startActivity(Intent.createChooser(i2, "Share this app"))
                    startActivity(i2)
                }catch (e:Exception){}


                return true;
            }
*/

            return false
        }

        private fun chromeTab(url: String) {
            try {
                val customTabsIntent = CustomTabsIntent.Builder(mCustomTabsSession)
                    //.setToolbarColor(color)
                    //.setToolbarColor(ContextCompat.getColor(requireContext(),R.color.color2))
                    .setShowTitle(true)
                    .build()
                customTabsIntent.launchUrl(this@HomeActivity, Uri.parse(url))
            }
            catch (e: ActivityNotFoundException){
                e.printStackTrace()
            }
            catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private fun loadPage(url: String) {
        try {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }catch (e:Exception){}
    }


    override fun onBackPressed() {

        try{
            appUpdateManager.unregisterListener(updateListener)
        }catch (e:Exception){
            commonLog("update01:Update e2 ${e.message}")
        }


        if (binding.webView != null) {
            if (binding.webView.canGoBack() && checkIsHomeOrNot!="https://swiftuiio.blogspot.com/") {
                binding.webView.goBack()
                return
            }
        }
        super.onBackPressed()


    }

    private fun commonLog(s: String) {

    }

    //.......................................................................
    //.......................................................................
    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            this, AD_UNIT_ID, adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.d(TAG, adError.message)
                    mInterstitialAd = null
                    mAdIsLoading = false
                    val error = "domain: ${adError.domain}, code: ${adError.code}, " +
                            "message: ${adError.message}"
                    //Toast.makeText(this@MainActivity2, "onAdFailedToLoad() with error $error", Toast.LENGTH_SHORT).show()
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    Log.d(TAG, "Ad was loaded.")
                    mInterstitialAd = interstitialAd
                    mAdIsLoading = false
                    // Toast.makeText(this@MainActivity2, "onAdLoaded()", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }



    private fun removeElement(webView: WebView) {
     /*   webView.loadUrl(
            "javascript:(function() { " +
                    "document.getElementById('id1').style.display='none'; " +
                    "})()"
        )
        webView.loadUrl(
            "javascript:(function() { " +
                    "document.getElementById('id2').style.display='none'; " +
                    "})()"
        )*/
    }



        // Create the game timer, which counts down to the end of the level
    // and shows the "retry" button.
    private fun createTimer(milliseconds: Long) {
        mCountDownTimer?.cancel()

        mCountDownTimer = object : CountDownTimer(milliseconds, 50) {
            override fun onTick(millisUntilFinished: Long) {
                mTimerMilliseconds = millisUntilFinished
                //binding.timer.text = "seconds remaining: ${ millisUntilFinished / 1000 + 1 }"
            }

            override fun onFinish() {
                mGameIsInProgress = false
                //binding.timer.text = "done!"
                //binding.retryButton.visibility = View.VISIBLE
                retryButton = true
            }
        }
    }

    // Show the ad if it's ready. Otherwise toast and restart the game.
    private fun showInterstitial() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Ad was dismissed.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mInterstitialAd = null
                    loadAd()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    Log.d(TAG, "Ad failed to show.")
                    // Don't forget to set the ad reference to null so you
                    // don't show the ad a second time.
                    mInterstitialAd = null
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Ad showed fullscreen content.")
                    // Called when ad is dismissed.
                }
            }
            mInterstitialAd?.show(this)
        } else {
            // Toast.makeText(this, "Ad wasn't loaded.", Toast.LENGTH_SHORT).show()
            startGame()
        }
    }

    // Request a new ad if one isn't already loaded, hide the button, and kick off the timer.
    private fun startGame() {
        if (!mAdIsLoading && mInterstitialAd == null) {
            mAdIsLoading = true
            loadAd()
        }
        retryButton = false
        //binding.retryButton.visibility = View.INVISIBLE
        resumeGame(GAME_LENGTH_MILLISECONDS)

    }
    private fun resumeGame(milliseconds: Long) {
        // Create a new timer for the correct length and start it.
        mGameIsInProgress = true
        mTimerMilliseconds = milliseconds
        createTimer(milliseconds)
        mCountDownTimer?.start()
    }
    // Resume the game if it's in progress.
    @SuppressLint("SetJavaScriptEnabled")
    public override fun onResume() {
        super.onResume()
        if (mGameIsInProgress) {
            resumeGame(mTimerMilliseconds)
        }
    }
    public override fun onPause() {
        mCountDownTimer?.cancel()
        super.onPause()
    }

}