package top.xuqingquan.web.system

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import top.xuqingquan.utils.Timber
import top.xuqingquan.utils.getDeniedPermissions
import top.xuqingquan.utils.hasPermission
import top.xuqingquan.web.nokernel.Action
import top.xuqingquan.web.nokernel.ActionActivity
import top.xuqingquan.web.nokernel.ActionActivity.KEY_FROM_INTENTION
import top.xuqingquan.web.nokernel.AgentWebPermissions
import top.xuqingquan.web.nokernel.PermissionInterceptor
import top.xuqingquan.web.publics.AgentWebUtils
import top.xuqingquan.web.publics.IVideo
import top.xuqingquan.web.publics.IndicatorController
import java.lang.ref.WeakReference

@Suppress("DEPRECATION", "OverridingDeprecatedMember")
class DefaultChromeClient(
    activity: Activity,
    /**
     * IndicatorController 进度条控制器
     */
    private val mIndicatorController: IndicatorController?,
    chromeClient: WebChromeClient?,
    /**
     * Video 处理类
     */
    private val mIVideo: IVideo?,
    /**
     * PermissionInterceptor 权限拦截器
     */
    private val mPermissionInterceptor: PermissionInterceptor?,
    /**
     * 当前 WebView
     */
    private val mWebView: WebView
) : MiddlewareWebChromeBase(chromeClient) {
    /**
     * Activity
     */
    private val mActivityWeakReference = WeakReference(activity)

    /**
     * 包装Flag
     */
    private val mIsWrapper = chromeClient != null

    /**
     * Web端触发的定位 mOrigin
     */
    private var mOrigin: String? = null

    /**
     * Web 端触发的定位 Callback 回调成功，或者失败
     */
    private var mCallback: GeolocationPermissions.Callback? = null

    /**
     * AbsAgentWebUIController
     */
    private val mAgentWebUIController =
        WeakReference(AgentWebUtils.getAgentWebUIControllerByWebView(mWebView))

    private val mPermissionListener = ActionActivity.PermissionListener { permissions, _, extras ->
        if (extras.getInt(KEY_FROM_INTENTION) == FROM_CODE_INTENTION_LOCATION) {
            val hasPermission = hasPermission(mActivityWeakReference.get()!!, *permissions)
            if (mCallback != null) {
                if (hasPermission) {
                    mCallback!!.invoke(mOrigin, true, false)
                } else {
                    val mActivity = mActivityWeakReference.get()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mActivity != null) {
                        var shouldShowRequestPermissionRationale = false
                        for (permission in permissions) {
                            if (mActivity.shouldShowRequestPermissionRationale(permission)
                            ) {//只要有一个需要申请权限的
                                shouldShowRequestPermissionRationale = true
                                break
                            }
                        }
                        mCallback!!.invoke(mOrigin, false, !shouldShowRequestPermissionRationale)
                    } else {
                        mCallback!!.invoke(mOrigin, false, true)
                    }
                }
                mCallback = null
                mOrigin = null
            }
            if (!hasPermission && null != mAgentWebUIController.get()) {
                mAgentWebUIController.get()!!
                    .onPermissionsDeny(
                        AgentWebPermissions.LOCATION,
                        AgentWebPermissions.ACTION_LOCATION,
                        AgentWebPermissions.ACTION_LOCATION
                    )
            }
        }
    }


    override fun onProgressChanged(view: WebView?, newProgress: Int) {
        super.onProgressChanged(view, newProgress)
        mIndicatorController?.progress(view, newProgress)
    }

    override fun onReceivedTitle(view: WebView?, title: String?) {
        if (mIsWrapper) {
            super.onReceivedTitle(view, title)
        }
    }

    override fun onJsAlert(
        view: WebView?, url: String?, message: String?, result: JsResult?
    ): Boolean {
        if (super.onJsAlert(view, url, message, result)) {
            return true
        }
        mAgentWebUIController.get()?.onJsAlert(view, url, message)
        result?.confirm()
        return true
    }

    //location
    override fun onGeolocationPermissionsShowPrompt(
        origin: String?, callback: GeolocationPermissions.Callback?
    ) {
        super.onGeolocationPermissionsShowPrompt(origin, callback)
        onGeolocationPermissionsShowPromptInternal(origin, callback)
    }

    private fun onGeolocationPermissionsShowPromptInternal(
        origin: String?, callback: GeolocationPermissions.Callback?
    ) {
        if (mPermissionInterceptor != null) {
            if (mPermissionInterceptor.intercept(
                    this.mWebView.url,
                    AgentWebPermissions.LOCATION,
                    "location"
                )
            ) {
                callback?.invoke(origin, false, false)
                return
            }
        }
        val mActivity = mActivityWeakReference.get()
        if (mActivity == null) {
            callback?.invoke(origin, false, false)
            return
        }
        val deniedPermissions = getDeniedPermissions(mActivity, AgentWebPermissions.LOCATION)
        if (deniedPermissions.isEmpty()) {
            Timber.i("onGeolocationPermissionsShowPromptInternal:true")
            callback?.invoke(origin, true, false)
        } else {
            val mAction = Action.createPermissionsAction(deniedPermissions)
            mAction.fromIntention = FROM_CODE_INTENTION_LOCATION
            ActionActivity.setPermissionListener(mPermissionListener)
            this.mCallback = callback
            this.mOrigin = origin
            ActionActivity.start(mActivity, mAction)
        }
    }

    override fun onJsPrompt(
        view: WebView?, url: String?, message: String?,
        defaultValue: String?, result: JsPromptResult?
    ): Boolean {
        if (super.onJsPrompt(view, url, message, defaultValue, result)) {
            return true
        }
        try {
            this.mAgentWebUIController.get()
                ?.onJsPrompt(mWebView, url, message, defaultValue, result)
        } catch (throwable: Throwable) {
            Timber.e(throwable)
        }
        return true
    }

    override fun onJsConfirm(
        view: WebView?, url: String?, message: String?, result: JsResult?
    ): Boolean {
        if (super.onJsConfirm(view, url, message, result)) {
            return true
        }
        mAgentWebUIController.get()?.onJsConfirm(view, url, message, result)
        return true
    }


    @Suppress("OVERRIDE_DEPRECATION")
    override fun onExceededDatabaseQuota(
        url: String?, databaseIdentifier: String?, quota: Long, estimatedDatabaseSize: Long,
        totalQuota: Long, quotaUpdater: WebStorage.QuotaUpdater?
    ) {
        super.onExceededDatabaseQuota(
            url,
            databaseIdentifier,
            quota,
            estimatedDatabaseSize,
            totalQuota,
            quotaUpdater
        )
        quotaUpdater?.updateQuota(totalQuota * 2)
    }

    override fun onShowFileChooser(
        webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        if (super.onShowFileChooser(webView, filePathCallback, fileChooserParams)) {
            return true
        }
        Timber.i("openFileChooser >= 5.0")
        return openFileChooserAboveL(filePathCallback, fileChooserParams)
    }

    private fun openFileChooserAboveL(
        valueCallbacks: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean {
        Timber.i("fileChooserParams:${fileChooserParams?.acceptTypes}  getTitle:${fileChooserParams?.title} accept:${fileChooserParams?.acceptTypes} length:${fileChooserParams?.acceptTypes?.size}  isCaptureEnabled:${fileChooserParams?.isCaptureEnabled}  ${fileChooserParams?.filenameHint}  intent:${fileChooserParams?.createIntent()}    mode:${fileChooserParams?.mode}")
        val mActivity = this.mActivityWeakReference.get()
        return if (mActivity == null || mActivity.isFinishing) {
            false
        } else AgentWebUtils.showFileChooserCompat(
            mActivity, mWebView, valueCallbacks, fileChooserParams,
            this.mPermissionInterceptor, null, null, null
        )
    }

    override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
        if (super.onConsoleMessage(consoleMessage)) {
            return true
        }
        Timber.d("onConsoleMessage:level=${consoleMessage?.messageLevel()},message=${consoleMessage?.message()},line=${consoleMessage?.lineNumber()},sourceId=${consoleMessage?.sourceId()}")
        return true
    }

    override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
        super.onShowCustomView(view, callback)
        mIVideo?.onShowCustomView(view, callback)
    }

    override fun onHideCustomView() {
        super.onHideCustomView()
        mIVideo?.onHideCustomView()
    }

    companion object {
        /**
         * 标志位
         */
        private const val FROM_CODE_INTENTION = 0x18

        /**
         * 标识当前是获取定位权限
         */
        private const val FROM_CODE_INTENTION_LOCATION = FROM_CODE_INTENTION shl 2
    }
}
