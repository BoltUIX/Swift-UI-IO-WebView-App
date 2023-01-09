package com.swiftui.io

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.webkit.*
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.swiftui.io.databinding.ActivityDetailsBinding
import java.io.File
import java.util.*


@Suppress("DEPRECATION")
class DetailsActivity : Activity() {
    private var mInterstitialAd: InterstitialAd? = null
    private var mCountDownTimer: CountDownTimer? = null
    private var retryButton = false
    private var mGameIsInProgress = false
    private var mAdIsLoading: Boolean = false
    private var mTimerMilliseconds = 0L
    private var TAG = "MainActivity"
    private var booleanIsValue = 1


    override fun onActivityResult(requestCode: Int, resultCode: Int, @Nullable data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2296) {
            if (SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // perform action when allow permission success
                } else {
                    Toast.makeText(this, "Allow permission for storage access!", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }



    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE -> {
                // If request is cancelled, the result arrays are empty.
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission was granted, yay!
                    // Download the Image
                    downloadImage(lastSelectedImage)
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.

                }
                return
            }
            // Add other 'when' lines to check for other
            // permissions this app might request.
            else -> {
                // Ignore all other requests.
            }
        }
    }




    @TargetApi(Build.VERSION_CODES.M)
    fun askPermissions() {

        if (SDK_INT >= Build.VERSION_CODES.R) {

            if(Environment.isExternalStorageManager()){
                downloadImage(lastSelectedImage)
            }
            else{
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.addCategory("android.intent.category.DEFAULT")
                    intent.data = Uri.parse(String.format("package:%s", applicationContext.packageName))
                    startActivityForResult(intent, 2296)
                } catch (e: java.lang.Exception) {
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION
                    startActivityForResult(intent, 2296)
                }
            }

        }
        else{
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    AlertDialog.Builder(this)
                        .setTitle("Permission required")
                        .setMessage("Permission required to save photos from the Web.")
                        .setPositiveButton("Accept") { dialog, id ->
                            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                            finish()
                        }
                        .setNegativeButton("Deny") { dialog, id -> dialog.cancel() }
                        .show()
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE)
                    // MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }
            else {
                // Permission has already been granted
                downloadImage(lastSelectedImage)
            }
        }



    }


    companion object {
        private const val MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1
    }





    private val wifiStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                if (isOnline(applicationContext)) {
                    binding.webView.apply{
                        loadUrl(lastSelectedImage)
                    }
                } else {
                    try {
                        binding.webView.apply{
                            onBackPressed()
                        }
                    } catch (e: java.lang.Exception) {
                        Log.e("error", e.toString())
                    }
                }
            } catch ( e:NullPointerException) {
                e.printStackTrace()
            }

        }
    }

    private fun removeElement(webView: WebView) {
       /* webView.loadUrl("javascript:(function() { " +
                "document.getElementsByClassName('devsite-footer-utility nocontent')[0].style.display='none'; " +
                "})()")
        webView.loadUrl("javascript:(function() { " +
                "document.getElementsByClassName('ogb-wrapper ogb-si ogb-si')[0].style.display='none'; " +
                "})()")

        webView.loadUrl("javascript:(function() { " +
                "document.getElementsByClassName('devsite-footer-linkboxes nocontent')[0].style.display='none'; " +
                "})()")*/
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
        val intentFilter = IntentFilter("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(wifiStateReceiver, intentFilter)
    }









    private lateinit var binding: ActivityDetailsBinding

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.progressBar.visibility=View.VISIBLE
        binding.progressBar.startAnimation(AnimationUtils.loadAnimation(applicationContext, R.anim.loading))


        binding.fab.setOnClickListener {
            askPermissions()
        }
        binding.close.setOnClickListener {
            finish()
        }

   /*     binding.bottomNav.setOnNavigationItemSelectedListener { item ->


            when (item.itemId) {
                R.id.home_close -> {
                    finish()
                }
                R.id.download -> {
                    askPermissions()
                }
                else -> {

                }
            }



          //  Toast.makeText(this@MainActivity, item.title, Toast.LENGTH_SHORT).show()
            true
        }*/

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



        binding.webView.apply{
         //   loadUrl("file:///android_asset/index.html")

            if (isOnline(applicationContext)) {
                //internetCheck = true
                loadUrl(lastSelectedImage)
                Log.d("test001", "yes")
            } else {
                //internetCheck = false
                //Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show();
                //loadUrl("file:///android_asset/www/index.html")
                Log.d("test001", "nio")
            }

            // to play video on a web view
            settings.javaScriptEnabled = true
            // to verify that the client requesting your web page is actually your Android app.
            settings.userAgentString = System.getProperty("http.agent") //Dalvik/2.1.0 (Linux; U; Android 11; M2012K11I Build/RKQ1.201112.002)
            settings.useWideViewPort = true
            // Enable zooming in web view
            settings.setSupportZoom(true)
            settings.builtInZoomControls = true
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




           // addJavascriptInterface(WebAppInterface(context), "Android")

           // settings.pluginState = WebSettings.PluginState.ON
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

    }

    /** Instantiate the interface and set the context  */
    class WebAppInterface(
        private val mContext: Context,
   ) {
        /** Show a toast from the web page  */
        @JavascriptInterface
        fun showToast(toast: String) {
            // toast.replace("s320","s8000")
            
        }
    }

    internal inner class MyWebClient : WebViewClient() {
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            Log.d("test001", "onReceivedError")
            binding.progressBar.visibility=View.GONE
        }


        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            Log.d("test001", "onPageCommitVisible")
            binding.progressBar.visibility=View.GONE
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            booleanIsValue++
            try{
                Log.d(TAG,"onPageFinished booleanIs: $booleanIsValue")
                if(booleanIsValue>20){
                    booleanIsValue = 1
                    showInterstitial()
                }
            }catch (e:Exception){}
        }
        @Deprecated("Deprecated in Java")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            Log.d("f1","url: $url")//url.contains("https") &&
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
    }

    private fun loadPage(url: String) {
        try {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(url)
            startActivity(i)
        }catch (e:Exception){}
    }


    override fun onBackPressed() {
        if (binding.webView != null) {
            if (binding.webView.canGoBack()) {
                binding.webView.goBack()
                return
            }
        }
        super.onBackPressed()
    }





  /*  public override fun onPause() {
        super.onPause()
        binding.webView.settings.javaScriptEnabled = false
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onResume() {
        super.onResume()
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled=true // need to check this
        binding.webView.settings.userAgentString = System.getProperty("http.agent")
    }
*/


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



       /* binding.webView.loadUrl("javascript:(function() { " +
                "document.getElementsByClassName('devsite-footer-utility nocontent')[0].style.display='none'; " +
                "})()")

        binding.webView.loadUrl("javascript:(function() { " +
                "document.getElementsByClassName('ogb-wrapper ogb-si ogb-si')[0].style.display='none'; " +
                "})()")*/
     //   removeElement(binding.webView)
    }

    // Cancel the timer if the game is paused.
    public override fun onPause() {
        mCountDownTimer?.cancel()
        super.onPause()
    }




    //..................................................................

    private fun downloadImage(url: String) {

        val directory = File(Environment.DIRECTORY_PICTURES)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val downloadManager = this.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val downloadUri = Uri.parse(url)

        val request = DownloadManager.Request(downloadUri).apply {
            setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false)
                 .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setTitle(url.substring(url.lastIndexOf("/") + 1))
                .setDescription("So Pix")
                .setDestinationInExternalPublicDir(
                    directory.toString(),
                    url.substring(url.lastIndexOf("/") + 1)
                )
        }
        val reference: Long = downloadManager.enqueue(request)



    }
}



