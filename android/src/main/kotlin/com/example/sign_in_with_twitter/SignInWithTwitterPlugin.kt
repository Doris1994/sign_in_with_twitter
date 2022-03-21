package com.example.sign_in_with_twitter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.webkit.CookieManager
import android.webkit.CookieSyncManager
import androidx.annotation.NonNull
import com.twitter.sdk.android.core.*
import com.twitter.sdk.android.core.identity.TwitterAuthClient
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener
import java.util.*

/** SignInWithTwitterPlugin */
class SignInWithTwitterPlugin : ActivityAware, FlutterPlugin, MethodCallHandler,
    ActivityResultListener {

    private var mMethodChannel: MethodChannel? = null
    private var authClientInstance: TwitterAuthClient? = null
    private val METHOD_AUTHORIZE = "authorize"
    private val METHOD_LOG_OUT = "logOut"
    private var mContext: Context? = null
    private var mActivity: Activity? = null
    private var pendingResult: Result? = null
    private lateinit var mPluginBinding: FlutterPlugin.FlutterPluginBinding

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        mPluginBinding = flutterPluginBinding
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        mMethodChannel?.setMethodCallHandler(null)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            METHOD_AUTHORIZE -> authorize(
                result,
                call)
            METHOD_LOG_OUT -> logOut(
                result,
                call)
            else -> result.notImplemented()
        }
    }


    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        this.mActivity = binding.activity
        this.mContext = mPluginBinding.applicationContext
        binding.addActivityResultListener(this)
        mMethodChannel = MethodChannel(mPluginBinding.binaryMessenger, "sign_in_with_twitter")
        mMethodChannel?.setMethodCallHandler(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        mMethodChannel?.setMethodCallHandler(null)
        mMethodChannel = null
        authClientInstance = null
        mContext = null
        mActivity = null
        pendingResult = null
    }

    private fun getCurrentSession( call: MethodCall):Boolean {
        initializeAuthClient(call)
        val session = TwitterCore.getInstance().sessionManager.activeSession
        if(session !=null &&  session.userId!=null){
            getTwitterUserEmail(session)
            return true
        }
        return false
    }

    private fun authorize(result: Result, call: MethodCall) {
//        if(getCurrentSession(call)) return
        setPendingResult("authorize", result)
        initializeAuthClient(call)?.authorize(mActivity, object : Callback<TwitterSession>() {
            override fun success(result: com.twitter.sdk.android.core.Result<TwitterSession>) {
                getTwitterUserEmail(result.data)
            }

            override fun failure(exception: TwitterException) {
                if (pendingResult != null) {
                    val resultMap: HashMap<String, Any> = object : HashMap<String, Any>() {
                        init {
                            put("status", "error")
                            put("errorMessage", exception.message ?: "")
                        }
                    }
                    pendingResult!!.success(resultMap)
                    pendingResult = null
                }
            }
        })
    }

    private fun getTwitterUserEmail(session: TwitterSession?) {
        val activeSession = TwitterCore.getInstance().sessionManager.activeSession
        authClientInstance?.requestEmail(activeSession, object : Callback<String>() {
            override fun success(result: com.twitter.sdk.android.core.Result<String>?) {
                loggedIn(session, result?.data)
            }

            override fun failure(exception: TwitterException?) {
                loggedIn(session, null)
            }
        });
    }

    private fun loggedIn(session: TwitterSession?, email: String?) {
        if (pendingResult != null) {
            val sessionMap = sessionToMap(session, email)
            val resultMap: HashMap<String, Any?> = object : HashMap<String, Any?>() {
                init {
                    put("status", "loggedIn")
                    put("session", sessionMap)
                }
            }
            pendingResult!!.success(resultMap)
            pendingResult = null
        }
    }

    private fun sessionToMap(session: TwitterSession?, email: String?): HashMap<String, Any?>? {
        return if (session == null) {
            null
        } else object : HashMap<String, Any?>() {
            init {
                put("secret", session.authToken.secret)
                put("token", session.authToken.token)
                put("userId", session.userId.toString())
                put("username", session.userName)
                put("email", email)
            }
        }
    }

    private fun initializeAuthClient(call: MethodCall): TwitterAuthClient? {
        if (authClientInstance == null) {
            val consumerKey = call.argument<String>("consumerKey")
            val consumerSecret = call.argument<String>("consumerSecret")
            authClientInstance = configureClient(consumerKey, consumerSecret)
        }
        return authClientInstance
    }

    private fun setPendingResult(methodName: String, result: Result) {
        if (pendingResult != null) {
            result.error(
                "TWITTER_LOGIN_IN_PROGRESS",
                methodName + " called while another Twitter " +
                        "login operation was in progress.",
                null
            )
        }
        pendingResult = result
    }

    private fun configureClient(consumerKey: String?, consumerSecret: String?): TwitterAuthClient {
        val authConfig = TwitterAuthConfig(consumerKey, consumerSecret)
        val config = TwitterConfig.Builder(mContext)
            .twitterAuthConfig(authConfig)
            .build()
        Twitter.initialize(config)
        return TwitterAuthClient()
    }

    private fun logOut(result: Result, call: MethodCall) {
        CookieSyncManager.createInstance(mContext)
        val cookieManager = CookieManager.getInstance()
        cookieManager.removeSessionCookie()
        initializeAuthClient(call)
        TwitterCore.getInstance().sessionManager.clearActiveSession()
//        result.success(null)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        authClientInstance?.onActivityResult(requestCode, resultCode, data)
        return false
    }


}
